package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.controller.form.AutomaticResourceTransferForm;
import com.github.retro_game.retro_game.dto.*;
import com.github.retro_game.retro_game.entity.*;
import com.github.retro_game.retro_game.repository.AutomaticResourceTransferRepository;
import com.github.retro_game.retro_game.repository.BodyRepository;
import com.github.retro_game.retro_game.security.CustomUser;
import com.github.retro_game.retro_game.service.AutomaticResourceTransferService;
import com.github.retro_game.retro_game.service.FlightService;
import com.github.retro_game.retro_game.service.exception.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class AutomaticResourceTransferServiceImpl implements AutomaticResourceTransferService {
  private static final int DISPATCH_LIMIT = 50;
  private static final int LAST_ERROR_MAX_LENGTH = 128;

  private final AutomaticResourceTransferRepository transferRepository;
  private final BodyRepository bodyRepository;
  private final FlightService flightService;
  private final TransactionTemplate transactionTemplate;

  AutomaticResourceTransferServiceImpl(AutomaticResourceTransferRepository transferRepository,
                                       BodyRepository bodyRepository,
                                       FlightService flightService,
                                       PlatformTransactionManager transactionManager) {
    this.transferRepository = transferRepository;
    this.bodyRepository = bodyRepository;
    this.flightService = flightService;

    var transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomaticResourceTransferDto> getTransfers(long bodyId) {
    long userId = CustomUser.getCurrentUserId();
    return transferRepository.findBySourceBody_IdAndUser_IdOrderById(bodyId, userId).stream()
        .map(this::convert)
        .toList();
  }

  @Override
  @Transactional
  public void create(long bodyId, AutomaticResourceTransferForm form) {
    long userId = CustomUser.getCurrentUserId();
    Body sourceBody = bodyRepository.findById(bodyId).orElseThrow(BodyDoesNotExistException::new);
    if (sourceBody.getUser().getId() != userId) {
      throw new BodyDoesNotExistException();
    }
    requireAllianceDepot(sourceBody);
    int maxTransfers = getMaxTransfers(sourceBody);
    if (transferRepository.countBySourceBody_IdAndUser_Id(bodyId, userId) >= maxTransfers) {
      throw new AutomaticResourceTransferLimitReachedException();
    }

    Body targetBody = bodyRepository.findById(form.getTargetBody()).orElseThrow(BodyDoesNotExistException::new);
    if (targetBody.getUser().getId() != userId || targetBody.getId() == sourceBody.getId()) {
      throw new WrongTargetException();
    }

    UnitKind shipKind = convertShipKind(form.getShipKind());
    Resources resources = new Resources(form.getMetal(), form.getCrystal(), form.getDeuterium());
    if (resources.total() <= 0) {
      throw new EmptyAutomaticTransferResourcesException();
    }

    var transfer = new AutomaticResourceTransfer();
    transfer.setUser(sourceBody.getUser());
    transfer.setSourceBody(sourceBody);
    transfer.setTargetBody(targetBody);
    transfer.setEnabled(true);
    transfer.setShipKind(shipKind);
    transfer.setShipCount(form.getShipCount());
    transfer.setResources(resources);
    transfer.setSpeedFactor(form.getSpeedFactor());
    transfer.setRunHour(getRunHour(form.getRunTime()));
    transfer.setRunMinute(getRunMinute(form.getRunTime()));
    transfer.setNextRunAt(nextRunAt(transfer.getRunHour(), transfer.getRunMinute(), new Date()));
    transferRepository.save(transfer);
  }

  @Override
  @Transactional
  public void toggle(long bodyId, long transferId) {
    AutomaticResourceTransfer transfer = getOwnedTransfer(bodyId, transferId);
    transfer.setEnabled(!transfer.isEnabled());
    if (transfer.isEnabled() && transfer.getNextRunAt().before(new Date())) {
      transfer.setNextRunAt(nextRunAt(transfer.getRunHour(), transfer.getRunMinute(), new Date()));
    }
  }

  @Override
  @Transactional
  public void delete(long bodyId, long transferId) {
    AutomaticResourceTransfer transfer = getOwnedTransfer(bodyId, transferId);
    transferRepository.delete(transfer);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getDueTransferIds(int limit) {
    return transferRepository.findDueIds(new Date(), PageRequest.of(0, Math.min(limit, DISPATCH_LIMIT)));
  }

  @Override
  public void dispatch(long transferId) {
    SendFleetParamsDto params = transactionTemplate.execute(status -> prepareDispatch(transferId));
    if (params == null) {
      return;
    }

    try {
      flightService.send(params);
      markDispatchResult(transferId, null);
    } catch (ServiceException e) {
      markDispatchResult(transferId, e.getClass().getSimpleName());
    } catch (RuntimeException e) {
      markDispatchResult(transferId, e.getClass().getSimpleName());
      throw e;
    }
  }

  private SendFleetParamsDto prepareDispatch(long transferId) {
    var transferOptional = transferRepository.findById(transferId);
    if (transferOptional.isEmpty()) {
      return null;
    }

    AutomaticResourceTransfer transfer = transferOptional.get();
    if (!transfer.isEnabled()) {
      return null;
    }

    Date now = new Date();
    transfer.setLastRunAt(now);
    transfer.setNextRunAt(nextRunAt(transfer.getRunHour(), transfer.getRunMinute(), now));

    if (transfer.getSourceBody().getBuildingLevel(BuildingKind.ALLIANCE_DEPOT) <= 0) {
      transfer.setLastError(AllianceDepotRequiredException.class.getSimpleName());
      return null;
    }
    if (isOverTransferLimit(transfer)) {
      transfer.setLastError(AutomaticResourceTransferLimitReachedException.class.getSimpleName());
      return null;
    }

    return new SendFleetParamsDto(
        transfer.getSourceBody().getId(),
        Map.of(Converter.convert(transfer.getShipKind()), transfer.getShipCount()),
        MissionDto.TRANSPORT,
        null,
        Converter.convert(transfer.getTargetBody().getCoordinates()),
        transfer.getSpeedFactor(),
        Converter.convert(transfer.getResources()),
        null,
        true);
  }

  private void markDispatchResult(long transferId, String error) {
    transactionTemplate.executeWithoutResult(status -> transferRepository.findById(transferId).ifPresent(transfer -> {
      if (error == null) {
        transfer.setLastError(null);
      } else {
        transfer.setLastError(error.substring(0, Math.min(error.length(), LAST_ERROR_MAX_LENGTH)));
      }
    }));
  }

  private AutomaticResourceTransfer getOwnedTransfer(long bodyId, long transferId) {
    long userId = CustomUser.getCurrentUserId();
    AutomaticResourceTransfer transfer = transferRepository.findById(transferId)
        .orElseThrow(AutomaticResourceTransferDoesNotExistException::new);
    if (transfer.getUser().getId() != userId || transfer.getSourceBody().getId() != bodyId) {
      throw new AutomaticResourceTransferDoesNotExistException();
    }
    return transfer;
  }

  private AutomaticResourceTransferDto convert(AutomaticResourceTransfer transfer) {
    Body targetBody = transfer.getTargetBody();
    return new AutomaticResourceTransferDto(
        transfer.getId(),
        targetBody.getId(),
        targetBody.getName(),
        Converter.convert(targetBody.getCoordinates()),
        transfer.isEnabled(),
        Converter.convert(transfer.getShipKind()),
        transfer.getShipCount(),
        Converter.convert(transfer.getResources()),
        transfer.getSpeedFactor(),
        transfer.getRunHour(),
        transfer.getRunMinute(),
        transfer.getNextRunAt(),
        transfer.getLastRunAt(),
        transfer.getLastError());
  }

  private void requireAllianceDepot(Body body) {
    if (body.getBuildingLevel(BuildingKind.ALLIANCE_DEPOT) <= 0) {
      throw new AllianceDepotRequiredException();
    }
  }

  private int getMaxTransfers(Body body) {
    return body.getBuildingLevel(BuildingKind.ALLIANCE_DEPOT) / 3;
  }

  private boolean isOverTransferLimit(AutomaticResourceTransfer transfer) {
    int maxTransfers = getMaxTransfers(transfer.getSourceBody());
    List<AutomaticResourceTransfer> transfers = transferRepository.findBySourceBody_IdAndUser_IdOrderById(
        transfer.getSourceBody().getId(), transfer.getUser().getId());
    for (int i = 0; i < transfers.size(); i++) {
      if (transfers.get(i).getId() == transfer.getId()) {
        return i >= maxTransfers;
      }
    }
    return true;
  }

  private int getRunHour(String runTime) {
    return Integer.parseInt(runTime.substring(0, 2));
  }

  private int getRunMinute(String runTime) {
    return Integer.parseInt(runTime.substring(3, 5));
  }

  private Date nextRunAt(int hour, int minute, Date now) {
    ZoneId zoneId = ZoneId.systemDefault();
    LocalDateTime current = LocalDateTime.ofInstant(now.toInstant(), zoneId);
    LocalDateTime next = LocalDate.now(zoneId).atTime(LocalTime.of(hour, minute));
    if (!next.isAfter(current)) {
      next = next.plusDays(1);
    }
    return Date.from(next.atZone(zoneId).toInstant());
  }

  private UnitKind convertShipKind(UnitKindDto shipKind) {
    if (shipKind != UnitKindDto.SMALL_CARGO && shipKind != UnitKindDto.LARGE_CARGO) {
      throw new WrongAutomaticTransferShipException();
    }
    return Converter.convert(shipKind);
  }
}

package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.controller.activity.Activity;
import com.github.retro_game.retro_game.controller.form.AutomaticResourceTransferForm;
import com.github.retro_game.retro_game.dto.BuildingKindDto;
import com.github.retro_game.retro_game.dto.UnitKindDto;
import com.github.retro_game.retro_game.dto.UserContextDto;
import com.github.retro_game.retro_game.service.AutomaticResourceTransferService;
import com.github.retro_game.retro_game.service.BodyService;
import com.github.retro_game.retro_game.service.UserService;
import com.github.retro_game.retro_game.service.exception.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AllianceDepotController {
  private final AutomaticResourceTransferService automaticResourceTransferService;
  private final BodyService bodyService;
  private final UserService userService;

  public AllianceDepotController(AutomaticResourceTransferService automaticResourceTransferService,
                                 BodyService bodyService,
                                 UserService userService) {
    this.automaticResourceTransferService = automaticResourceTransferService;
    this.bodyService = bodyService;
    this.userService = userService;
  }

  private String perform(long bodyId, Supplier<Integer> action) {
    AutomaticResourceTransferError error = null;
    try {
      action.get();
    } catch (AllianceDepotRequiredException e) {
      error = AutomaticResourceTransferError.ALLIANCE_DEPOT_REQUIRED;
    } catch (AutomaticResourceTransferDoesNotExistException e) {
      error = AutomaticResourceTransferError.TRANSFER_DOES_NOT_EXIST;
    } catch (AutomaticResourceTransferLimitReachedException e) {
      error = AutomaticResourceTransferError.TRANSFER_LIMIT_REACHED;
    } catch (BodyDoesNotExistException e) {
      error = AutomaticResourceTransferError.BODY_DOES_NOT_EXIST;
    } catch (EmptyAutomaticTransferResourcesException e) {
      error = AutomaticResourceTransferError.EMPTY_RESOURCES;
    } catch (WrongAutomaticTransferShipException e) {
      error = AutomaticResourceTransferError.WRONG_SHIP;
    } catch (WrongTargetException e) {
      error = AutomaticResourceTransferError.WRONG_TARGET;
    } catch (ConcurrencyFailureException e) {
      error = AutomaticResourceTransferError.CONCURRENCY;
    }
    if (error != null) {
      return "redirect:/alliance-depot/transfers?body=" + bodyId + "&error=" + error;
    }
    return "redirect:/alliance-depot/transfers?body=" + bodyId;
  }

  @GetMapping("/alliance-depot/transfers")
  @PreAuthorize("hasPermission(#bodyId, 'ACCESS')")
  @Activity(bodies = "#bodyId")
  public String transfers(@RequestParam(name = "body") long bodyId,
                          @RequestParam(required = false) AutomaticResourceTransferError error,
                          Model model) {
    UserContextDto ctx = userService.getCurrentUserContext(bodyId);
    int allianceDepotLevel = ctx.curBody().buildings().getOrDefault(BuildingKindDto.ALLIANCE_DEPOT, 0);
    int maxTransfers = allianceDepotLevel / 3;
    var transfers = automaticResourceTransferService.getTransfers(bodyId);
    model.addAttribute("bodyId", bodyId);
    model.addAttribute("ctx", ctx);
    model.addAttribute("error", error);
    model.addAttribute("transfers", transfers);
    model.addAttribute("maxTransfers", maxTransfers);
    model.addAttribute("transferLimitReached", transfers.size() >= maxTransfers);
    model.addAttribute("bodies", bodyService.getBodiesBasicInfo(bodyId));
    model.addAttribute("cargoKinds", List.of(UnitKindDto.SMALL_CARGO, UnitKindDto.LARGE_CARGO));
    return "alliance-depot-transfers";
  }

  @PostMapping("/alliance-depot/transfers")
  @PreAuthorize("hasPermission(#form.body, 'ACCESS')")
  @Activity(bodies = "#form.body")
  public String create(@Valid AutomaticResourceTransferForm form) {
    return perform(form.getBody(), () -> {
      automaticResourceTransferService.create(form.getBody(), form);
      return 0;
    });
  }

  @PostMapping("/alliance-depot/transfers/toggle")
  @PreAuthorize("hasPermission(#bodyId, 'ACCESS')")
  @Activity(bodies = "#bodyId")
  public String toggle(@RequestParam(name = "body") long bodyId, @RequestParam long transfer) {
    return perform(bodyId, () -> {
      automaticResourceTransferService.toggle(bodyId, transfer);
      return 0;
    });
  }

  @PostMapping("/alliance-depot/transfers/delete")
  @PreAuthorize("hasPermission(#bodyId, 'ACCESS')")
  @Activity(bodies = "#bodyId")
  public String delete(@RequestParam(name = "body") long bodyId, @RequestParam long transfer) {
    return perform(bodyId, () -> {
      automaticResourceTransferService.delete(bodyId, transfer);
      return 0;
    });
  }

  public enum AutomaticResourceTransferError {
    ALLIANCE_DEPOT_REQUIRED,
    BODY_DOES_NOT_EXIST,
    CONCURRENCY,
    EMPTY_RESOURCES,
    TRANSFER_LIMIT_REACHED,
    TRANSFER_DOES_NOT_EXIST,
    WRONG_SHIP,
    WRONG_TARGET,
  }
}

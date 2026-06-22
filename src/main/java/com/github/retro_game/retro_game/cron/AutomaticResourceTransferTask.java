package com.github.retro_game.retro_game.cron;

import com.github.retro_game.retro_game.service.AutomaticResourceTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class AutomaticResourceTransferTask {
  private static final int LIMIT = 50;
  private static final Logger logger = LoggerFactory.getLogger(AutomaticResourceTransferTask.class);

  private final AutomaticResourceTransferService automaticResourceTransferService;
  private final boolean allianceDepotAutoTransfer;

  AutomaticResourceTransferTask(AutomaticResourceTransferService automaticResourceTransferService,
                                @Value("${retro-game.alliance-depot-auto-transfer:false}")
                                boolean allianceDepotAutoTransfer) {
    this.automaticResourceTransferService = automaticResourceTransferService;
    this.allianceDepotAutoTransfer = allianceDepotAutoTransfer;
  }

  @Scheduled(fixedDelay = 30000)
  void dispatchDueTransfers() {
    if (!allianceDepotAutoTransfer) {
      return;
    }

    for (long transferId : automaticResourceTransferService.getDueTransferIds(LIMIT)) {
      try {
        automaticResourceTransferService.dispatch(transferId);
      } catch (RuntimeException e) {
        logger.warn("Automatic resource transfer failed unexpectedly: transferId={} msg={}", transferId,
            e.getMessage());
      }
    }
  }
}

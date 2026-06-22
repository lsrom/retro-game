package com.github.retro_game.retro_game.cron;

import com.github.retro_game.retro_game.service.AutomaticResourceTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class AutomaticResourceTransferTask {
  private static final int LIMIT = 50;
  private static final Logger logger = LoggerFactory.getLogger(AutomaticResourceTransferTask.class);

  private final AutomaticResourceTransferService automaticResourceTransferService;

  AutomaticResourceTransferTask(AutomaticResourceTransferService automaticResourceTransferService) {
    this.automaticResourceTransferService = automaticResourceTransferService;
  }

  @Scheduled(fixedDelay = 30000)
  void dispatchDueTransfers() {
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

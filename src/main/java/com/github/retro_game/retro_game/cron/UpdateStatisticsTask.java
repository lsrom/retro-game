package com.github.retro_game.retro_game.cron;

import com.github.retro_game.retro_game.service.StatisticsUpdateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class UpdateStatisticsTask {
  private final StatisticsUpdateService statisticsUpdateService;

  public UpdateStatisticsTask(StatisticsUpdateService statisticsUpdateService) {
    this.statisticsUpdateService = statisticsUpdateService;
  }

  @Scheduled(cron = "0 0 0,8,16 * * *")
  private void update() {
    statisticsUpdateService.updateStatistics();
  }
}

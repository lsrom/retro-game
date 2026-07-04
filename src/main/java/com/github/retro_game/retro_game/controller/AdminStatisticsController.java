package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.service.StatisticsUpdateService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminStatisticsController {
  private final StatisticsUpdateService statisticsUpdateService;

  public AdminStatisticsController(StatisticsUpdateService statisticsUpdateService) {
    this.statisticsUpdateService = statisticsUpdateService;
  }

  @GetMapping("/admin/statistics")
  public String statistics() {
    return "admin-statistics";
  }

  @PostMapping("/admin/statistics/update")
  public String update(RedirectAttributes redirectAttributes) {
    statisticsUpdateService.updateStatistics();
    redirectAttributes.addFlashAttribute("statisticsUpdated", true);
    return "redirect:/admin/statistics";
  }
}

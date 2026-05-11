package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.controller.activity.Activity;
import com.github.retro_game.retro_game.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Random;

@Controller
public class TraderController {
  private final UserService userService;

  public TraderController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/trader")
  @PreAuthorize("hasPermission(#bodyId, 'ACCESS')")
  @Activity(bodies = "#bodyId")
  public String trader(@RequestParam(name = "body") long bodyId, Model model) {
    var ctx = userService.getCurrentUserContext(bodyId);

    long seed = LocalDate.now().toEpochDay();
    var random = new Random(seed);
    String[] resources = {"Metal", "Crystal", "Deuterium"};
    String tradedResource = resources[random.nextInt(resources.length)];

    double crystalRate = 1.6 + random.nextDouble() * (2.1 - 1.6);
    double metalRate = 2.4 + random.nextDouble() * (3.2 - 2.4);

    model.addAttribute("bodyId", bodyId);
    model.addAttribute("ctx", ctx);
    model.addAttribute("tradedResource", tradedResource);
    model.addAttribute("metalRate", metalRate);
    model.addAttribute("crystalRate", crystalRate);
    model.addAttribute("deuteriumRate", 1.0);
    return "trader";
  }
}

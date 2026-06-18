package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.repository.BodyRepository;
import com.github.retro_game.retro_game.security.CustomUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {
  private final BodyRepository bodyRepository;

  public AdminHomeController(BodyRepository bodyRepository) {
    this.bodyRepository = bodyRepository;
  }

  @GetMapping("/admin/")
  public String home() {
    return "admin-home";
  }

  @GetMapping("/admin/game")
  public String gameOverview() {
    return bodyRepository.findIdsByUserIdOrderById(CustomUser.getCurrentUserId()).stream()
        .findFirst()
        .map(bodyId -> "redirect:/overview?body=" + bodyId)
        .orElse("redirect:/");
  }
}

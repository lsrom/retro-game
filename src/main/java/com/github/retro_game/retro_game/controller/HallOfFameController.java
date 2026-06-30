package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.cache.UserInfoCache;
import com.github.retro_game.retro_game.dto.CombatReportSortOrderDto;
import com.github.retro_game.retro_game.dto.UserInfoDto;
import com.github.retro_game.retro_game.service.HallOfFameService;
import com.github.retro_game.retro_game.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HallOfFameController {
  private static final long PIRATE_USER_ID = -1L;
  private static final long ALIEN_USER_ID = -2L;

  private final boolean hallOfFameEnabled;
  private final UserInfoCache userInfoCache;
  private final HallOfFameService hallOfFameService;
  private final MessageSource messageSource;
  private final UserService userService;

  public HallOfFameController(@Value("${retro-game.hall-of-fame-enabled}") boolean hallOfFameEnabled,
                              UserInfoCache userInfoCache, HallOfFameService hallOfFameService,
                              MessageSource messageSource, UserService userService) {
    this.hallOfFameEnabled = hallOfFameEnabled;
    this.userInfoCache = userInfoCache;
    this.hallOfFameService = hallOfFameService;
    this.messageSource = messageSource;
    this.userService = userService;
  }

  @GetMapping("/hall-of-fame")
  public String hallOfFame(@RequestParam(name = "body") long bodyId,
                           @RequestParam(required = false) CombatReportSortOrderDto order, Model model) {
    var ctx = userService.getCurrentUserContext(bodyId);

    model.addAttribute("bodyId", bodyId);
    model.addAttribute("ctx", ctx);
    model.addAttribute("enabled", hallOfFameEnabled);

    if (!hallOfFameEnabled) {
      return "hall-of-fame";
    }

    if (order == null) {
      order = CombatReportSortOrderDto.LOSS;
    }
    var entries = hallOfFameService.get(order);

    var userIds = new HashSet<Long>();
    for (var entry : entries) {
      entry.attackers().stream().filter(id -> id > 0).forEach(userIds::add);
      entry.defenders().stream().filter(id -> id > 0).forEach(userIds::add);
    }
    var userInfos = userInfoCache.getAll(userIds);

    Function<ArrayList<Long>, String> makeNames = list -> list.stream()
        .map(id -> getCombatantName(id, userInfos))
        .collect(Collectors.joining(", "));

    var attackerNames = new ArrayList<String>(entries.size());
    var defenderNames = new ArrayList<String>(entries.size());
    for (var entry : entries) {
      attackerNames.add(makeNames.apply(entry.attackers()));
      defenderNames.add(makeNames.apply(entry.defenders()));
    }

    model.addAttribute("entries", entries);
    model.addAttribute("attackerNames", attackerNames);
    model.addAttribute("defenderNames", defenderNames);

    return "hall-of-fame";
  }

  private String getCombatantName(long userId, Map<Long, UserInfoDto> userInfos) {
    if (userId == PIRATE_USER_ID) {
      return messageSource.getMessage("combatantPirates", null, LocaleContextHolder.getLocale());
    }
    if (userId == ALIEN_USER_ID) {
      return messageSource.getMessage("combatantAliens", null, LocaleContextHolder.getLocale());
    }
    if (userId <= 0) {
      return "[deleted]";
    }

    var info = userInfos.get(userId);
    return info == null ? "[deleted]" : info.getName();
  }
}

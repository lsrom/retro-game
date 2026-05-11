package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.controller.activity.Activity;
import com.github.retro_game.retro_game.controller.form.TradeForm;
import com.github.retro_game.retro_game.dto.TradeResourcesParamsDto;
import com.github.retro_game.retro_game.service.TraderService;
import com.github.retro_game.retro_game.service.UserService;
import com.github.retro_game.retro_game.service.exception.NotEnoughResourcesException;
import com.github.retro_game.retro_game.service.exception.WrongTradeAmountException;
import com.github.retro_game.retro_game.service.exception.WrongTradedResourceException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@Controller
public class TraderController {
  private final UserService userService;
  private final TraderService traderService;

  public TraderController(UserService userService, TraderService traderService) {
    this.userService = userService;
    this.traderService = traderService;
  }

  @GetMapping("/trader")
  @PreAuthorize("hasPermission(#bodyId, 'ACCESS')")
  @Activity(bodies = "#bodyId")
  public String trader(@RequestParam(name = "body") long bodyId,
                       @RequestParam(required = false) String error,
                       Model model) {
    var ctx = userService.getCurrentUserContext(bodyId);
    var offer = traderService.getCurrentOffer();

    model.addAttribute("bodyId", bodyId);
    model.addAttribute("ctx", ctx);
    model.addAttribute("error", error);
    model.addAttribute("tradedResource", offer.getTradedResource());
    model.addAttribute("tradedResourceKey", offer.getTradedResourceKey());
    model.addAttribute("metalRate", offer.getMetalRate());
    model.addAttribute("crystalRate", offer.getCrystalRate());
    model.addAttribute("deuteriumRate", offer.getDeuteriumRate());
    return "trader";
  }

  @PostMapping("/trader/trade")
  @PreAuthorize("hasPermission(#form.body, 'ACCESS')")
  @Activity(bodies = "#form.body")
  public String doTrade(@Valid TradeForm form) {
    var params = new TradeResourcesParamsDto(
        form.getBody(),
        form.getTradedResourceKey(),
        form.getMetalAmount(),
        form.getCrystalAmount(),
        form.getDeuteriumAmount(),
        form.getExpectedReceivedAmount()
    );

    String error = null;
    try {
      traderService.trade(params);
    } catch (NotEnoughResourcesException e) {
      error = "NOT_ENOUGH_RESOURCES";
    } catch (WrongTradeAmountException e) {
      error = "WRONG_TRADE_AMOUNT";
    } catch (WrongTradedResourceException e) {
      error = "WRONG_TRADED_RESOURCE";
    }

    if (error != null) {
      return "redirect:/trader?body=" + form.getBody() + "&error=" + error;
    }
    return "redirect:/trader?body=" + form.getBody();
  }
}

package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.controller.form.AdminItemCreateForm;
import com.github.retro_game.retro_game.controller.form.AdminItemForm;
import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.entity.UnitType;
import com.github.retro_game.retro_game.service.AdminItemService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin panel screens for the content catalog: list every building, technology
 * and unit, edit an individual item's values, and create new items. Access is
 * gated by the {@code /admin/**} rule in {@code SecurityConfig}.
 */
@Controller
public class AdminItemController {
  private final AdminItemService adminItemService;

  public AdminItemController(AdminItemService adminItemService) {
    this.adminItemService = adminItemService;
  }

  // Exposed to every screen in this controller so the create form can render
  // the type and unit-type drop-downs.
  @ModelAttribute("itemTypes")
  public ItemType[] itemTypes() {
    return ItemType.values();
  }

  @ModelAttribute("unitTypes")
  public UnitType[] unitTypes() {
    return UnitType.values();
  }

  @GetMapping("/admin/items")
  public String items(Model model) {
    model.addAttribute("items", adminItemService.getAllItems());
    return "admin-items";
  }

  @GetMapping("/admin/items/edit")
  public String edit(@RequestParam long id, Model model) {
    ItemDefinition item = adminItemService.getItem(id);
    model.addAttribute("item", item);
    model.addAttribute("itemForm", AdminItemForm.fromItem(item));
    model.addAttribute("requirements", adminItemService.getRequirements(item));
    // Every catalog item, for the add-requirement drop-down.
    model.addAttribute("allItems", adminItemService.getAllItems());
    return "admin-item-edit";
  }

  @PostMapping("/admin/items/save")
  public String save(@Valid @ModelAttribute("itemForm") AdminItemForm itemForm, BindingResult bindingResult,
                      Model model) {
    if (bindingResult.hasErrors()) {
      // Re-render the edit form with the rejected values and the read-only context.
      ItemDefinition item = adminItemService.getItem(itemForm.getId());
      model.addAttribute("item", item);
      model.addAttribute("requirements", adminItemService.getRequirements(item));
      model.addAttribute("allItems", adminItemService.getAllItems());
      return "admin-item-edit";
    }
    adminItemService.updateItem(itemForm);
    return "redirect:/admin/items?saved";
  }

  @PostMapping("/admin/items/requirement/add")
  public String addRequirement(@RequestParam long itemId, @RequestParam String requiredKind,
                               @RequestParam int requiredLevel) {
    adminItemService.addRequirement(itemId, requiredKind, requiredLevel);
    return "redirect:/admin/items/edit?id=" + itemId;
  }

  @PostMapping("/admin/items/requirement/remove")
  public String removeRequirement(@RequestParam long itemId, @RequestParam long requirementId) {
    adminItemService.removeRequirement(requirementId);
    return "redirect:/admin/items/edit?id=" + itemId;
  }

  @GetMapping("/admin/items/create")
  public String createForm(@ModelAttribute("itemForm") AdminItemCreateForm itemForm) {
    // Declaring the form as a @ModelAttribute also registers an (empty)
    // BindingResult, so the template's error helpers work on the first render.
    return "admin-item-create";
  }

  @PostMapping("/admin/items/create")
  public String create(@Valid @ModelAttribute("itemForm") AdminItemCreateForm itemForm,
                        BindingResult bindingResult) {
    if (!bindingResult.hasErrors()) {
      try {
        adminItemService.createItem(itemForm);
        return "redirect:/admin/items?created";
      } catch (IllegalArgumentException e) {
        // Surface the rejection (duplicate kind, bad cost factor, ...) on the form.
        bindingResult.reject("createItemFailed", e.getMessage());
      }
    }
    return "admin-item-create";
  }
}

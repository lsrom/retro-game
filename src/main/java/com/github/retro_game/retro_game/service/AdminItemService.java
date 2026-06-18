package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.controller.form.AdminItemCreateForm;
import com.github.retro_game.retro_game.controller.form.AdminItemForm;
import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemRequirement;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.repository.ItemDefinitionRepository;
import com.github.retro_game.retro_game.repository.ItemRequirementRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read/write operations on the content catalog, used by the admin panel.
 *
 * <p>Supports listing items, editing an existing item's values, and creating
 * brand-new items. A created item is added to the catalog right away; the game
 * acts on it once the catalog — rather than the fixed Java enum set — becomes
 * its item registry in a later phase.
 */
@Service
public class AdminItemService {
  private final ItemDefinitionRepository itemDefinitionRepository;
  private final ItemRequirementRepository itemRequirementRepository;
  private final CatalogService catalogService;

  public AdminItemService(ItemDefinitionRepository itemDefinitionRepository,
                          ItemRequirementRepository itemRequirementRepository,
                          CatalogService catalogService) {
    this.itemDefinitionRepository = itemDefinitionRepository;
    this.itemRequirementRepository = itemRequirementRepository;
    this.catalogService = catalogService;
  }

  @Transactional(readOnly = true)
  public List<ItemDefinition> getAllItems() {
    return itemDefinitionRepository.findAll(Sort.by("type", "name"));
  }

  @Transactional(readOnly = true)
  public ItemDefinition getItem(long id) {
    return itemDefinitionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No catalog item with id " + id));
  }

  @Transactional(readOnly = true)
  public List<ItemRequirement> getRequirements(ItemDefinition item) {
    return itemRequirementRepository.findByItem(item);
  }

  /**
   * Adds a build/research prerequisite to an item: the catalog item with kind
   * {@code requiredKind} must reach {@code requiredLevel}.
   *
   * @throws IllegalArgumentException if either item is unknown, the required
   *                                  level is below 1, or the item already has
   *                                  a requirement on that required item
   */
  @Transactional
  public void addRequirement(long itemId, String requiredKind, int requiredLevel) {
    var item = itemDefinitionRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("No catalog item with id " + itemId));
    var requiredItem = itemDefinitionRepository.findByKind(requiredKind)
        .orElseThrow(() -> new IllegalArgumentException("No catalog item with kind '" + requiredKind + "'"));
    if (requiredLevel < 1) {
      throw new IllegalArgumentException("The required level must be at least 1");
    }
    var alreadyRequired = itemRequirementRepository.findByItem(item).stream()
        .anyMatch(existing -> existing.getRequiredItem().getId() == requiredItem.getId());
    if (alreadyRequired) {
      throw new IllegalArgumentException(
          "The item already has a requirement on '" + requiredKind + "'");
    }

    var requirement = new ItemRequirement();
    requirement.setItem(item);
    requirement.setRequiredItem(requiredItem);
    requirement.setRequiredLevel(requiredLevel);
    itemRequirementRepository.save(requirement);
    // Refresh the in-memory catalog so the requirement takes effect on the running game.
    catalogService.reload();
  }

  /**
   * Removes a build/research prerequisite by its id.
   *
   * @throws IllegalArgumentException if there is no such requirement
   */
  @Transactional
  public void removeRequirement(long requirementId) {
    if (!itemRequirementRepository.existsById(requirementId)) {
      throw new IllegalArgumentException("No item requirement with id " + requirementId);
    }
    itemRequirementRepository.deleteById(requirementId);
    // Refresh the in-memory catalog so the removal takes effect on the running game.
    catalogService.reload();
  }

  /**
   * Applies the edited values to an existing item. Only the fields relevant to
   * the item's {@link com.github.retro_game.retro_game.entity.ItemType} are
   * touched, so the form's unused fields cannot clobber type-specific data.
   */
  @Transactional
  public void updateItem(AdminItemForm form) {
    var item = itemDefinitionRepository.findById(form.getId())
        .orElseThrow(() -> new IllegalArgumentException("No catalog item with id " + form.getId()));
    item.setName(form.getName());
    item.setMetalCost(form.getMetalCost());
    item.setCrystalCost(form.getCrystalCost());
    item.setDeuteriumCost(form.getDeuteriumCost());
    switch (item.getType()) {
      case BUILDING -> {
        item.setCostFactor(form.getCostFactor());
        item.setBaseEnergy(form.getBaseEnergy());
      }
      case TECHNOLOGY -> item.setCostFactor(form.getCostFactor());
      case UNIT -> {
        item.setCapacity(form.getCapacity());
        item.setWeapons(form.getWeapons());
        item.setShield(form.getShield());
        item.setArmor(form.getArmor());
      }
    }
    itemDefinitionRepository.save(item);
    // Refresh the in-memory catalog so the edit takes effect on the running game.
    catalogService.reload();
  }

  /**
   * Creates a brand-new catalog item. The {@code kind} must not already be in
   * use. Only the fields relevant to the chosen {@link ItemType} are applied,
   * mirroring {@link #updateItem}.
   *
   * @throws IllegalArgumentException if the kind is taken, a building or
   *                                  technology has a cost factor below 1, or a
   *                                  unit has no unit type
   */
  @Transactional
  public void createItem(AdminItemCreateForm form) {
    var kind = form.getKind();
    if (itemDefinitionRepository.findByKind(kind).isPresent()) {
      throw new IllegalArgumentException("An item with kind '" + kind + "' already exists");
    }
    var type = form.getType();
    if ((type == ItemType.BUILDING || type == ItemType.TECHNOLOGY) && form.getCostFactor() < 1.0) {
      throw new IllegalArgumentException("The cost factor of a building or technology must be at least 1");
    }
    if (type == ItemType.UNIT && form.getUnitType() == null) {
      throw new IllegalArgumentException("A unit must have a unit type (FLEET or DEFENSE)");
    }

    var item = new ItemDefinition();
    item.setType(type);
    item.setKind(kind);
    item.setName(form.getName());
    item.setMetalCost(form.getMetalCost());
    item.setCrystalCost(form.getCrystalCost());
    item.setDeuteriumCost(form.getDeuteriumCost());
    switch (type) {
      case BUILDING -> {
        item.setCostFactor(form.getCostFactor());
        item.setBaseEnergy(form.getBaseEnergy());
      }
      case TECHNOLOGY -> item.setCostFactor(form.getCostFactor());
      case UNIT -> {
        // Units have a flat cost, but cost_factor is still NOT NULL and >= 1.
        item.setCostFactor(1.0);
        item.setUnitType(form.getUnitType());
        item.setCapacity(form.getCapacity());
        item.setWeapons(form.getWeapons());
        item.setShield(form.getShield());
        item.setArmor(form.getArmor());
      }
    }
    itemDefinitionRepository.save(item);
    // Refresh the in-memory catalog so the new item is visible to the running game.
    catalogService.reload();
  }
}

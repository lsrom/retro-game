package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.repository.ItemDefinitionRepository;
import com.github.retro_game.retro_game.repository.ItemRequirementRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory view of the content catalog, used by the game to read item
 * definitions (costs, cost factors, required energy, ...) at runtime.
 *
 * <p>Part of phase 2 of the data-driven content rebuild. The catalog is loaded
 * from the database on first use and refreshed whenever the admin panel edits
 * an item, so edits take effect on the running game.
 *
 * <p>A static accessor is exposed because some long-standing callers — notably
 * {@code ItemCostUtils} — are static utility classes; routing them through this
 * bean avoids converting them, and their many call sites, to Spring beans.
 */
@Component
public class CatalogService {
  private static volatile CatalogService instance;

  private final ItemDefinitionRepository itemDefinitionRepository;
  private final ItemRequirementRepository itemRequirementRepository;
  private volatile Map<String, ItemDefinition> definitionsByKind = Map.of();
  private volatile Map<String, List<Requirement>> requirementsByKind = Map.of();

  public CatalogService(ItemDefinitionRepository itemDefinitionRepository,
                        ItemRequirementRepository itemRequirementRepository) {
    this.itemDefinitionRepository = itemDefinitionRepository;
    this.itemRequirementRepository = itemRequirementRepository;
    instance = this;
  }

  /**
   * One prerequisite of a catalog item, as plain immutable data — the building
   * or technology {@code requiredKind} must reach {@code requiredLevel} before
   * the owning item becomes available.
   *
   * <p>Unlike the {@link com.github.retro_game.retro_game.entity.ItemRequirement}
   * entity it is copied from, this carries no lazy JPA associations, so it is
   * safe to read outside a transaction.
   *
   * @param requiredKind  the kind of the required item, e.g. {@code "ROBOTICS_FACTORY"}
   * @param requiredType  whether the required item is a building or a technology
   * @param requiredLevel the level the required item must reach
   */
  public record Requirement(String requiredKind, ItemType requiredType, int requiredLevel) {
  }

  /** Returns the singleton instance, for use by static utility code such as ItemCostUtils. */
  public static CatalogService getInstance() {
    return instance;
  }

  /** (Re)loads the catalog from the database. Runs on first use and after an admin edit. */
  public synchronized void reload() {
    var map = new HashMap<String, ItemDefinition>();
    for (var definition : itemDefinitionRepository.findAll()) {
      map.put(definition.getKind(), definition);
    }
    definitionsByKind = Map.copyOf(map);

    // Build requirements as plain immutable data keyed by the owning item's kind.
    // The fetch-joined query resolves both @ManyToOne associations up front, so
    // this is safe even though reload() is not always called inside a transaction.
    var requirementsMap = new HashMap<String, List<Requirement>>();
    for (var requirement : itemRequirementRepository.findAllWithItems()) {
      var requiredItem = requirement.getRequiredItem();
      requirementsMap
          .computeIfAbsent(requirement.getItem().getKind(), k -> new ArrayList<>())
          .add(new Requirement(requiredItem.getKind(), requiredItem.getType(), requirement.getRequiredLevel()));
    }
    var immutable = new HashMap<String, List<Requirement>>();
    requirementsMap.forEach((kind, list) -> immutable.put(kind, List.copyOf(list)));
    requirementsByKind = Map.copyOf(immutable);
  }

  /** Returns the catalog definition for the given item kind, e.g. {@code "METAL_MINE"}. */
  public ItemDefinition getDefinition(String kind) {
    var definition = definitions().get(kind);
    if (definition == null) {
      throw new IllegalStateException("The content catalog has no item with kind " + kind);
    }
    return definition;
  }

  /** Returns whether the catalog contains an item with the given kind. */
  public boolean hasDefinition(String kind) {
    return definitions().containsKey(kind);
  }

  /** Returns every catalog item, ordered by id — i.e. the order they were seeded in. */
  public List<ItemDefinition> getAll() {
    return definitions().values().stream()
        .sorted(Comparator.comparingLong(ItemDefinition::getId))
        .toList();
  }

  /** Returns every catalog item of the given type, ordered by id. */
  public List<ItemDefinition> getAllByType(ItemType type) {
    return getAll().stream().filter(definition -> definition.getType() == type).toList();
  }

  /**
   * Returns the prerequisites of the item with the given kind — the buildings
   * and technologies that must reach a level before it can be built or
   * researched — or an empty list if it has none.
   *
   * <p>Because the catalog is reloaded after every admin edit, this reflects
   * requirement changes made through the admin panel on the running game.
   */
  public List<Requirement> getRequirements(String kind) {
    // Touch definitions() so a first-use reload (which also loads requirements) has run.
    definitions();
    return requirementsByKind.getOrDefault(kind, List.of());
  }

  /** Returns the catalog map, loading it from the database on first use. */
  private Map<String, ItemDefinition> definitions() {
    var map = definitionsByKind;
    if (map.isEmpty()) {
      reload();
      map = definitionsByKind;
    }
    return map;
  }
}

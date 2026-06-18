package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.repository.ItemDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Populates the content catalog ({@code item_definitions} / {@code item_requirements}),
 * the first time the application starts against an empty catalog, by running
 * the checked-in seed script {@code sql/catalog-seed.sql}.
 *
 * <p>The seed script holds the 57 built-in items and their requirements. Up to
 * stage 5 of the data-driven content rebuild the catalog was instead seeded
 * from the hardcoded Java item classes under {@code model/}; stage 6 removed
 * those classes, so the SQL script — generated once from a correctly seeded
 * database — is now the source of the built-in content. A copy of the script
 * lives on the classpath at {@code catalog-seed.sql} so it can be loaded as a
 * resource and executed through the {@link DataSource}.
 *
 * <p>On every start it also verifies the catalog defines every built-in item,
 * failing fast otherwise: the game still resolves those items by their enum
 * name, so a missing row would otherwise surface as a random error mid-game.
 */
@Component
public class CatalogSeeder {
  private static final Logger logger = LoggerFactory.getLogger(CatalogSeeder.class);

  // The catalog seed script, on the classpath (a copy of sql/catalog-seed.sql).
  private static final String SEED_SCRIPT = "catalog-seed.sql";

  private final ItemDefinitionRepository itemDefinitionRepository;
  private final DataSource dataSource;

  public CatalogSeeder(ItemDefinitionRepository itemDefinitionRepository, DataSource dataSource) {
    this.itemDefinitionRepository = itemDefinitionRepository;
    this.dataSource = dataSource;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seedCatalog() {
    if (itemDefinitionRepository.count() > 0) {
      logger.info("Content catalog already populated; skipping seed.");
    } else {
      seedFromScript();
    }
    // Whether freshly seeded or pre-existing, the catalog must define every
    // built-in item; the game still resolves those by their enum name.
    verifyCatalogComplete();
  }

  /**
   * Executes the checked-in {@code catalog-seed.sql} against the database. The
   * script inserts every built-in item definition and requirement with explicit
   * ids and then advances the id sequences, so admin-created items get fresh ids.
   */
  private void seedFromScript() {
    logger.info("Seeding the content catalog from {}...", SEED_SCRIPT);
    var script = new EncodedResource(new ClassPathResource(SEED_SCRIPT), "UTF-8");
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, script);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to seed the content catalog from " + SEED_SCRIPT, e);
    }
    logger.info("Seeded {} item definitions.", itemDefinitionRepository.count());
  }

  /**
   * Asserts the catalog has a definition for every built-in item. The game still
   * resolves buildings, technologies and units by their enum name, so a missing
   * row is a fatal misconfiguration — better caught here, at start-up, than as a
   * random failure once a player reaches that item.
   */
  private void verifyCatalogComplete() {
    var existingKinds = new HashSet<String>();
    for (var definition : itemDefinitionRepository.findAll()) {
      existingKinds.add(definition.getKind());
    }

    var missing = new ArrayList<String>();
    for (var kind : BuildingKind.values()) {
      if (!existingKinds.contains(kind.name())) {
        missing.add(kind.name());
      }
    }
    for (var kind : TechnologyKind.values()) {
      if (!existingKinds.contains(kind.name())) {
        missing.add(kind.name());
      }
    }
    for (var kind : UnitKind.values()) {
      if (!existingKinds.contains(kind.name())) {
        missing.add(kind.name());
      }
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "The content catalog is missing definitions for: " + String.join(", ", missing)
              + ". Every built-in BuildingKind, TechnologyKind and UnitKind must have a matching "
              + "item_definitions row.");
    }
    var builtInCount = BuildingKind.values().length + TechnologyKind.values().length
        + UnitKind.values().length;
    logger.info("Content catalog verified: all {} built-in items are present.", builtInCount);
  }
}

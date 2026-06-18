package com.github.retro_game.retro_game.entity;

import java.io.Serializable;

/**
 * The stored (jsonb) shapes of the three construction-queue columns.
 *
 * <p>The queues used to be packed into {@code int[]}/{@code long[]} columns
 * with each item's kind stored as a Java enum {@code ordinal()}. That made the
 * stored data positional: reordering an enum constant silently corrupted every
 * queue. These records persist each entry's kind (and action) as the stable
 * item-<em>name</em> string instead, so the stored data no longer depends on
 * enum order.
 *
 * <p>Each record is the element type of a JSON array held in one jsonb column.
 * They are deliberately plain — a {@code String} kind, not an enum — so
 * Jackson/hypersistence map them without a custom (de)serializer. The
 * {@code Body}/{@code User} accessors translate between these stored records
 * and the enum-typed {@code BuildingQueueEntry}/{@code ShipyardQueueEntry}/
 * {@code TechnologyQueueEntry} the rest of the game works with.
 */
final class QueueEntries {
  private QueueEntries() {
  }

  /**
   * One entry of {@code bodies.building_queue}.
   *
   * @param sequence the entry's position key within the queue
   * @param kind     a {@link BuildingKind} name
   * @param action   a {@link BuildingQueueAction} name
   */
  // Serializable so it is safe as a field of the Serializable Body entity.
  record StoredBuildingQueueEntry(int sequence, String kind, String action) implements Serializable {
  }

  /**
   * One entry of {@code bodies.shipyard_queue} (an ordered list).
   *
   * @param kind  a {@link UnitKind} name
   * @param count how many units this entry builds
   */
  // Serializable so it is safe as a field of the Serializable Body entity.
  record StoredShipyardQueueEntry(String kind, int count) implements Serializable {
  }

  /**
   * One entry of {@code users.technology_queue}.
   *
   * @param sequence the entry's position key within the queue
   * @param kind     a {@link TechnologyKind} name
   * @param bodyId   the body the research is queued on
   */
  record StoredTechnologyQueueEntry(int sequence, String kind, long bodyId) implements Serializable {
  }
}

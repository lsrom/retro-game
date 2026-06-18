package com.github.retro_game.retro_game.entity;

import com.github.retro_game.retro_game.entity.QueueEntries.StoredBuildingQueueEntry;
import com.github.retro_game.retro_game.entity.QueueEntries.StoredShipyardQueueEntry;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "bodies")
public class Body implements Serializable {
  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @Embedded
  private Coordinates coordinates;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAt;

  @Column(name = "updated_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedAt;

  @Column(name = "diameter", nullable = false, updatable = false)
  private int diameter;

  @Column(name = "temperature", nullable = false, updatable = false)
  private int temperature;

  @Column(name = "type", nullable = false, updatable = false)
  private BodyType type;

  @Column(name = "image", nullable = false)
  private int image;

  @Embedded
  private Resources resources;

  @Embedded
  private ProductionFactors productionFactors;

  @Column(name = "last_jump_at")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastJumpAt;

  @Column(name = "shipyard_start_at")
  @Temporal(TemporalType.TIMESTAMP)
  private Date shipyardStartAt;

  // Building levels and unit counts, each keyed by item name, e.g. {"METAL_MINE": 5}.
  // An item absent from the map counts as 0.
  @Column(name = "buildings", nullable = false)
  @Type(JsonBinaryType.class)
  private Map<String, Integer> buildings = new HashMap<>();

  @Column(name = "units", nullable = false)
  @Type(JsonBinaryType.class)
  private Map<String, Integer> units = new HashMap<>();

  // The construction queues, each a JSON array of objects whose kind/action are
  // stored as stable item-name strings, e.g. [{"sequence": 1, "kind":
  // "METAL_MINE", "action": "CONSTRUCT"}]. The public accessors below convert
  // between these stored lists and the enum-typed queue types the game uses.
  @Column(name = "building_queue", nullable = false)
  @Type(JsonBinaryType.class)
  private List<StoredBuildingQueueEntry> buildingQueue = new ArrayList<>();

  @Column(name = "shipyard_queue", nullable = false)
  @Type(JsonBinaryType.class)
  private List<StoredShipyardQueueEntry> shipyardQueue = new ArrayList<>();

  public long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(Coordinates coordinates) {
    this.coordinates = coordinates;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public int getDiameter() {
    return diameter;
  }

  public void setDiameter(int diameter) {
    this.diameter = diameter;
  }

  public int getTemperature() {
    return temperature;
  }

  public void setTemperature(int temperature) {
    this.temperature = temperature;
  }

  public BodyType getType() {
    return type;
  }

  public void setType(BodyType type) {
    this.type = type;
  }

  public int getImage() {
    return image;
  }

  public void setImage(int image) {
    this.image = image;
  }

  public Resources getResources() {
    return resources;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  public ProductionFactors getProductionFactors() {
    return productionFactors;
  }

  public void setProductionFactors(ProductionFactors productionFactors) {
    this.productionFactors = productionFactors;
  }

  public Date getLastJumpAt() {
    return lastJumpAt;
  }

  public void setLastJumpAt(Date lastJumpAt) {
    this.lastJumpAt = lastJumpAt;
  }

  public Date getShipyardStartAt() {
    return shipyardStartAt;
  }

  public void setShipyardStartAt(Date shipyardStartAt) {
    this.shipyardStartAt = shipyardStartAt;
  }

  public EnumMap<BuildingKind, Integer> getBuildings() {
    return ItemMaps.toEnumMap(BuildingKind.class, buildings);
  }

  public void setBuildings(Map<BuildingKind, Integer> buildings) {
    this.buildings = ItemMaps.toStored(buildings);
  }

  public int getBuildingLevel(BuildingKind kind) {
    return ItemMaps.get(buildings, kind);
  }

  public void setBuildingLevel(BuildingKind kind, int level) {
    assert level >= 0;
    buildings.put(kind.name(), level);
  }

  public EnumMap<UnitKind, Integer> getUnits() {
    return ItemMaps.toEnumMap(UnitKind.class, units);
  }

  public void setUnits(Map<UnitKind, Integer> units) {
    this.units = ItemMaps.toStored(units);
  }

  public int getUnitsCount(UnitKind kind) {
    return ItemMaps.get(units, kind);
  }

  public void setUnitsCount(UnitKind kind, int count) {
    assert count >= 0;
    units.put(kind.name(), count);
  }

  public int getTotalUnitsCount() {
    return units.values().stream().mapToInt(Integer::intValue).sum();
  }

  public SortedMap<Integer, BuildingQueueEntry> getBuildingQueue() {
    var queue = new TreeMap<Integer, BuildingQueueEntry>();
    for (var entry : buildingQueue) {
      var kind = BuildingKind.valueOf(entry.kind());
      var action = BuildingQueueAction.valueOf(entry.action());
      queue.put(entry.sequence(), new BuildingQueueEntry(kind, action));
    }
    return queue;
  }

  public void setBuildingQueue(SortedMap<Integer, BuildingQueueEntry> queue) {
    var list = new ArrayList<StoredBuildingQueueEntry>(queue.size());
    for (var entry : queue.entrySet()) {
      list.add(new StoredBuildingQueueEntry(
          entry.getKey(), entry.getValue().kind().name(), entry.getValue().action().name()));
    }
    buildingQueue = list;
  }

  public List<ShipyardQueueEntry> getShipyardQueue() {
    var queue = new ArrayList<ShipyardQueueEntry>(shipyardQueue.size());
    for (var entry : shipyardQueue) {
      queue.add(new ShipyardQueueEntry(UnitKind.valueOf(entry.kind()), entry.count()));
    }
    return queue;
  }

  public void setShipyardQueue(List<ShipyardQueueEntry> queue) {
    var list = new ArrayList<StoredShipyardQueueEntry>(queue.size());
    for (var entry : queue) {
      list.add(new StoredShipyardQueueEntry(entry.kind().name(), entry.count()));
    }
    shipyardQueue = list;
  }
}

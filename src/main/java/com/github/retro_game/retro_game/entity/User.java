package com.github.retro_game.retro_game.entity;

import com.github.retro_game.retro_game.entity.QueueEntries.StoredTechnologyQueueEntry;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.springframework.data.domain.Sort;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "users")
public class User {
  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "roles", nullable = false)
  private int roles;

  @Column(name = "private_received_messages_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date privateReceivedMessagesSeenAt;

  @Column(name = "alliance_messages_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date allianceMessagesSeenAt;

  @Column(name = "broadcast_messages_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date broadcastMessagesSeenAt;

  @Column(name = "combat_reports_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date combatReportsSeenAt;

  @Column(name = "espionage_reports_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date espionageReportsSeenAt;

  @Column(name = "harvest_reports_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date harvestReportsSeenAt;

  @Column(name = "transport_reports_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date transportReportsSeenAt;

  @Column(name = "other_reports_seen_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date otherReportsSeenAt;

  @Column(name = "language", nullable = false)
  private String language;

  @Column(name = "skin", nullable = false)
  private String skin;

  @Column(name = "num_probes", nullable = false)
  private int numProbes;

  @Column(name = "bodies_sort_order", nullable = false)
  private BodiesSortOrder bodiesSortOrder;

  @Column(name = "bodies_sort_direction", nullable = false)
  private Sort.Direction bodiesSortDirection;

  @Column(name = "flags", nullable = false)
  private int flags;

  @Column(name = "vacation_until")
  @Temporal(TemporalType.TIMESTAMP)
  private Date vacationUntil;

  @Column(name = "forced_vacation", nullable = false)
  private boolean forcedVacation;

  // Technology levels, keyed by item name, e.g. {"ENERGY_TECHNOLOGY": 5}.
  // A technology absent from the map counts as 0.
  @Column(name = "technologies", nullable = false)
  @Type(JsonBinaryType.class)
  private Map<String, Integer> technologies = new HashMap<>();

  // The research queue, a JSON array of objects whose kind is stored as a
  // stable item-name string, e.g. [{"sequence": 1, "kind": "ASTROPHYSICS",
  // "bodyId": 42}]. The public accessors below convert between this stored
  // list and the enum-typed queue type the game uses.
  @Column(name = "technology_queue", nullable = false)
  @Type(JsonBinaryType.class)
  private List<StoredTechnologyQueueEntry> technologyQueue = new ArrayList<>();

  @OneToMany(mappedBy = "user")
  @MapKey(name = "id")
  // The SortedMap is already ordered by its key (the body id); Hibernate 6
  // rejects pairing a sorted collection with @OrderBy.
  private SortedMap<Long, Body> bodies;

  @JoinTable(
      name = "party_users",
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "party_id", referencedColumnName = "id"))
  @ManyToMany
  private List<Party> parties;

  public boolean hasRole(int role) {
    assert (role & (role - 1)) == 0;
    return (roles & role) != 0;
  }

  public boolean hasFlag(int flag) {
    assert (flag & (flag - 1)) == 0;
    return (flags & flag) != 0;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getRoles() {
    return roles;
  }

  public void setRoles(int roles) {
    this.roles = roles;
  }

  public Date getPrivateReceivedMessagesSeenAt() {
    return privateReceivedMessagesSeenAt;
  }

  public void setPrivateReceivedMessagesSeenAt(Date privateReceivedMessagesSeenAt) {
    this.privateReceivedMessagesSeenAt = privateReceivedMessagesSeenAt;
  }

  public Date getAllianceMessagesSeenAt() {
    return allianceMessagesSeenAt;
  }

  public void setAllianceMessagesSeenAt(Date allianceMessagesSeenAt) {
    this.allianceMessagesSeenAt = allianceMessagesSeenAt;
  }

  public Date getBroadcastMessagesSeenAt() {
    return broadcastMessagesSeenAt;
  }

  public void setBroadcastMessagesSeenAt(Date broadcastMessagesSeenAt) {
    this.broadcastMessagesSeenAt = broadcastMessagesSeenAt;
  }

  public Date getCombatReportsSeenAt() {
    return combatReportsSeenAt;
  }

  public void setCombatReportsSeenAt(Date combatReportsSeenAt) {
    this.combatReportsSeenAt = combatReportsSeenAt;
  }

  public Date getEspionageReportsSeenAt() {
    return espionageReportsSeenAt;
  }

  public void setEspionageReportsSeenAt(Date espionageReportsSeenAt) {
    this.espionageReportsSeenAt = espionageReportsSeenAt;
  }

  public Date getHarvestReportsSeenAt() {
    return harvestReportsSeenAt;
  }

  public void setHarvestReportsSeenAt(Date harvestReportsSeenAt) {
    this.harvestReportsSeenAt = harvestReportsSeenAt;
  }

  public Date getTransportReportsSeenAt() {
    return transportReportsSeenAt;
  }

  public void setTransportReportsSeenAt(Date transportReportsSeenAt) {
    this.transportReportsSeenAt = transportReportsSeenAt;
  }

  public Date getOtherReportsSeenAt() {
    return otherReportsSeenAt;
  }

  public void setOtherReportsSeenAt(Date otherReportsSeenAt) {
    this.otherReportsSeenAt = otherReportsSeenAt;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getSkin() {
    return skin;
  }

  public void setSkin(String skin) {
    this.skin = skin;
  }

  public int getNumProbes() {
    return numProbes;
  }

  public void setNumProbes(int numProbes) {
    this.numProbes = numProbes;
  }

  public BodiesSortOrder getBodiesSortOrder() {
    return bodiesSortOrder;
  }

  public void setBodiesSortOrder(BodiesSortOrder bodiesSortOrder) {
    this.bodiesSortOrder = bodiesSortOrder;
  }

  public Sort.Direction getBodiesSortDirection() {
    return bodiesSortDirection;
  }

  public void setBodiesSortDirection(Sort.Direction bodiesSortDirection) {
    this.bodiesSortDirection = bodiesSortDirection;
  }

  public int getFlags() {
    return flags;
  }

  public void setFlags(int flags) {
    this.flags = flags;
  }

  public Date getVacationUntil() {
    return vacationUntil;
  }

  public void setVacationUntil(Date vacationUntil) {
    this.vacationUntil = vacationUntil;
  }

  public boolean isForcedVacation() {
    return forcedVacation;
  }

  public void setForcedVacation(boolean forcedVacation) {
    this.forcedVacation = forcedVacation;
  }

  public EnumMap<TechnologyKind, Integer> getTechnologies() {
    return ItemMaps.toEnumMap(TechnologyKind.class, technologies);
  }

  public void setTechnologies(Map<TechnologyKind, Integer> technologies) {
    this.technologies = ItemMaps.toStored(technologies);
  }

  public int getTechnologyLevel(TechnologyKind kind) {
    return ItemMaps.get(technologies, kind);
  }

  public void setTechnologyLevel(TechnologyKind kind, int level) {
    assert level >= 0;
    technologies.put(kind.name(), level);
  }

  public SortedMap<Integer, TechnologyQueueEntry> getTechnologyQueue() {
    var queue = new TreeMap<Integer, TechnologyQueueEntry>();
    for (var entry : technologyQueue) {
      var kind = TechnologyKind.valueOf(entry.kind());
      queue.put(entry.sequence(), new TechnologyQueueEntry(kind, entry.bodyId()));
    }
    return queue;
  }

  public void setTechnologyQueue(SortedMap<Integer, TechnologyQueueEntry> queue) {
    var list = new ArrayList<StoredTechnologyQueueEntry>(queue.size());
    for (var entry : queue.entrySet()) {
      list.add(new StoredTechnologyQueueEntry(
          entry.getKey(), entry.getValue().kind().name(), entry.getValue().bodyId()));
    }
    technologyQueue = list;
  }

  public SortedMap<Long, Body> getBodies() {
    return bodies;
  }

  public List<Party> getParties() {
    return parties;
  }
}

package com.github.retro_game.retro_game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A prerequisite of a catalog item: the {@code requiredItem} — a building or
 * technology — must reach {@code requiredLevel} before {@code item} becomes
 * available to build or research.
 */
@Entity
@Table(name = "item_requirements")
@Getter
@Setter
public class ItemRequirement {
  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @JoinColumn(name = "item_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private ItemDefinition item;

  @JoinColumn(name = "required_item_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private ItemDefinition requiredItem;

  @Column(name = "required_level", nullable = false)
  private int requiredLevel;
}

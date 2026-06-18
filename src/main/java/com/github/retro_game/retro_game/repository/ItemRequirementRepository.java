package com.github.retro_game.retro_game.repository;

import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRequirementRepository extends JpaRepository<ItemRequirement, Long> {
  List<ItemRequirement> findByItem(ItemDefinition item);

  /**
   * Every requirement with both the owning item and the required item eagerly
   * fetched. Used by {@code CatalogService.reload()}, which is not always run
   * inside a transaction and so cannot resolve the lazy {@code @ManyToOne}
   * associations afterwards.
   */
  @Query("select r from ItemRequirement r join fetch r.item join fetch r.requiredItem")
  List<ItemRequirement> findAllWithItems();
}

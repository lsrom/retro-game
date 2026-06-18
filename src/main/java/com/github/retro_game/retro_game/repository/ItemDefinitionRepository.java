package com.github.retro_game.retro_game.repository;

import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemDefinitionRepository extends JpaRepository<ItemDefinition, Long> {
  List<ItemDefinition> findByTypeOrderByNameAsc(ItemType type);

  Optional<ItemDefinition> findByKind(String kind);
}

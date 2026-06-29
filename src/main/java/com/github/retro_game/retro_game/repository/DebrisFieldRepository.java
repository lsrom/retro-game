package com.github.retro_game.retro_game.repository;

import com.github.retro_game.retro_game.entity.DebrisField;
import com.github.retro_game.retro_game.entity.DebrisFieldKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DebrisFieldRepository extends JpaRepository<DebrisField, DebrisFieldKey> {
  boolean existsByKey_GalaxyAndKey_SystemAndKey_Position(int galaxy, int system, int position);

  Optional<DebrisField> findByKey_GalaxyAndKey_SystemAndKey_Position(int galaxy, int system, int position);
}

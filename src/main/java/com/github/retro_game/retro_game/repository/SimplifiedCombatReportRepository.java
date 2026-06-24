package com.github.retro_game.retro_game.repository;

import com.github.retro_game.retro_game.entity.CoordinatesKind;
import com.github.retro_game.retro_game.entity.CombatResult;
import com.github.retro_game.retro_game.entity.SimplifiedCombatReport;
import com.github.retro_game.retro_game.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface SimplifiedCombatReportRepository extends JpaRepository<SimplifiedCombatReport, Long>,
    SimplifiedCombatReportRepositoryCustom {
  int countByUserAndDeletedIsFalseAndAtAfter(User user, Date at);

  @Transactional
  @Modifying
  @Query("update SimplifiedCombatReport set deleted = true where user.id = ?1")
  void markAllAsDeletedByUserId(long userId);

  List<SimplifiedCombatReport>
  findByUserAndDeletedIsFalseAndResultAndCoordinates_GalaxyAndCoordinates_SystemAndCoordinates_KindAndCombatReportIdIsNotNullOrderByAtDesc(
      User user, CombatResult result, int galaxy, int system, CoordinatesKind kind);

  List<SimplifiedCombatReport>
  findByUserAndDeletedIsFalseAndResultAndCoordinates_GalaxyAndCoordinates_SystemAndCoordinates_PositionAndCoordinates_KindAndCombatReportIdIsNotNullOrderByAtDesc(
      User user, CombatResult result, int galaxy, int system, int position, CoordinatesKind kind);
}

package com.github.retro_game.retro_game.repository;

import com.github.retro_game.retro_game.entity.AutomaticResourceTransfer;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AutomaticResourceTransferRepository extends JpaRepository<AutomaticResourceTransfer, Long> {
  List<AutomaticResourceTransfer> findBySourceBody_IdAndUser_IdOrderById(long sourceBodyId, long userId);

  long countBySourceBody_IdAndUser_Id(long sourceBodyId, long userId);

  @Query("""
      select transfer.id
      from AutomaticResourceTransfer transfer
      where transfer.enabled = true and transfer.nextRunAt <= ?1
      order by transfer.nextRunAt, transfer.id
      """)
  List<Long> findDueIds(Date now, Pageable pageable);
}

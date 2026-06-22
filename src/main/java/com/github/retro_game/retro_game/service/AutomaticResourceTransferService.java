package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.controller.form.AutomaticResourceTransferForm;
import com.github.retro_game.retro_game.dto.AutomaticResourceTransferDto;
import java.util.List;

public interface AutomaticResourceTransferService {
  List<AutomaticResourceTransferDto> getTransfers(long bodyId);

  void create(long bodyId, AutomaticResourceTransferForm form);

  void toggle(long bodyId, long transferId);

  void delete(long bodyId, long transferId);

  List<Long> getDueTransferIds(int limit);

  void dispatch(long transferId);
}

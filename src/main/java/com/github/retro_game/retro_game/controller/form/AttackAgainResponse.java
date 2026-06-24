package com.github.retro_game.retro_game.controller.form;

import com.github.retro_game.retro_game.dto.UnitKindDto;

import java.util.Map;

public class AttackAgainResponse {
  private boolean success;
  private String error;
  private Map<UnitKindDto, Integer> units;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Map<UnitKindDto, Integer> getUnits() {
    return units;
  }

  public void setUnits(Map<UnitKindDto, Integer> units) {
    this.units = units;
  }
}

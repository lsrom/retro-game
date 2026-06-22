package com.github.retro_game.retro_game.controller.form;

import com.github.retro_game.retro_game.dto.UnitKindDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;

public class AutomaticResourceTransferForm {
  private long body;

  @Min(1)
  private long targetBody;

  @NotNull
  private UnitKindDto shipKind;

  @Min(1)
  private int shipCount;

  @Min(0)
  private long metal;

  @Min(0)
  private long crystal;

  @Min(0)
  private long deuterium;

  @Range(min = 1, max = 10)
  private int speedFactor;

  @NotNull
  @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
  private String runTime;

  public long getBody() {
    return body;
  }

  public void setBody(long body) {
    this.body = body;
  }

  public long getTargetBody() {
    return targetBody;
  }

  public void setTargetBody(long targetBody) {
    this.targetBody = targetBody;
  }

  public UnitKindDto getShipKind() {
    return shipKind;
  }

  public void setShipKind(UnitKindDto shipKind) {
    this.shipKind = shipKind;
  }

  public int getShipCount() {
    return shipCount;
  }

  public void setShipCount(int shipCount) {
    this.shipCount = shipCount;
  }

  public long getMetal() {
    return metal;
  }

  public void setMetal(long metal) {
    this.metal = metal;
  }

  public long getCrystal() {
    return crystal;
  }

  public void setCrystal(long crystal) {
    this.crystal = crystal;
  }

  public long getDeuterium() {
    return deuterium;
  }

  public void setDeuterium(long deuterium) {
    this.deuterium = deuterium;
  }

  public int getSpeedFactor() {
    return speedFactor;
  }

  public void setSpeedFactor(int speedFactor) {
    this.speedFactor = speedFactor;
  }

  public String getRunTime() {
    return runTime;
  }

  public void setRunTime(String runTime) {
    this.runTime = runTime;
  }
}

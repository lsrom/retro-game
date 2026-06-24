package com.github.retro_game.retro_game.controller.form;

import com.github.retro_game.retro_game.dto.CoordinatesKindDto;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotNull;

public class AttackAgainRequest {
  private long body;

  @Range(min = 1, max = 5)
  private int galaxy;

  @Range(min = 1, max = 500)
  private int system;

  @Range(min = 1, max = 15)
  private int position;

  @NotNull
  private CoordinatesKindDto kind;

  public long getBody() {
    return body;
  }

  public void setBody(long body) {
    this.body = body;
  }

  public int getGalaxy() {
    return galaxy;
  }

  public void setGalaxy(int galaxy) {
    this.galaxy = galaxy;
  }

  public int getSystem() {
    return system;
  }

  public void setSystem(int system) {
    this.system = system;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public CoordinatesKindDto getKind() {
    return kind;
  }

  public void setKind(CoordinatesKindDto kind) {
    this.kind = kind;
  }
}

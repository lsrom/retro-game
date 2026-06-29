package com.github.retro_game.retro_game.entity;

public enum ExpeditionEventType {
  Nothing(50),
  Delay(120),
  FleetLoss(10),
  Pirates(200),
  Aliens(100),
  OreAsteroid(400),
  GasCloud(200),
  SpectacularSupernova(50),
  WarpWindow(80),
  RescueShips(150),
  RescueFleet(60);

  private final int weight;

  ExpeditionEventType(int weight) {
    this.weight = weight;
  }

  public int getWeight() {
    return weight;
  }
}

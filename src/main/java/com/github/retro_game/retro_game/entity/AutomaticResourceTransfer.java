package com.github.retro_game.retro_game.entity;

import com.github.retro_game.retro_game.battleengine.UnitKind;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "automatic_resource_transfers")
public class AutomaticResourceTransfer {
  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @JoinColumn(name = "source_body_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Body sourceBody;

  @JoinColumn(name = "target_body_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Body targetBody;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "ship_kind", nullable = false)
  private UnitKind shipKind;

  @Column(name = "ship_count", nullable = false)
  private int shipCount;

  @Embedded
  private Resources resources;

  @Column(name = "speed_factor", nullable = false)
  private int speedFactor;

  @Column(name = "run_hour", nullable = false)
  private int runHour;

  @Column(name = "run_minute", nullable = false)
  private int runMinute;

  @Column(name = "next_run_at", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date nextRunAt;

  @Column(name = "last_run_at")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastRunAt;

  @Column(name = "last_error")
  private String lastError;

  public long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Body getSourceBody() {
    return sourceBody;
  }

  public void setSourceBody(Body sourceBody) {
    this.sourceBody = sourceBody;
  }

  public Body getTargetBody() {
    return targetBody;
  }

  public void setTargetBody(Body targetBody) {
    this.targetBody = targetBody;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public UnitKind getShipKind() {
    return shipKind;
  }

  public void setShipKind(UnitKind shipKind) {
    this.shipKind = shipKind;
  }

  public int getShipCount() {
    return shipCount;
  }

  public void setShipCount(int shipCount) {
    this.shipCount = shipCount;
  }

  public Resources getResources() {
    return resources;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  public int getSpeedFactor() {
    return speedFactor;
  }

  public void setSpeedFactor(int speedFactor) {
    this.speedFactor = speedFactor;
  }

  public int getRunHour() {
    return runHour;
  }

  public void setRunHour(int runHour) {
    this.runHour = runHour;
  }

  public int getRunMinute() {
    return runMinute;
  }

  public void setRunMinute(int runMinute) {
    this.runMinute = runMinute;
  }

  public Date getNextRunAt() {
    return nextRunAt;
  }

  public void setNextRunAt(Date nextRunAt) {
    this.nextRunAt = nextRunAt;
  }

  public Date getLastRunAt() {
    return lastRunAt;
  }

  public void setLastRunAt(Date lastRunAt) {
    this.lastRunAt = lastRunAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}

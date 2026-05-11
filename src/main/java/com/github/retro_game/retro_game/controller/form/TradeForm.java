package com.github.retro_game.retro_game.controller.form;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TradeForm {
  private long body;

  @NotBlank
  private String tradedResourceKey;

  @NotNull
  @Min(0)
  private Double expectedReceivedAmount;

  @NotNull
  @Min(0)
  private Double metalAmount;

  @NotNull
  @Min(0)
  private Double crystalAmount;

  @NotNull
  @Min(0)
  private Double deuteriumAmount;

  public long getBody() {
    return body;
  }

  public void setBody(long body) {
    this.body = body;
  }

  public String getTradedResourceKey() {
    return tradedResourceKey;
  }

  public void setTradedResourceKey(String tradedResourceKey) {
    this.tradedResourceKey = tradedResourceKey;
  }

  public Double getExpectedReceivedAmount() {
    return expectedReceivedAmount;
  }

  public void setExpectedReceivedAmount(Double expectedReceivedAmount) {
    this.expectedReceivedAmount = expectedReceivedAmount;
  }

  public Double getMetalAmount() {
    return metalAmount;
  }

  public void setMetalAmount(Double metalAmount) {
    this.metalAmount = metalAmount;
  }

  public Double getCrystalAmount() {
    return crystalAmount;
  }

  public void setCrystalAmount(Double crystalAmount) {
    this.crystalAmount = crystalAmount;
  }

  public Double getDeuteriumAmount() {
    return deuteriumAmount;
  }

  public void setDeuteriumAmount(Double deuteriumAmount) {
    this.deuteriumAmount = deuteriumAmount;
  }
}

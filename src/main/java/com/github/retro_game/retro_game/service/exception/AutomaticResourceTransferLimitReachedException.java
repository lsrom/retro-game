package com.github.retro_game.retro_game.service.exception;

public class AutomaticResourceTransferLimitReachedException extends ServiceException {
  public AutomaticResourceTransferLimitReachedException() {
    super("Automatic resource transfer limit reached");
  }
}

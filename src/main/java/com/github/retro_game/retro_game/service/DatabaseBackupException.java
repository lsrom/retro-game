package com.github.retro_game.retro_game.service;

public class DatabaseBackupException extends RuntimeException {
  public DatabaseBackupException(String message) {
    super(message);
  }

  public DatabaseBackupException(String message, Throwable cause) {
    super(message, cause);
  }
}

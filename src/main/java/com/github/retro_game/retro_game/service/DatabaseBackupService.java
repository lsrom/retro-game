package com.github.retro_game.retro_game.service;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DatabaseBackupService {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
  private static final int MAX_ERROR_MESSAGE_LENGTH = 4_000;

  private final DataSource dataSource;
  private final String databaseUrl;
  private final String username;
  private final String password;
  private final String pgDumpCommand;
  private final String pgRestoreCommand;
  private final String psqlCommand;
  private final ReentrantLock operationLock = new ReentrantLock();

  public DatabaseBackupService(
      DataSource dataSource,
      @Value("${spring.datasource.url}") String databaseUrl,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password:}") String password,
      @Value("${retro-game.database-backup.pg-dump-command:pg_dump}") String pgDumpCommand,
      @Value("${retro-game.database-backup.pg-restore-command:pg_restore}") String pgRestoreCommand,
      @Value("${retro-game.database-backup.psql-command:psql}") String psqlCommand) {
    this.dataSource = dataSource;
    this.databaseUrl = databaseUrl;
    this.username = username;
    this.password = password;
    this.pgDumpCommand = pgDumpCommand;
    this.pgRestoreCommand = pgRestoreCommand;
    this.psqlCommand = psqlCommand;
  }

  public Path createBackup() {
    operationLock.lock();
    Path backup = null;
    try {
      backup = Files.createTempFile("retro-game-backup-", ".dump");
      var command = new ArrayList<String>();
      command.add(pgDumpCommand);
      command.add("--format=custom");
      command.add("--no-owner");
      command.add("--no-privileges");
      command.add("--file=" + backup);
      command.addAll(connectionArguments());
      run(command, "Database backup failed");
      return backup;
    } catch (IOException e) {
      deleteQuietly(backup);
      throw new DatabaseBackupException("Could not create a temporary backup file", e);
    } catch (RuntimeException e) {
      deleteQuietly(backup);
      throw e;
    } finally {
      operationLock.unlock();
    }
  }

  public void restore(Path backup) {
    operationLock.lock();
    Path restoreScript = null;
    Path compatibleRestoreScript = null;
    try {
      run(List.of(pgRestoreCommand, "--list", backup.toString()),
          "The uploaded file is not a valid PostgreSQL backup");

      restoreScript = Files.createTempFile("retro-game-restore-", ".sql");
      run(List.of(
          pgRestoreCommand,
          "--clean",
          "--if-exists",
          "--no-owner",
          "--no-privileges",
          "--file=" + restoreScript,
          backup.toString()), "Could not read the database backup");

      compatibleRestoreScript = Files.createTempFile("retro-game-restore-compatible-", ".sql");
      removeUnsupportedSettings(restoreScript, compatibleRestoreScript);

      var psql = new ArrayList<String>();
      psql.add(psqlCommand);
      psql.add("--single-transaction");
      psql.add("--set=ON_ERROR_STOP=1");
      psql.add("--file=" + compatibleRestoreScript);
      psql.addAll(connectionArguments());
      run(psql, "Database restore failed");
      evictDatabaseConnections();
    } catch (IOException e) {
      throw new DatabaseBackupException("Could not prepare the database restore", e);
    } finally {
      deleteQuietly(restoreScript);
      deleteQuietly(compatibleRestoreScript);
      operationLock.unlock();
    }
  }

  private void evictDatabaseConnections() {
    try {
      HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
      var pool = hikariDataSource.getHikariPoolMXBean();
      if (pool != null) {
        pool.softEvictConnections();
      }
    } catch (Exception e) {
      // The restore itself succeeded. Log pool cleanup failures without
      // incorrectly reporting that the database restore was rolled back.
      logger.warn("Database restored, but existing pooled connections could not be evicted", e);
    }
  }

  private static void removeUnsupportedSettings(Path source, Path destination) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        // PostgreSQL 17+ clients emit this setting, but PostgreSQL 12-16 servers
        // do not recognize it. It only controls session timeout behavior and is
        // not part of the backed-up data or schema.
        if (line.equals("SET transaction_timeout = 0;")) {
          continue;
        }
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private List<String> connectionArguments() {
    DatabaseLocation location = DatabaseLocation.fromJdbcUrl(databaseUrl);
    var arguments = new ArrayList<String>();
    arguments.add("--host=" + location.host());
    arguments.add("--port=" + location.port());
    arguments.add("--username=" + username);
    arguments.add("--dbname=" + location.database());
    return arguments;
  }

  private void run(List<String> command, String failureMessage) {
    Path output = null;
    try {
      output = Files.createTempFile("retro-game-postgres-", ".log");
      var processBuilder = new ProcessBuilder(command)
          .redirectErrorStream(true)
          .redirectOutput(output.toFile());
      processBuilder.environment().put("PGPASSWORD", password);

      Process process = processBuilder.start();
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        String detail = Files.readString(output, StandardCharsets.UTF_8).trim();
        if (detail.length() > MAX_ERROR_MESSAGE_LENGTH) {
          detail = detail.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        throw new DatabaseBackupException(
            detail.isEmpty() ? failureMessage : failureMessage + ": " + detail);
      }
    } catch (IOException e) {
      throw new DatabaseBackupException(
          failureMessage + ". Ensure pg_dump, pg_restore, and psql are installed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DatabaseBackupException(failureMessage + ": operation interrupted", e);
    } finally {
      deleteQuietly(output);
    }
  }

  public static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Temporary files are also cleaned by the operating system.
    }
  }

  private record DatabaseLocation(String host, int port, String database) {
    private static DatabaseLocation fromJdbcUrl(String jdbcUrl) {
      if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
        throw new DatabaseBackupException(
            "Database backup requires a PostgreSQL JDBC URL with an explicit host");
      }
      try {
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String path = uri.getPath();
        if (uri.getHost() == null || path == null || path.length() <= 1) {
          throw new IllegalArgumentException("Missing database host or name");
        }
        String database = path.substring(1);
        return new DatabaseLocation(uri.getHost(), uri.getPort() < 0 ? 5432 : uri.getPort(), database);
      } catch (IllegalArgumentException e) {
        throw new DatabaseBackupException("Invalid PostgreSQL JDBC URL", e);
      }
    }
  }
}

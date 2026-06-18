package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.service.DatabaseBackupException;
import com.github.retro_game.retro_game.service.DatabaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class AdminDatabaseController {
  private static final Logger logger = LoggerFactory.getLogger(AdminDatabaseController.class);
  private static final DateTimeFormatter BACKUP_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final DatabaseBackupService databaseBackupService;
  private final long maxBackupSize;
  private final Clock clock;

  @Autowired
  public AdminDatabaseController(
      DatabaseBackupService databaseBackupService,
      @Value("${retro-game.database-backup.max-size:1GB}") DataSize maxBackupSize) {
    this(databaseBackupService, maxBackupSize, Clock.systemDefaultZone());
  }

  AdminDatabaseController(DatabaseBackupService databaseBackupService, DataSize maxBackupSize, Clock clock) {
    this.databaseBackupService = databaseBackupService;
    this.maxBackupSize = maxBackupSize.toBytes();
    this.clock = clock;
  }

  @GetMapping("/admin/database")
  public String database() {
    return "admin-database";
  }

  @GetMapping("/admin/database/backup")
  public ResponseEntity<StreamingResponseBody> backup() {
    Path backup = databaseBackupService.createBackup();
    String filename = "retro-game-" + LocalDateTime.now(clock).format(BACKUP_TIMESTAMP) + ".dump";
    StreamingResponseBody body = outputStream -> {
      try {
        Files.copy(backup, outputStream);
      } finally {
        DatabaseBackupService.deleteQuietly(backup);
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(fileSize(backup))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(body);
  }

  @PostMapping("/admin/database/restore")
  public String restore(@RequestParam("backup") MultipartFile backup,
                        RedirectAttributes redirectAttributes) {
    if (backup.isEmpty()) {
      redirectAttributes.addFlashAttribute("restoreError", "empty");
      return "redirect:/admin/database";
    }
    if (backup.getSize() > maxBackupSize) {
      redirectAttributes.addFlashAttribute("restoreError", "tooLarge");
      return "redirect:/admin/database";
    }

    Path uploadedBackup = null;
    try {
      uploadedBackup = Files.createTempFile("retro-game-upload-", ".dump");
      backup.transferTo(uploadedBackup);
      databaseBackupService.restore(uploadedBackup);
      redirectAttributes.addFlashAttribute("restoreSuccess", true);
    } catch (DatabaseBackupException | IOException e) {
      logger.error("Database restore failed", e);
      redirectAttributes.addFlashAttribute("restoreError", "failed");
    } finally {
      DatabaseBackupService.deleteQuietly(uploadedBackup);
    }
    return "redirect:/admin/database";
  }

  private static long fileSize(Path path) {
    try {
      return Files.size(path);
    } catch (IOException e) {
      DatabaseBackupService.deleteQuietly(path);
      throw new DatabaseBackupException("Could not read the database backup", e);
    }
  }
}

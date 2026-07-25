package com.github.retro_game.retro_game.battleengineservice

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

class HistoryDatabase(
  databasePath: Path = defaultHistoryDatabasePath(),
) {
  private val jdbcUrl = "jdbc:sqlite:${databasePath.toAbsolutePath()}"

  init {
    databasePath.parent?.let(Files::createDirectories)
    connection().use { connection ->
      connection.createStatement().use { statement ->
        statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS history_items (
            id TEXT PRIMARY KEY,
            utc_timestamp INTEGER NOT NULL,
            query TEXT NOT NULL,
            seed INTEGER NOT NULL,
            total_attacker_losses INTEGER NOT NULL,
            total_defender_losses INTEGER NOT NULL,
            total_debris_field INTEGER NOT NULL,
            plunder INTEGER NOT NULL,
            elapsed_time INTEGER NOT NULL DEFAULT 0
          )
          """.trimIndent()
        )
        if (!connection.hasColumn("history_items", "elapsed_time")) {
          statement.executeUpdate("ALTER TABLE history_items ADD COLUMN elapsed_time INTEGER NOT NULL DEFAULT 0")
        }
        statement.executeUpdate(
          """
          CREATE INDEX IF NOT EXISTS history_items_utc_timestamp_idx
          ON history_items (utc_timestamp DESC)
          """.trimIndent()
        )
      }
    }
  }

  fun save(item: HistoryItem) {
    connection().use { connection ->
      connection.prepareStatement(
        """
        INSERT INTO history_items (
          id,
          utc_timestamp,
          query,
          seed,
          total_attacker_losses,
          total_defender_losses,
          total_debris_field,
          plunder,
          elapsed_time
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, item.id.toString())
        statement.setLong(2, item.utcTimestamp)
        statement.setString(3, item.query)
        statement.setLong(4, item.seed)
        statement.setLong(5, item.totalAttackerLosses)
        statement.setLong(6, item.totalDefenderLosses)
        statement.setLong(7, item.totalDebrisField)
        statement.setLong(8, item.plunder)
        statement.setLong(9, item.elapsedTime)
        statement.executeUpdate()
      }
    }
  }

  fun load(id: UUID): HistoryItem? =
    connection().use { connection ->
      connection.prepareStatement(
        """
        SELECT
          id,
          utc_timestamp,
          query,
          seed,
          total_attacker_losses,
          total_defender_losses,
          total_debris_field,
          plunder,
          elapsed_time
        FROM history_items
        WHERE id = ?
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, id.toString())
        statement.executeQuery().use { results ->
          if (results.next()) results.toHistoryItem() else null
        }
      }
    }

  fun list(limit: Int, offset: Int): List<HistoryItem> {
    val normalizedLimit = limit.coerceIn(1, MAX_PAGE_SIZE)
    val normalizedOffset = offset.coerceAtLeast(0)

    return connection().use { connection ->
      connection.prepareStatement(
        """
        SELECT
          id,
          utc_timestamp,
          query,
          seed,
          total_attacker_losses,
          total_defender_losses,
          total_debris_field,
          plunder,
          elapsed_time
        FROM history_items
        ORDER BY utc_timestamp DESC
        LIMIT ?
        OFFSET ?
        """.trimIndent()
      ).use { statement ->
        statement.setInt(1, normalizedLimit)
        statement.setInt(2, normalizedOffset)
        statement.executeQuery().use { results ->
          buildList {
            while (results.next()) {
              add(results.toHistoryItem())
            }
          }
        }
      }
    }
  }

  private fun connection(): Connection = DriverManager.getConnection(jdbcUrl)

  private fun ResultSet.toHistoryItem(): HistoryItem =
    HistoryItem(
      id = UUID.fromString(getString("id")),
      utcTimestamp = getLong("utc_timestamp"),
      query = getString("query"),
      seed = getLong("seed"),
      totalAttackerLosses = getLong("total_attacker_losses"),
      totalDefenderLosses = getLong("total_defender_losses"),
      totalDebrisField = getLong("total_debris_field"),
      plunder = getLong("plunder"),
      elapsedTime = getLong("elapsed_time"),
    )

  companion object {
    private const val MAX_PAGE_SIZE = 500
  }
}

private fun Connection.hasColumn(tableName: String, columnName: String): Boolean =
  createStatement().use { statement ->
    statement.executeQuery("PRAGMA table_info($tableName)").use { results ->
      while (results.next()) {
        if (results.getString("name") == columnName) {
          return true
        }
      }
      false
    }
  }

private fun defaultHistoryDatabasePath(): Path =
  Path.of(System.getenv("HISTORY_DATABASE_PATH") ?: "battle-sim-history.sqlite")

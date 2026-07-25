package com.github.retro_game.retro_game.battleengineservice

import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryDatabaseTest {

  @Test
  fun `saves and loads history item`() {
    val database = testDatabase()
    val item = historyItem(query = "seed=1", timestamp = 1000L)

    database.save(item)

    assertEquals(item, database.load(item.id))
  }

  @Test
  fun `returns null when history item does not exist`() {
    val database = testDatabase()

    assertNull(database.load(UUID.randomUUID()))
  }

  @Test
  fun `lists history items with paging ordered by newest first`() {
    val database = testDatabase()
    val oldest = historyItem(query = "seed=1", timestamp = 1000L)
    val middle = historyItem(query = "seed=2", timestamp = 2000L)
    val newest = historyItem(query = "seed=3", timestamp = 3000L)
    database.save(oldest)
    database.save(middle)
    database.save(newest)

    assertEquals(listOf(newest, middle), database.list(limit = 2, offset = 0))
    assertEquals(listOf(oldest), database.list(limit = 2, offset = 2))
  }

  private fun testDatabase(): HistoryDatabase {
    val directory = Files.createTempDirectory("battle-history-test")
    return HistoryDatabase(directory.resolve("history.sqlite"))
  }

  private fun historyItem(query: String, timestamp: Long): HistoryItem =
    HistoryItem(
      id = UUID.randomUUID(),
      utcTimestamp = timestamp,
      query = query,
      seed = 1L,
      totalAttackerLosses = 2L,
      totalDefenderLosses = 3L,
      totalDebrisField = 4L,
      plunder = 5L,
      elapsedTime = 6L,
    )
}

package com.github.retro_game.retro_game.battleengineservice

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistoryCsvTest {

  @Test
  fun `encodes and decodes history items`() {
    val item = HistoryItem(
      id = UUID.randomUUID(),
      utcTimestamp = 1000L,
      query = "enemy_pos=1:1:1&note=a,\"b\"",
      seed = 2L,
      totalAttackerLosses = 3L,
      totalDefenderLosses = 4L,
      totalDebrisField = 5L,
      plunder = 6L,
      elapsedTime = 7L,
      engine = "native",
    )

    assertEquals(listOf(item), HistoryCsv.decode(HistoryCsv.encode(listOf(item))))
  }

  @Test
  fun `rejects csv with invalid header`() {
    assertFailsWith<IllegalArgumentException> {
      HistoryCsv.decode("id,query\n1,x\n")
    }
  }
}

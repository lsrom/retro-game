package com.github.retro_game.retro_game.battleengineservice

import java.util.UUID

internal object HistoryCsv {
  private val header = listOf(
    "id",
    "utc_timestamp",
    "query",
    "seed",
    "total_attacker_losses",
    "total_defender_losses",
    "total_debris_field",
    "plunder",
    "elapsed_time",
  )

  fun encode(items: List<HistoryItem>): String =
    buildString {
      appendLine(header.joinToString(","))
      for (item in items) {
        appendLine(
          listOf(
            item.id.toString(),
            item.utcTimestamp.toString(),
            item.query,
            item.seed.toString(),
            item.totalAttackerLosses.toString(),
            item.totalDefenderLosses.toString(),
            item.totalDebrisField.toString(),
            item.plunder.toString(),
            item.elapsedTime.toString(),
          ).joinToString(",") { it.toCsvCell() }
        )
      }
    }

  fun decode(csv: String): List<HistoryItem> {
    val records = parseRecords(csv).filterNot { record -> record.size == 1 && record[0].isEmpty() }
    require(records.isNotEmpty()) { "CSV file is empty." }
    require(records.first() == header) { "CSV header does not match expected history export format." }

    return records.drop(1).mapIndexed { index, record ->
      val lineNumber = index + 2
      require(record.size == header.size) { "CSV line $lineNumber has ${record.size} fields; expected ${header.size}." }
      HistoryItem(
        id = parseUuid(record[0], lineNumber, "id"),
        utcTimestamp = parseLong(record[1], lineNumber, "utc_timestamp"),
        query = record[2],
        seed = parseLong(record[3], lineNumber, "seed"),
        totalAttackerLosses = parseLong(record[4], lineNumber, "total_attacker_losses"),
        totalDefenderLosses = parseLong(record[5], lineNumber, "total_defender_losses"),
        totalDebrisField = parseLong(record[6], lineNumber, "total_debris_field"),
        plunder = parseLong(record[7], lineNumber, "plunder"),
        elapsedTime = parseLong(record[8], lineNumber, "elapsed_time"),
      )
    }
  }

  private fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
  }

  private fun parseRecords(csv: String): List<List<String>> {
    val records = mutableListOf<List<String>>()
    val record = mutableListOf<String>()
    val cell = StringBuilder()
    var index = 0
    var quoted = false

    while (index < csv.length) {
      val char = csv[index]
      when {
        quoted && char == '"' && csv.getOrNull(index + 1) == '"' -> {
          cell.append('"')
          index += 1
        }

        char == '"' -> quoted = !quoted
        !quoted && char == ',' -> {
          record.add(cell.toString())
          cell.clear()
        }

        !quoted && (char == '\n' || char == '\r') -> {
          record.add(cell.toString())
          cell.clear()
          records.add(record.toList())
          record.clear()
          if (char == '\r' && csv.getOrNull(index + 1) == '\n') {
            index += 1
          }
        }

        else -> cell.append(char)
      }
      index += 1
    }

    require(!quoted) { "CSV has an unterminated quoted field." }
    if (cell.isNotEmpty() || record.isNotEmpty()) {
      record.add(cell.toString())
      records.add(record.toList())
    }
    return records
  }

  private fun parseUuid(value: String, lineNumber: Int, column: String): UUID =
    try {
      UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
      throw IllegalArgumentException("CSV line $lineNumber has invalid $column.")
    }

  private fun parseLong(value: String, lineNumber: Int, column: String): Long =
    value.toLongOrNull() ?: throw IllegalArgumentException("CSV line $lineNumber has invalid $column.")
}

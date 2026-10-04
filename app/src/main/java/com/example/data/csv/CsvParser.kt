package com.example.data.csv

import com.example.data.model.BetObservation
import com.example.data.model.FootballMatch
import com.example.data.model.Outcome
import com.example.data.model.ResearchStrategyType
import java.util.UUID

data class CsvValidationError(
    val rowNumber: Int,
    val rawLine: String,
    val reason: String
)

data class CsvImportResult(
    val matches: List<FootballMatch>,
    val observations: List<BetObservation>,
    val errors: List<CsvValidationError>,
    val totalRowsProcessed: Int,
    val validRowsCount: Int
)

object CsvParser {

    fun parse(csvText: String): CsvImportResult {
        val lines = csvText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return CsvImportResult(emptyList(), emptyList(), emptyList(), 0, 0)
        }

        // Header parsing
        val headerTokens = parseCsvRow(lines.first()).map { it.lowercase().trim() }
        val colMap = headerTokens.withIndex().associate { it.value to it.index }

        val matchesMap = LinkedHashMap<String, FootballMatch>()
        val observations = ArrayList<BetObservation>()
        val errors = ArrayList<CsvValidationError>()

        var validRows = 0

        for (rowIdx in 1 until lines.size) {
            val rawLine = lines[rowIdx]
            val tokens = parseCsvRow(rawLine)
            val rowNum = rowIdx + 1

            try {
                fun get(vararg names: String): String? {
                    for (n in names) {
                        val col = colMap[n]
                        if (col != null && col < tokens.size) {
                            val v = tokens[col].trim()
                            if (v.isNotEmpty()) return v
                        }
                    }
                    return null
                }

                val date = get("date", "match_date")
                val homeTeam = get("home_team", "home", "hometeam")
                val awayTeam = get("away_team", "away", "awayteam")
                val comp = get("competition", "tournament", "league") ?: "Tournament"
                val stage = get("stage", "round", "phase") ?: "Knockout"

                if (date.isNullOrEmpty() || homeTeam.isNullOrEmpty() || awayTeam.isNullOrEmpty()) {
                    errors.add(CsvValidationError(rowNum, rawLine, "Missing required match fields: date, home_team, or away_team."))
                    continue
                }

                val matchId = get("match_id", "id") ?: "${date}_${homeTeam}_vs_${awayTeam}".replace(" ", "_")

                val homeGoalsStr = get("home_goals", "fthg", "hg")
                val awayGoalsStr = get("away_goals", "ftag", "ag")

                val homeGoals = homeGoalsStr?.toIntOrNull()
                val awayGoals = awayGoalsStr?.toIntOrNull()

                if (homeGoals == null || awayGoals == null) {
                    errors.add(CsvValidationError(rowNum, rawLine, "Invalid or missing home/away final score values."))
                    continue
                }

                val totalGoalsStr = get("total_goals", "tg", "goals")
                val totalGoals = totalGoalsStr?.toIntOrNull() ?: (homeGoals + awayGoals)

                val htHome = get("ht_home_goals", "hthg")?.toIntOrNull()
                val htAway = get("ht_away_goals", "htag")?.toIntOrNull()

                val bookmaker = get("bookmaker", "bk") ?: "Closing Market"
                val country = get("country", "nation") ?: "Global"
                val season = get("season", "year") ?: "2023-2024"
                val source = get("source") ?: "CSV Import"

                val match = FootballMatch(
                    id = matchId,
                    date = date,
                    competition = comp,
                    stage = stage,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    homeGoals = homeGoals,
                    awayGoals = awayGoals,
                    totalGoals = totalGoals,
                    halfTimeHomeGoals = htHome,
                    halfTimeAwayGoals = htAway,
                    bookmaker = bookmaker,
                    country = country,
                    season = season,
                    source = source
                )
                matchesMap.putIfAbsent(matchId, match)

                // Observation columns
                val market = get("market", "bet_market") ?: "Total Goals"
                val selection = get("selection", "bet_selection") ?: "Under 3.5"
                val oddsStr = get("odds", "price", "decimal_odds")

                if (oddsStr.isNullOrEmpty()) {
                    errors.add(CsvValidationError(rowNum, rawLine, "Missing betting odds. Odds cannot be invented."))
                    continue
                }

                val odds = oddsStr.toDoubleOrNull()
                if (odds == null || odds <= 1.0) {
                    errors.add(CsvValidationError(rowNum, rawLine, "Invalid decimal odds '$oddsStr'. Must be > 1.0."))
                    continue
                }

                val rawResult = get("result", "outcome", "status")?.lowercase()
                val outcome: Outcome = when {
                    rawResult == "win" || rawResult == "w" || rawResult == "1" -> Outcome.WIN
                    rawResult == "loss" || rawResult == "lose" || rawResult == "l" || rawResult == "0" -> Outcome.LOSS
                    rawResult == "void" || rawResult == "push" || rawResult == "v" -> Outcome.VOID
                    else -> {
                        // Attempt strict derivation from score & selection
                        deriveOutcome(market, selection, homeGoals, awayGoals, htHome, htAway)
                            ?: run {
                                errors.add(CsvValidationError(rowNum, rawLine, "Cannot determine result for market '$market' selection '$selection' without explicit result or sufficient score data."))
                                null
                            } ?: continue
                    }
                }

                val strategyType = ResearchStrategyType.fromMarketAndSelection(market, selection)
                    ?: when {
                        selection.contains("under 3.5", ignoreCase = true) -> ResearchStrategyType.UNDER_3_5
                        selection.contains("over 3.5", ignoreCase = true) -> ResearchStrategyType.OVER_3_5
                        selection.contains("under 1.5", ignoreCase = true) -> ResearchStrategyType.FIRST_HALF_UNDER_1_5
                        selection.contains("btts", ignoreCase = true) || selection.contains("yes", ignoreCase = true) -> ResearchStrategyType.BTTS_YES
                        selection.contains("draw", ignoreCase = true) -> ResearchStrategyType.DRAW_1X2
                        else -> ResearchStrategyType.FAVOURITE_1X2
                    }

                val stake = get("stake", "amount")?.toDoubleOrNull() ?: 100.0

                val obs = BetObservation(
                    id = UUID.randomUUID().toString(),
                    matchId = matchId,
                    date = date,
                    strategyType = strategyType,
                    market = market,
                    selection = selection,
                    odds = odds,
                    outcome = outcome,
                    stake = stake
                )

                observations.add(obs)
                validRows++
            } catch (e: Exception) {
                errors.add(CsvValidationError(rowNum, rawLine, "Parsing exception: ${e.message}"))
            }
        }

        return CsvImportResult(
            matches = matchesMap.values.toList(),
            observations = observations,
            errors = errors,
            totalRowsProcessed = lines.size - 1,
            validRowsCount = validRows
        )
    }

    private fun deriveOutcome(
        market: String,
        selection: String,
        homeGoals: Int,
        awayGoals: Int,
        htHome: Int?,
        htAway: Int?
    ): Outcome? {
        val sel = selection.trim().lowercase()
        val total = homeGoals + awayGoals

        return when {
            sel.contains("under 3.5") || sel == "u3.5" -> if (total < 4) Outcome.WIN else Outcome.LOSS
            sel.contains("over 3.5") || sel == "o3.5" -> if (total >= 4) Outcome.WIN else Outcome.LOSS
            sel.contains("btts") || sel == "yes" -> if (homeGoals > 0 && awayGoals > 0) Outcome.WIN else Outcome.LOSS
            sel.contains("draw") || sel == "x" -> if (homeGoals == awayGoals) Outcome.WIN else Outcome.LOSS
            sel.contains("1h under 1.5") || sel.contains("under 1.5") -> {
                if (htHome != null && htAway != null) {
                    if (htHome + htAway < 2) Outcome.WIN else Outcome.LOSS
                } else null
            }
            else -> null
        }
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}

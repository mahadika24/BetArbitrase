package com.example.data.model

import java.util.UUID

enum class Outcome {
    WIN,
    LOSS,
    VOID,
    PENDING
}

enum class MatchStage(val displayName: String) {
    KNOCKOUT("Knockout"),
    GROUP("Group Stage"),
    LEAGUE("League"),
    QUALIFIER("Qualifier"),
    FRIENDLY("Friendly"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): MatchStage {
            val normalized = value.trim().lowercase()
            return when {
                normalized.contains("knockout") || normalized.contains("final") ||
                        normalized.contains("round of 16") || normalized.contains("quarter") ||
                        normalized.contains("semi") || normalized.contains("playoff") -> KNOCKOUT
                normalized.contains("group") -> GROUP
                normalized.contains("league") -> LEAGUE
                normalized.contains("qualif") -> QUALIFIER
                normalized.contains("friend") -> FRIENDLY
                else -> OTHER
            }
        }
    }
}

enum class ResearchStrategyType(
    val id: String,
    val title: String,
    val market: String,
    val selection: String,
    val description: String
) {
    UNDER_3_5(
        id = "under_3_5",
        title = "Under 3.5 Goals",
        market = "Total Goals",
        selection = "Under 3.5",
        description = "Hypothesis target: Backing knockout matches to yield 3 or fewer total goals (0-3)."
    ),
    FIRST_HALF_UNDER_1_5(
        id = "1h_under_1_5",
        title = "1H Under 1.5",
        market = "1H Total Goals",
        selection = "Under 1.5",
        description = "Backing conservative first-half play with 0 or 1 goal scored in the opening 45 mins."
    ),
    BTTS_YES(
        id = "btts_yes",
        title = "BTTS Yes",
        market = "Both Teams To Score",
        selection = "Yes",
        description = "Both teams score at least one goal in regular time."
    ),
    FAVOURITE_1X2(
        id = "favourite_1x2",
        title = "Favourite 1X2",
        market = "Match Result (1X2)",
        selection = "Favourite",
        description = "Backing the pre-match bookmaker favourite (lowest decimal odds) to win in 90 minutes."
    ),
    DRAW_1X2(
        id = "draw_1x2",
        title = "Draw 1X2",
        market = "Match Result (1X2)",
        selection = "Draw",
        description = "Backing regular time to end in parity before extra time or penalties."
    ),
    OVER_3_5(
        id = "over_3_5",
        title = "Over 3.5 Goals",
        market = "Total Goals",
        selection = "Over 3.5",
        description = "Counter-hypothesis: Backing high-variance explosive games with 4 or more goals."
    );

    companion object {
        fun fromMarketAndSelection(market: String, selection: String): ResearchStrategyType? {
            val m = market.trim().lowercase()
            val s = selection.trim().lowercase()
            return when {
                (m.contains("total") || m.contains("over/under") || m.contains("goals")) && s.contains("under 3.5") -> UNDER_3_5
                (m.contains("total") || m.contains("over/under") || m.contains("goals")) && s.contains("over 3.5") -> OVER_3_5
                (m.contains("1h") || m.contains("half") || m.contains("first half")) && (s.contains("under 1.5") || s.contains("u1.5")) -> FIRST_HALF_UNDER_1_5
                (m.contains("btts") || m.contains("both teams") || m.contains("gg")) && (s.contains("yes") || s == "y") -> BTTS_YES
                (m.contains("1x2") || m.contains("result") || m.contains("match")) && (s.contains("fav") || s.contains("home_fav") || s.contains("away_fav")) -> FAVOURITE_1X2
                (m.contains("1x2") || m.contains("result") || m.contains("match")) && (s.contains("draw") || s == "x" || s == "d") -> DRAW_1X2
                else -> null
            }
        }
    }
}

data class FootballMatch(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val competition: String,
    val stage: String, // e.g. "Knockout", "Group"
    val homeTeam: String,
    val awayTeam: String,
    val homeGoals: Int,
    val awayGoals: Int,
    val totalGoals: Int = homeGoals + awayGoals,
    val halfTimeHomeGoals: Int? = null,
    val halfTimeAwayGoals: Int? = null,
    val bookmaker: String = "Pinnacle/Closing",
    val country: String = "Europe",
    val season: String = "2023/24",
    val round: String = "",
    val source: String = "Historical Database"
) {
    val isKnockout: Boolean
        get() = MatchStage.fromString(stage) == MatchStage.KNOCKOUT
}

data class BetObservation(
    val id: String = UUID.randomUUID().toString(),
    val matchId: String,
    val date: String, // chronological alignment
    val strategyType: ResearchStrategyType,
    val market: String,
    val selection: String,
    val odds: Double,
    val outcome: Outcome,
    val stake: Double = 100.0,
    val profit: Double = calculateDefaultProfit(stake, odds, outcome)
) {
    companion object {
        fun calculateDefaultProfit(stake: Double, odds: Double, outcome: Outcome): Double {
            return when (outcome) {
                Outcome.WIN -> stake * (odds - 1.0)
                Outcome.LOSS -> -stake
                Outcome.VOID -> 0.0
                Outcome.PENDING -> 0.0
            }
        }
    }
}

enum class EvidenceClassification(
    val emoji: String,
    val label: String,
    val summary: String
) {
    ROBUST(
        "🟢",
        "Robust / Promising",
        "Edge survives rigorous testing: positive ROI, bootstrap 95% CI lower bound > 0%, resistant to outlier removal, and positive Out-of-Sample return."
    ),
    POSITIVE_INSUFFICIENT(
        "🟡",
        "Positive but Insufficient Evidence",
        "Nominally positive historical ROI, but confidence interval crosses 0%, sample is small, or edge collapses when top outliers are removed."
    ),
    WEAK_UNSTABLE(
        "🔴",
        "Weak / Likely Unstable",
        "Negative overall ROI, high drawdown, or extreme variance. Cannot reject the null hypothesis of no statistical edge."
    ),
    INSUFFICIENT_DATA(
        "⚪",
        "Insufficient Data",
        "Sample size N is too small (N < 30 observations) for reliable inference."
    )
}

data class VerificationRule(
    val title: String,
    val passed: Boolean,
    val valueDisplay: String,
    val requirement: String,
    val explanation: String
)

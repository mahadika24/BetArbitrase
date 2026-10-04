package com.example.data.csv

import com.example.data.model.BetObservation
import com.example.data.model.FootballMatch
import com.example.stats.StrategyEvaluationReport

object CsvExporter {

    fun exportObservationsToCsv(matches: List<FootballMatch>, observations: List<BetObservation>): String {
        val matchMap = matches.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("match_id,date,competition,stage,home_team,away_team,home_goals,away_goals,total_goals,market,selection,odds,result,stake,profit\n")

        for (obs in observations) {
            val m = matchMap[obs.matchId]
            val date = m?.date ?: obs.date
            val comp = m?.competition ?: "Unknown"
            val stage = m?.stage ?: "Knockout"
            val home = m?.homeTeam ?: "Home"
            val away = m?.awayTeam ?: "Away"
            val hg = m?.homeGoals ?: 0
            val ag = m?.awayGoals ?: 0
            val tg = m?.totalGoals ?: (hg + ag)
            val resultStr = obs.outcome.name

            sb.append("\"${obs.matchId}\",\"$date\",\"$comp\",\"$stage\",\"$home\",\"$away\",$hg,$ag,$tg,\"${obs.market}\",\"${obs.selection}\",${obs.odds},\"$resultStr\",${obs.stake},${String.format("%.2f", obs.profit)}\n")
        }
        return sb.toString()
    }

    fun exportStrategyComparisonCsv(reports: List<StrategyEvaluationReport>): String {
        val sb = StringBuilder()
        sb.append("Strategy,N_Observations,Matches,Wins,Losses,Win_Rate_Pct,Wilson_95_CI,Avg_Odds,Median_Odds,Break_Even_Pct,Total_Stake,Profit,ROI_Pct,Bootstrap_95_CI,Max_Drawdown_Pct,Max_Losing_Streak,Classification\n")

        for (r in reports) {
            val winCi = "[${String.format("%.1f", r.wilsonWinRateCi.lowerBound)}% - ${String.format("%.1f", r.wilsonWinRateCi.upperBound)}%]"
            val bootCi = "[${String.format("%.1f", r.bootstrapResult.lowerCi95)}% - ${String.format("%.1f", r.bootstrapResult.upperCi95)}%]"
            sb.append("\"${r.strategyType.title}\",${r.sampleSize},${r.matchesCount},${r.wins},${r.losses},${String.format("%.2f", r.winRate)},\"$winCi\",${String.format("%.2f", r.averageOdds)},${String.format("%.2f", r.medianOdds)},${String.format("%.2f", r.portfolioBreakEvenRate)},${String.format("%.2f", r.totalStake)},${String.format("%.2f", r.totalProfit)},${String.format("%.2f", r.roi)},\"$bootCi\",${String.format("%.2f", r.drawdownAnalysis.maxDrawdownPercent)},${r.drawdownAnalysis.longestLosingStreak},\"${r.classification.label}\"\n")
        }
        return sb.toString()
    }
}

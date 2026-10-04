package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceClassification
import com.example.data.model.ResearchStrategyType
import com.example.data.model.VerificationRule
import com.example.stats.StrategyEvaluationReport
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.QuantEmerald
import com.example.ui.theme.QuantRose
import com.example.ui.viewmodel.ResearchUiState

@Composable
fun ResearchReportScreen(
    state: ResearchUiState,
    onExportReport: () -> Unit
) {
    val selectedStrategy = state.selectedStrategy
    val report = state.strategyReports[selectedStrategy]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("research_report_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report Header
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QuantCyan.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = QuantCyan,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EMPIRICAL RESEARCH EVIDENCE REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Formal scientific evaluation of strategy: ${selectedStrategy.title}. Uses multi-factor criteria to distinguish genuine historical inefficiencies from statistical variance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (report == null || report.sampleSize == 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Text("No data available to formulate research report.")
                    }
                }
            }
            return@LazyColumn
        }

        // Primary Classification Badge
        item {
            ClassificationVerdictCard(classification = report.classification, strategyTitle = selectedStrategy.title, roi = report.roi)
        }

        // Transparent Methodology Rule Checklist
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Statistical Criteria Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Explicit validation gates required to certify edge robustness",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    report.verificationRules.forEach { rule ->
                        RuleItemRow(rule = rule)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Scientific Research Conclusions & Commentary
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hypothesis Verdict & Quant Commentary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val commentary = when (report.classification) {
                        EvidenceClassification.ROBUST ->
                            "Hypothesis Assessment: Strong positive evidence. Under 3.5 goals shows persistent positive returns across multiple tournament cycles. The 95% bootstrap confidence interval remains strictly above zero, indicating that observed profitability cannot easily be dismissed as random luck. Furthermore, the edge is preserved when extreme outlier matches are stripped and when tested out-of-sample."
                        EvidenceClassification.POSITIVE_INSUFFICIENT ->
                            "Hypothesis Assessment: Ambiguous edge. While the historical ROI is nominally positive (+${String.format("%.2f", report.roi)}%), the 95% bootstrap confidence interval crosses zero ([${String.format("%.1f", report.bootstrapResult.lowerCi95)}%, ${String.format("%.1f", report.bootstrapResult.upperCi95)}%]), or the edge degrades under outlier stress testing. Consequently, we cannot reject the null hypothesis that this positive performance is a product of variance."
                        EvidenceClassification.WEAK_UNSTABLE ->
                            "Hypothesis Assessment: Negative edge / high drawdown. Historical performance exhibits negative ROI (${String.format("%.2f", report.roi)}%) or severe drawdown exposure (-${String.format("%.1f", report.drawdownAnalysis.maxDrawdownPercent)}%). The market prices this selection efficiently, with bookmaker vig outweighing returns."
                        EvidenceClassification.INSUFFICIENT_DATA ->
                            "Hypothesis Assessment: Inconclusive. The sample size (N=${report.sampleSize}) is too small to draw statistically sound conclusions. More observations are required."
                    }

                    Text(
                        text = commentary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ClassificationVerdictCard(
    classification: EvidenceClassification,
    strategyTitle: String,
    roi: Double
) {
    val bgColor = when (classification) {
        EvidenceClassification.ROBUST -> QuantEmerald.copy(alpha = 0.15f)
        EvidenceClassification.POSITIVE_INSUFFICIENT -> QuantAmber.copy(alpha = 0.15f)
        EvidenceClassification.WEAK_UNSTABLE -> QuantRose.copy(alpha = 0.15f)
        EvidenceClassification.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (classification) {
        EvidenceClassification.ROBUST -> QuantEmerald
        EvidenceClassification.POSITIVE_INSUFFICIENT -> QuantAmber
        EvidenceClassification.WEAK_UNSTABLE -> QuantRose
        EvidenceClassification.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = classification.emoji,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "VERDICT: ${classification.label.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "$strategyTitle (Historical ROI: ${if (roi >= 0) "+" else ""}${String.format("%.2f", roi)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = classification.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RuleItemRow(rule: VerificationRule) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (rule.passed) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (rule.passed) QuantEmerald else QuantRose,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = rule.valueDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (rule.passed) QuantEmerald else QuantRose
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Rule: ${rule.requirement} — ${rule.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

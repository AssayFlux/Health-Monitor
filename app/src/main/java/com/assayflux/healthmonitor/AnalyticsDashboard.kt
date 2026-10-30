package com.assayflux.healthmonitor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private data class TrendPoint(val label: String, val value: Float)

/**
 * Scientific overview based on all stored observations (not the recent-30 UI lists).
 * Each metric keeps its native unit; missing values are excluded rather than imputed.
 * The charts show descriptive trends only and are not diagnostic thresholds.
 */
@Composable
fun AnalyticsDashboard(db: HealthDb, refreshToken: Int) {
    val series = remember(refreshToken) {
        val bp = db.rows("""
            SELECT COALESCE(measurement_date,date(recorded_at)) AS d,
              AVG(CASE
                WHEN left_systolic IS NOT NULL AND right_systolic IS NOT NULL THEN (left_systolic+right_systolic)/2.0
                WHEN left_systolic IS NOT NULL THEN left_systolic
                WHEN right_systolic IS NOT NULL THEN right_systolic
                WHEN systolic IS NOT NULL THEN systolic ELSE NULL END)
            FROM vitals
            GROUP BY d ORDER BY d
        """.trimIndent()).mapNotNull { row -> row.getOrNull(1)?.toFloatOrNull()?.let { TrendPoint(row[0], it) } }
        val pulse = db.rows("""
            SELECT COALESCE(measurement_date,date(recorded_at)), AVG(pulse)
            FROM vitals WHERE pulse IS NOT NULL
            GROUP BY 1 ORDER BY 1
        """.trimIndent()).mapNotNull { row -> row.getOrNull(1)?.toFloatOrNull()?.let { TrendPoint(row[0], it) } }
        val weight = db.rows("""
            SELECT COALESCE(measurement_date,date(recorded_at)), AVG(weight)
            FROM vitals WHERE weight IS NOT NULL
            GROUP BY 1 ORDER BY 1
        """.trimIndent()).mapNotNull { row -> row.getOrNull(1)?.toFloatOrNull()?.let { TrendPoint(row[0], it) } }
        val oxygen = db.rows("""
            SELECT COALESCE(measurement_date,date(recorded_at)), AVG(spo2)
            FROM vitals WHERE spo2 IS NOT NULL
            GROUP BY 1 ORDER BY 1
        """.trimIndent()).mapNotNull { row -> row.getOrNull(1)?.toFloatOrNull()?.let { TrendPoint(row[0], it) } }
        val temperature = db.rows("""
            SELECT COALESCE(measurement_date,date(recorded_at)), AVG(temp)
            FROM vitals WHERE temp IS NOT NULL
            GROUP BY 1 ORDER BY 1
        """.trimIndent()).mapNotNull { row -> row.getOrNull(1)?.toFloatOrNull()?.let { TrendPoint(row[0], it) } }
        val tests = db.rows("SELECT COALESCE(status,'Unclassified'), COUNT(*) FROM clinical_tests GROUP BY status ORDER BY status")
        val labCount = db.count("labs")
        val anthropometryCount = db.count("anthropometry")
        val medicineCount = db.count("medicines")
        val clinicalCount = db.count("clinical_tests")
        val memberCount = db.count("members")
        val vitalCount = db.count("vitals")
        AnalyticsSnapshot(bp,pulse,weight,oxygen,temperature,tests,labCount,anthropometryCount,medicineCount,clinicalCount,memberCount,vitalCount)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SCIENTIFIC OVERVIEW", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Your health data, in context", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("Descriptive summaries from all saved records. Trends are grouped by measurement date; missing values are not estimated.", color = Ink.copy(alpha=.7f), fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            AnalyticsMetric("People",series.members.toString(),Modifier.weight(1f))
            AnalyticsMetric("Vitals",series.vitals.toString(),Modifier.weight(1f))
            AnalyticsMetric("Labs",series.labs.toString(),Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            AnalyticsMetric("Anthropometry",series.anthropometry.toString(),Modifier.weight(1f))
            AnalyticsMetric("Clinical tests",series.clinical.toString(),Modifier.weight(1f))
            AnalyticsMetric("Medicines",series.medicines.toString(),Modifier.weight(1f))
        }
        ScientificLineChart("Systolic blood pressure", "mmHg · average of available arm readings per date", series.bp)
        ScientificLineChart("Pulse", "beats/min · daily mean", series.pulse)
        ScientificLineChart("Body weight", "kg · daily mean", series.weight)
        ScientificLineChart("Oxygen saturation", "% · daily mean", series.oxygen)
        ScientificLineChart("Body temperature", "°C · daily mean", series.temperature)
        Surface(color=Cream, shape=RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Clinical test classification", color=Ink, fontWeight=FontWeight.SemiBold)
                if (series.testStatuses.isEmpty()) Text("No structured clinical test results recorded yet.", color=Ink.copy(alpha=.65f), fontSize=13.sp)
                series.testStatuses.forEach { Text("${it[0]} · ${it[1]}", color=Ink, fontSize=13.sp) }
            }
        }
        Text("Interpretation: daily averages can hide variation between readings and between arms. Review the original observations, measurement conditions, units, laboratory reference interval, and clinician advice. This dashboard does not diagnose disease.", color=Ink.copy(alpha=.7f), fontSize=12.sp)
    }
}

private data class AnalyticsSnapshot(
    val bp: List<TrendPoint>, val pulse: List<TrendPoint>, val weight: List<TrendPoint>,
    val oxygen: List<TrendPoint>, val temperature: List<TrendPoint>,
    val testStatuses: List<List<String>>, val labs: Int, val anthropometry: Int,
    val medicines: Int, val clinical: Int, val members: Int, val vitals: Int
)

@Composable
private fun AnalyticsMetric(label:String,value:String,modifier:Modifier=Modifier) {
    Surface(modifier, color=Cream, shape=RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color=Forest, fontSize=21.sp, fontWeight=FontWeight.SemiBold)
            Text(label, color=Ink.copy(alpha=.65f), fontSize=11.sp)
        }
    }
}

@Composable
private fun ScientificLineChart(title:String, unit:String, points:List<TrendPoint>) {
    Surface(color=Cream, shape=RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(title, color=Ink, fontWeight=FontWeight.SemiBold)
            Text(unit, color=Ink.copy(alpha=.6f), fontSize=11.sp)
            if (points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment=androidx.compose.ui.Alignment.Center) {
                    Text("Not enough recorded data to plot.", color=Ink.copy(alpha=.55f), fontSize=12.sp)
                }
            } else {
                val values = points.map { it.value }
                val low = values.minOrNull() ?: 0f
                val high = values.maxOrNull() ?: 1f
                val span = max(high-low, max(kotlin.math.abs(high)*.05f, .1f))
                val axisMin = low - span*.12f
                val axisMax = high + span*.12f
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val left=42f; val right=size.width-8f; val top=8f; val bottom=size.height-24f
                    for (i in 0..4) {
                        val y=top+(bottom-top)*i/4f
                        drawLine(Forest.copy(alpha=.12f),Offset(left,y),Offset(right,y),1f)
                    }
                    val path=Path()
                    points.forEachIndexed { index, point ->
                        val x=if(points.size==1) (left+right)/2f else left+(right-left)*index/(points.size-1)
                        val y=bottom-(point.value-axisMin)/(axisMax-axisMin)*(bottom-top)
                        if(index==0) path.moveTo(x,y) else path.lineTo(x,y)
                        drawCircle(Clay,radius=3.5f,center=Offset(x,y))
                    }
                    drawPath(path,Forest,style=Stroke(width=2.5f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(points.first().label, color=Ink.copy(alpha=.55f), fontSize=9.sp)
                    Text("n = ${points.size} dates", color=Ink.copy(alpha=.55f), fontSize=9.sp)
                    Text(points.last().label, color=Ink.copy(alpha=.55f), fontSize=9.sp)
                }
                val mean=values.average()
                val sd=if(values.size>1) kotlin.math.sqrt(values.sumOf { (it-mean).toDouble()*(it-mean).toDouble() }/(values.size-1)).toFloat() else 0f
                Text("Mean ${"%.1f".format(java.util.Locale.US,mean)} · SD ${"%.1f".format(java.util.Locale.US,sd)} · Range ${"%.1f".format(java.util.Locale.US,low)}–${"%.1f".format(java.util.Locale.US,high)}", color=Ink.copy(alpha=.75f), fontSize=11.sp)
            }
        }
    }
}

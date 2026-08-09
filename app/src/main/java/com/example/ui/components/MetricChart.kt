package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.AccentViolet

@Composable
fun MetricBarChart(
    teacherCount: Int,
    studentCount: Int,
    classCount: Int,
    submissionCount: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(teacherCount, studentCount, classCount, submissionCount, 1).toFloat()
    val barColor = PrimaryBlue
    val secondaryColor = SecondaryTeal
    val accentColor = AccentViolet
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(modifier = modifier) {
        Text(
            text = "System Overview Distribution",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / 9f

            // Draw horizontal gridlines
            for (i in 0..4) {
                val y = height - (height * (i / 4f))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            val data = listOf(
                Triple("Teachers", teacherCount, barColor),
                Triple("Students", studentCount, secondaryColor),
                Triple("Classes", classCount, accentColor),
                Triple("Submissions", submissionCount, PrimaryBlue)
            )

            data.forEachIndexed { index, item ->
                val ratio = item.second / maxVal
                val barHeight = (height * 0.8f) * ratio
                val x = barWidth + index * (barWidth * 2f)
                val y = height - barHeight

                // Draw Bar with Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(item.third, item.third.copy(alpha = 0.4f))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Top highlight line
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(x, y),
                    end = Offset(x + barWidth, y),
                    strokeWidth = 3f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ChartLegendItem("Teachers", teacherCount, barColor)
            ChartLegendItem("Students", studentCount, secondaryColor)
            ChartLegendItem("Classes", classCount, accentColor)
            ChartLegendItem("Submissions", submissionCount, PrimaryBlue)
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

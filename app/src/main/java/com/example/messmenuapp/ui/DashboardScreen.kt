package com.example.messmenuapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class MenuItemUI(
    val day: String,
    val breakfast: String,
    val lunch: String,
    val snacks: String,
    val dinner: String,
    val date: LocalDate = LocalDate.now()
)

private val Ink = Color(0xFF202A24)
private val Muted = Color(0xFF727C75)
private val Green = Color(0xFF236B4E)
private val Canvas = Color(0xFFF5F7F2)

@Composable
fun DashboardScreen(state: DashboardState, onImportClick: () -> Unit, onSelectDate: (LocalDate) -> Unit, onChangeMonth: (Long) -> Unit) {
    val menu = state.menu.firstOrNull { it.date == state.selectedDate }
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("MESS MENU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green, letterSpacing = 2.sp)
                Text("Plan your meals.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
            }

            Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(if (state.menu.isEmpty()) "Import spreadsheet" else "Update spreadsheet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Green)
            if (state.menu.isNotEmpty()) CalendarCard(state, onSelectDate, onChangeMonth)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF28694D), Color(0xFF5A9B6D))))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("SELECTED DAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .82f), letterSpacing = 1.5.sp)
                Text(menu?.day ?: state.selectedDate.toString(), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    when {
                        state.isLoading -> "Reading your spreadsheet"
                        menu != null -> "Four meals planned for your day"
                        state.menu.isNotEmpty() -> "No menu found for this date"
                        else -> "Import your mess schedule to get started"
                    },
                    fontSize = 14.sp, color = Color.White.copy(alpha = .88f)
                )
            }

            if (!state.isLoading && state.menu.isEmpty()) {
                InfoCard(
                    title = if (state.message == null) "Your menu starts here" else "We couldn't find today's menu",
                    body = state.message ?: "Select the .xlsx schedule shared by your mess. Your menu will be saved on this device."
                )
            } else {
                if (menu == null) InfoCard("No menu for this day", "Choose a highlighted date to see its meals.") else {
                Text("MEALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.4.sp)
                MealCard("Breakfast", menu.breakfast, "☀")
                MealCard("Lunch", menu.lunch, "◉")
                MealCard("Snacks", menu.snacks, "✦")
                MealCard("Dinner", menu.dinner, "☾")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalendarCard(state: DashboardState, onSelectDate: (LocalDate) -> Unit, onChangeMonth: (Long) -> Unit) {
    val ym = YearMonth.from(state.month)
    val offset = (ym.atDay(1).dayOfWeek.value + 6) % 7
    val dates: List<LocalDate?> = List(offset) { null } + (1..ym.lengthOfMonth()).map(ym::atDay)
    val marked = state.menu.map { it.date }.toSet()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Row { TextButton(onClick = { onChangeMonth(-1) }) { Text("‹", fontSize = 22.sp, color = Green) }; TextButton(onClick = { onChangeMonth(1) }) { Text("›", fontSize = 22.sp, color = Green) } }
            }
            Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().heightIn(max = 290.dp), userScrollEnabled = false) {
                items(dates) { date ->
                    Box(Modifier.aspectRatio(1f).padding(2.dp).then(if (date == null) Modifier else Modifier.clip(RoundedCornerShape(12.dp)).background(if (date == state.selectedDate) Green else if (date in marked) Color(0xFFE7F2E9) else Color.Transparent).clickable { onSelectDate(date) }), contentAlignment = Alignment.Center) {
                        if (date != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfMonth.toString(), color = if (date == state.selectedDate) Color.White else Ink, fontWeight = if (date == state.selectedDate) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                            if (date in marked) Box(Modifier.padding(top = 2.dp).size(4.dp).clip(RoundedCornerShape(2.dp)).background(if (date == state.selectedDate) Color.White else Green))
                        }
                    }
                }
            }
            Text("Green dots mark days with a menu", fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(body, fontSize = 14.sp, color = Muted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun MealCard(title: String, value: String, mark: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF3EC)),
                contentAlignment = Alignment.Center
            ) { Text(mark, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Green) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green)
                Text(value.ifBlank { "No items listed" }, fontSize = 15.sp, color = if (value.isBlank()) Muted else Ink, lineHeight = 21.sp)
            }
        }
    }
}

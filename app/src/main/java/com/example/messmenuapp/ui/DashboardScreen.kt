package com.example.messmenuapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MenuItemUI(
    val day: String,
    val breakfast: String,
    val lunch: String,
    val snacks: String,
    val dinner: String
)

private val Ink = Color(0xFF202A24)
private val Muted = Color(0xFF727C75)
private val Green = Color(0xFF236B4E)
private val Canvas = Color(0xFFF5F7F2)

@Composable
fun DashboardScreen(state: DashboardState, onImportClick: () -> Unit) {
    val menu = state.menu.firstOrNull()
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("MESS MENU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green, letterSpacing = 2.sp)
                Text("Eat well today.", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Your daily meal plan, all in one place.", fontSize = 15.sp, color = Muted)
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF28694D), Color(0xFF5A9B6D))))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TODAY'S MENU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .82f), letterSpacing = 1.5.sp)
                Text(menu?.day ?: if (state.isLoading) "Getting your menu…" else "What's cooking?", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    when {
                        state.isLoading -> "Reading your spreadsheet"
                        menu != null -> "Four meals planned for your day"
                        else -> "Import your mess schedule to get started"
                    },
                    fontSize = 14.sp, color = Color.White.copy(alpha = .88f)
                )
            }

            Button(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White)
            ) {
                Text(if (menu == null) "Choose menu spreadsheet" else "Update menu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Green)
            } else if (menu == null) {
                InfoCard(
                    title = if (state.message == null) "Your menu starts here" else "We couldn't find today's menu",
                    body = state.message ?: "Select the .xlsx schedule shared by your mess. Your menu will be saved on this device."
                )
            } else {
                Text("TODAY'S MEALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.4.sp)
                MealCard("Breakfast", menu.breakfast, "☀")
                MealCard("Lunch", menu.lunch, "◉")
                MealCard("Snacks", menu.snacks, "✦")
                MealCard("Dinner", menu.dinner, "☾")
                Text("Menu imported from your spreadsheet", fontSize = 12.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(8.dp))
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

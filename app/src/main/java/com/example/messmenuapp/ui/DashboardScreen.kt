package com.example.messmenuapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

@Composable
fun DashboardScreen(menuList: List<MenuItemUI>, onImportClick: () -> Unit) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF5F7BFF), Color(0xFFB68CFF)))

    Box(Modifier.fillMaxSize().background(Color(0xFFF6F7FB))) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Mess Menu", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Take Charge Of Your Meals", fontSize = 16.sp, color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .background(gradient, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Today's Menu", fontSize = 14.sp, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (menuList.isEmpty()) "No menu" else "Ready",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Today's menu schedule", fontSize = 13.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Import New Menu", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(menuList) { TodayMenuCard(it) }
            }
        }
    }
}

@Composable
private fun TodayMenuCard(item: MenuItemUI) {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.day, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            MealBox("Breakfast", item.breakfast)
            MealBox("Lunch", item.lunch)
            MealBox("Snacks", item.snacks)
            MealBox("Dinner", item.dinner)
        }
    }
}

@Composable
private fun MealBox(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7FA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(.30f))
            Text(if (value.isBlank()) "—" else value, fontSize = 14.sp, modifier = Modifier.weight(.70f))
        }
    }
}

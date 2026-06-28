package com.example.to_dotasktracker.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityPreferencesScreen(
    onBackClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val isDark = isSystemInDarkTheme()

    var sessionLength by remember { mutableFloatStateOf(45f) }
    var strictMode by remember { mutableStateOf(true) }
    var aiPrediction by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf("M", "T", "W", "T", "F")) }

    var fromTime by remember { mutableStateOf("10:00 PM") }
    var untilTime by remember { mutableStateOf("07:00 AM") }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPickingFrom by remember { mutableStateOf(true) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = if (isPickingFrom) 22 else 7,
            initialMinute = 0,
            is24Hour = false
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPickingFrom) "Select Start Time" else "Select End Time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    TimePicker(state = timePickerState)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                val hour = timePickerState.hour
                                val minute = timePickerState.minute
                                val amPm = if (hour >= 12) "PM" else "AM"
                                val displayHour = when {
                                    hour == 0 -> 12
                                    hour > 12 -> hour - 12
                                    else -> hour
                                }
                                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                                
                                if (isPickingFrom) fromTime = formattedTime else untilTime = formattedTime
                                showTimePicker = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Productivity Preferences",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = surfaceColor)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                tonalElevation = 0.dp
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    indicatorColor = primaryColor,
                    unselectedIconColor = onSurfaceVariant,
                    selectedTextColor = primaryColor,
                    unselectedTextColor = onSurfaceVariant
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onDashboardClick,
                    icon = { Icon(Icons.Outlined.GridView, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onTasksClick,
                    icon = { Icon(Icons.Outlined.TaskAlt, contentDescription = "Tasks") },
                    label = { Text("Tasks", fontSize = 10.sp) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onCalendarClick,
                    icon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 10.sp) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = true,
                    onClick = onProfileClick,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = navItemColors
                )
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Image Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF60A5FA))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            "Productivity\nPreferences",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Design your ideal systematic workflow",
                            style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        )
                    }
                }
            }

            // Deep Work Duration
            item {
                PreferenceCard(title = "Deep Work Duration", icon = Icons.Outlined.Timer, primaryColor = Color(0xFF3B82F6)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SESSION LENGTH",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
                            )
                            Text(
                                "${sessionLength.toInt()} min",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            )
                        }
                        Slider(
                            value = sessionLength,
                            onValueChange = { sessionLength = it },
                            valueRange = 15f..120f,
                            colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("15 MIN", style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant))
                            Text("60 MIN", style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant))
                            Text("120 MIN", style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PresetChip("Classic (25/5)", selected = true, primaryColor)
                            PresetChip("Flow State (50/10)", selected = false, primaryColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PresetChip("Endurance (90/15)", selected = false, primaryColor)
                    }
                }
            }

            // Daily Goal Targets
            item {
                PreferenceCard(title = "Daily Goal Targets", icon = Icons.Outlined.Flag, primaryColor = Color(0xFF10B981)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TargetItem(label = "TARGET FOCUS", value = "6.5 Hours", progress = 0.65f, primaryColor = primaryColor)
                        TargetItem(label = "TASK VELOCITY", value = "12 Tasks", progress = 0.4f, primaryColor = Color(0xFF10B981))
                    }
                }
            }

            // Focus Score Logic
            item {
                PreferenceCard(title = "Focus Score Logic", icon = Icons.Outlined.Analytics, primaryColor = Color(0xFF6366F1)) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SwitchItem(
                            title = "Strict Mode",
                            subtitle = "Deduct score for minor distractions",
                            checked = strictMode,
                            onCheckedChange = { strictMode = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor)
                        SwitchItem(
                            title = "AI Prediction",
                            subtitle = "Weight sessions by cognitive load",
                            checked = aiPrediction,
                            onCheckedChange = { aiPrediction = it }
                        )
                    }
                }
            }

            // Quiet Hours
            item {
                PreferenceCard(title = "Quiet Hours", icon = Icons.Outlined.NotificationsOff, primaryColor = Color(0xFFF87171)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimePickerField(
                                label = "FROM", 
                                time = fromTime, 
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isPickingFrom = true
                                    showTimePicker = true
                                }
                            )
                            TimePickerField(
                                label = "UNTIL", 
                                time = untilTime, 
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isPickingFrom = false
                                    showTimePicker = true
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "APPLY TO DAYS",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                                val isSelected = selectedDays.contains(day)
                                DayChip(day, isSelected, onClick = {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                }, primaryColor = primaryColor)
                            }
                        }
                    }
                }
            }

            // Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text("Discard Changes", style = TextStyle(fontWeight = FontWeight.Bold))
                    }
                    Button(
                        onClick = { /* Apply */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Apply Workflow", style = TextStyle(fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PreferenceCard(
    title: String,
    icon: ImageVector,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = primaryColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
            content()
        }
    }
}

@Composable
fun PresetChip(text: String, selected: Boolean, primaryColor: Color) {
    Surface(
        color = if (selected) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = TextStyle(
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun TargetItem(label: String, value: String, progress: Float, primaryColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                label,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = primaryColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun SwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(subtitle, style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun TimePickerField(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(modifier = modifier) {
        Text(
            label,
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(time, style = TextStyle(fontSize = 14.sp))
                Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DayChip(day: String, isSelected: Boolean, onClick: () -> Unit, primaryColor: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            day,
            style = TextStyle(
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductivityPreferencesScreenPreview() {
    ProductivityPreferencesScreen()
}

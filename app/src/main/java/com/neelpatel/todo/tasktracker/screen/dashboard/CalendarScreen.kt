package com.neelpatel.todo.tasktracker.screen.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neelpatel.todo.tasktracker.model.TaskRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDashboardClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTaskClick: (String) -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lightGrayBg = MaterialTheme.colorScheme.background
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var allTasks by remember { mutableStateOf<List<TaskRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    LaunchedEffect(userId) {
        if (userId != null) {
            val tasksRef = database.child("users").child(userId).child("tasks")
            tasksRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val taskList = mutableListOf<TaskRequest>()
                    val twentyFourHoursMillis = 24 * 60 * 60 * 1000L
                    val currentTime = System.currentTimeMillis()
                    
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(TaskRequest::class.java)
                        if (task != null) {
                            // Cleanup logic for Completed tasks
                            if (task.status == "Completed" && task.completedAt != null) {
                                if (currentTime - task.completedAt > twentyFourHoursMillis) {
                                    tasksRef.child(taskSnapshot.key ?: "").removeValue()
                                    continue
                                }
                            }
                            // Cleanup logic for Pending tasks
                            if (task.status == "Pending" && task.pendingAt != null) {
                                if (currentTime - task.pendingAt > twentyFourHoursMillis) {
                                    tasksRef.child(taskSnapshot.key ?: "").removeValue()
                                    continue
                                }
                            }
                            taskList.add(task)
                        }
                    }
                    allTasks = taskList
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                }
            })
        } else {
            isLoading = false
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    val filteredTasks = allTasks.filter { it.dueDate == selectedDate.format(dateFormatter) && it.status != "Completed" }

    val completedTasksCount = allTasks.count { it.status == "Completed" }
    val totalTasksCount = allTasks.size
    val focusScore = if (totalTasksCount > 0) (completedTasksCount * 100) / totalTasksCount else 0
    val deepWorkHours = completedTasksCount

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Focus",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    indicatorColor = primaryColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = primaryColor,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 10.sp) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onProfileClick,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = navItemColors
                )
            }
        },
        containerColor = lightGrayBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
        ) {
            // Dynamic Calendar View Card
            item {
                CalendarViewCard(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    tasks = allTasks
                )
            }

            // Tasks for selected date
            item {
                TaskSection(
                    date = selectedDate.format(dateFormatter),
                    tasks = filteredTasks,
                    primaryColor = primaryColor,
                    onTaskClick = onTaskClick
                )
            }

            // Productivity Score Card
            item {
                ProductivityScoreCard(primaryBlue = primaryColor, score = focusScore)
            }

            // Quick Stats Card
            item {
                QuickStatsCard(completedCount = completedTasksCount, deepWorkHrs = deepWorkHours)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarViewCard(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    tasks: List<TaskRequest> = emptyList()
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Row {
                    IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            // Days labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach {
                    Text(
                        it, 
                        modifier = Modifier.weight(1f), 
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(it == "SAT" || it == "SUN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dynamic Grid Calculation
            val firstDayOfMonth = displayedMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            
            val daysInMonth = displayedMonth.lengthOfMonth()
            val prevMonth = displayedMonth.minusMonths(1)
            val daysInPrevMonth = prevMonth.lengthOfMonth()
            
            val calendarDays = mutableListOf<Pair<LocalDate, Boolean>>() // Date to isCurrentMonth
            
            // Previous month overflow
            for (i in (firstDayOfWeek - 1) downTo 1) {
                val date = prevMonth.atDay(daysInPrevMonth - i + 1)
                calendarDays.add(date to false)
            }
            
            // Current month
            for (i in 1..daysInMonth) {
                val date = displayedMonth.atDay(i)
                calendarDays.add(date to true)
            }
            
            // Next month overflow to complete the grid (42 cells to ensure constant height)
            val nextMonth = displayedMonth.plusMonths(1)
            val remaining = 42 - calendarDays.size
            for (i in 1..remaining) {
                val date = nextMonth.atDay(i)
                calendarDays.add(date to false)
            }
            
            calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { (date, isCurrentMonth) ->
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White 
                                               else if (isToday) MaterialTheme.colorScheme.primary
                                               else if (!isCurrentMonth) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) 
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                // Priority Indicators
                                if (isCurrentMonth) {
                                    val hasPendingTasks = tasks.any { it.dueDate == date.format(dateFormatter) && it.status != "Completed" }
                                    
                                    if (hasPendingTasks) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSection(
    date: String,
    tasks: List<TaskRequest>,
    primaryColor: Color,
    onTaskClick: (String) -> Unit = {}
) {
    Column {
        Text(
            text = "Tasks for $date",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        if (tasks.isEmpty()) {
            Text(
                text = "No tasks scheduled for this day.",
                style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        } else {
            tasks.forEach { task ->
                TaskSmallItem(task = task, primaryColor = primaryColor, onClick = { task.id?.let { onTaskClick(it) } })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TaskSmallItem(task: TaskRequest, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (task.priority) {
                            "High" -> Color(0xFFEF4444)
                            "Medium" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.dueTime} • ${task.category}",
                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            if (task.status == "Completed") {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ProductivityScoreCard(primaryBlue: Color, score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = primaryBlue)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("PRODUCTIVITY SCORE", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("$score%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (score == 0) "Keep tracking your sessions to see your productivity score." 
                else "You're doing great! Keep up the consistent work.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { score.toFloat() / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun QuickStatsCard(completedCount: Int, deepWorkHrs: Int) {
    val context = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = context.surface),
        border = BorderStroke(1.dp, context.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quick Stats", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = context.onSurface))
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFDCFCE7).copy(alpha = if(isSystemInDarkTheme()) 0.2f else 1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Tasks Completed", modifier = Modifier.weight(1f), fontSize = 14.sp, color = context.onSurfaceVariant)
                Text("$completedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = context.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E7FF).copy(alpha = if(isSystemInDarkTheme()) 0.2f else 1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Deep Work Hrs", modifier = Modifier.weight(1f), fontSize = 14.sp, color = context.onSurfaceVariant)
                Text("${deepWorkHrs}h", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = context.onSurface)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen()
}

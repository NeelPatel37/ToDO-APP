package com.neelpatel.todo.tasktracker.screen.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neelpatel.todo.tasktracker.common.PreferenceManager
import com.neelpatel.todo.tasktracker.model.TaskRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.airbnb.lottie.compose.*
import com.neelpatel.todo.tasktracker.notification.NotificationHelper
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTasksClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onCreateTaskClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lightGrayBg = MaterialTheme.colorScheme.background
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    var userName by remember { mutableStateOf(preferenceManager.getUserName().ifEmpty { "User" }) }
    var userEmail by remember { mutableStateOf(preferenceManager.getUserEmail()) }

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference
    val notificationHelper = remember { NotificationHelper(context) }

    var allTasks by remember { mutableStateOf<List<TaskRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            // Fetch User Profile
            database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
                val name = snapshot.child("fullName").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                if (name.isNotEmpty()) {
                    userName = name
                    preferenceManager.saveUserName(name)
                }
                if (email.isNotEmpty()) {
                    userEmail = email
                    preferenceManager.saveUserEmail(email)
                }
            }

            val tasksRef = database.child("users").child(userId).child("tasks")
            tasksRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val taskList = mutableListOf<TaskRequest>()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(TaskRequest::class.java)
                        if (task != null) {
                            // Check and cleanup old completed tasks (24 hours = 86400000 ms)
                            if (task.status == "Completed") {
                                // Cancel notifications for completed tasks
                                notificationHelper.cancelTaskNotifications(task.id ?: "")
                                
                                if (task.completedAt != null) {
                                    val twentyFourHoursMillis = 24 * 60 * 60 * 1000L
                                    if (System.currentTimeMillis() - task.completedAt > twentyFourHoursMillis) {
                                        tasksRef.child(taskSnapshot.key ?: "").removeValue()
                                        continue
                                    }
                                }
                            }
                            
                            // Check and cleanup old pending tasks (24 hours)
                            if (task.status == "Pending") {
                                if (task.pendingAt != null) {
                                    val twentyFourHoursMillis = 24 * 60 * 60 * 1000L
                                    if (System.currentTimeMillis() - task.pendingAt > twentyFourHoursMillis) {
                                        tasksRef.child(taskSnapshot.key ?: "").removeValue()
                                        continue
                                    }
                                }
                            }
                            
                            taskList.add(task)
                            // Schedule notifications for existing tasks
                            notificationHelper.scheduleTaskNotifications(task)
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

    // Greeting logic
    val greeting = remember {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    // Task calculations
    val totalTasks = allTasks.size
    val doneCount = allTasks.count { it.status == "Completed" }
    val focusScore = if (totalTasks > 0) (doneCount * 100) / totalTasks else 0
    val deepWorkHours = doneCount // Consistent with ProfileScreen estimation
    val weeklyGrowth = if (totalTasks > 0) (doneCount * 100) / totalTasks else 0

    val priorityTasks = allTasks
        .filter { it.status != "Completed" && it.priority == "High" }
        .sortedByDescending { it.createdAt }
        .take(3)

    // Progress Animation State - Hoisted to prevent re-animation on scroll
    var animationPlayed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!animationPlayed) {
            animationPlayed = true
        }
    }

    // State for tracking which task is being confirmed for completion
    var taskToComplete by remember { mutableStateOf<TaskRequest?>(null) }

    // Confirmation Dialog
    if (taskToComplete != null) {
        AlertDialog(
            onDismissRequest = { taskToComplete = null },
            title = { 
                Text(
                    text = "Complete Task?", 
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                ) 
            },
            text = { 
                Text(
                    text = "Are you sure task completed?",
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val task = taskToComplete
                        if (userId != null && task?.id != null) {
                            val updates = mapOf(
                                "status" to "Completed",
                                "completedAt" to System.currentTimeMillis()
                            )
                            database.child("users").child(userId).child("tasks").child(task.id).updateChildren(updates)
                            notificationHelper.cancelTaskNotifications(task.id)
                            database.child("users").child(userId).child("tasks").child(task.id).child("pendingAt").removeValue()
                        }
                        taskToComplete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Complete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToComplete = null }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Focus",
                        style = TextStyle(

                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color =  primaryColor
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    selectedTextColor = if (isSystemInDarkTheme()) Color.White else primaryColor,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onTasksClick,
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = "Tasks") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
        ) {
            // Greeting Section
            item {
                Column {
                    Text(
                        text = "$greeting, $userName.",
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have $totalTasks tasks scheduled for today.",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Progress Card
            item {
                ProgressCard(
                    doneCount = doneCount, 
                    totalCount = totalTasks, 
                    primaryColor = primaryColor,
                    animationPlayed = animationPlayed
                )
            }

            // Capture Card
            item {
                CaptureCard(primaryColor = primaryColor, onQuickAddClick = onCreateTaskClick)
            }

            // Top Priorities Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Top Priorities",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "View All",
                        modifier = Modifier.clickable { onTasksClick() }.padding(8.dp),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSystemInDarkTheme()) Color.White else primaryColor
                        )
                    )
                }
            }

            // Task List
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
            } else if (priorityTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All caught up! No priorities left.",
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        )
                    }
                }
            } else {
                itemsIndexed(priorityTasks, key = { _, task -> task.id ?: "" }) { index, task ->
                    AnimatedListItem(index = index) {
                        PriorityItem(
                            title = task.title,
                            tag = task.category,
                            tagBg = when (task.category) {
                                "Work" -> Color(0xFFE0E7FF)
                                "Personal" -> Color(0xFFDCFCE7)
                                "Design" -> Color(0xFFDBEAFE)
                                "Health" -> Color(0xFFF3F4F6)
                                else -> Color(0xFFF3F4F6)
                            },
                            tagText = when (task.category) {
                                "Work" -> Color(0xFF4338CA)
                                "Personal" -> Color(0xFF15803D)
                                "Design" -> Color(0xFF1D4ED8)
                                "Health" -> Color(0xFF374151)
                                else -> Color(0xFF374151)
                            },
                            time = task.dueDate,
                            onComplete = {
                                taskToComplete = task
                            },
                            onClick = { task.id?.let { onTaskClick(it) } }
                        )
                    }
                }
            }

            // Stats Cards Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Performance",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                )
            }

            // Stats Cards
            item {
                StatCard(
                    icon = Icons.Default.Bolt,
                    label = "Focus Score",
                    value = "$focusScore/100",
                    primaryColor = primaryColor
                )
            }
            item {
                StatCard(
                    icon = Icons.Default.Timer,
                    label = "Deep Work Hours",
                    value = "${deepWorkHours}h Today",
                    primaryColor = primaryColor
                )
            }
            item {
                StatCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Weekly Growth",
                    value = "$weeklyGrowth%",
                    primaryColor = primaryColor
                )
            }
        }
    }
}

@Composable
fun AnimatedListItem(index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(index * 80L)
            visible = true
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha = animatedAlpha,
                translationX = animatedOffset
            )
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun ProgressCard(doneCount: Int, totalCount: Int, primaryColor: Color, animationPlayed: Boolean) {
    // Progress Animation Setup
    val progressTarget = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progressTarget else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )

    // Lottie Animation setup
    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/8568602b-8a71-4603-9f5b-972166e47963/5S5fG7I1Yy.json"))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Progress",
                modifier = Modifier.align(Alignment.Start),
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center) {
                // Animation View (Lottie)
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .size(110.dp)
                        .alpha(0.6f)
                )

                CircularProgress(
                    progress = animatedProgress,
                    primaryColor = primaryColor,
                    modifier = Modifier.size(150.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "COMPLETED",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$doneCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEFT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${totalCount - doneCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun CircularProgress(progress: Float, primaryColor: Color, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CaptureCard(primaryColor: Color, onQuickAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AddTask, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture a thought",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = "Quickly add a new task",
                    style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                )
            }
            
            Button(
                onClick = onQuickAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Quick Add", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PriorityItem(
    title: String, 
    tag: String, 
    tagBg: Color, 
    tagText: Color, 
    time: String, 
    onComplete: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(36.dp)
                    .background(Color(0xFF0056D2), RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = tagBg.copy(alpha = if(isSystemInDarkTheme()) 0.2f else 1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = TextStyle(fontSize = 10.sp, color = if(isSystemInDarkTheme()) tagBg else tagText, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = time, style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
            
            Checkbox(
                checked = false,
                onCheckedChange = { if (it) onComplete() },
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkedColor = Color(0xFF10B981)
                )
            )
        }
    }
}

@Composable
fun StatCard(icon: ImageVector, label: String, value: String, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium))
            Text(text = value, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen()
}

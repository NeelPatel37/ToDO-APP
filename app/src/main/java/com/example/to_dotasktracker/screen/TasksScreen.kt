package com.example.to_dotasktracker.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_dotasktracker.model.TaskRequest
import com.example.to_dotasktracker.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onDashboardClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCreateTaskClick: () -> Unit = {},
    onEditTaskClick: (String) -> Unit = {},
    onTaskClick: (String) -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lightGrayBg = MaterialTheme.colorScheme.background

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }

    var tasks by remember { mutableStateOf<List<TaskRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInProgressAlert by remember { mutableStateOf(false) }

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
                    tasks = taskList
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

    if (showInProgressAlert) {
        AlertDialog(
            onDismissRequest = { showInProgressAlert = false },
            title = { Text("Task In Progress", style = TextStyle(fontWeight = FontWeight.Bold)) },
            text = { Text("Your task is in progress so you can not modify it") },
            confirmButton = {
                TextButton(onClick = { showInProgressAlert = false }) {
                    Text("OK", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Focus",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            ))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    selected = true,
                    onClick = { },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTaskClick,
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = lightGrayBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {


            // Filter Tabs
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("To Do", "In Progress", "Completed", "Pending")

            Spacer(modifier = Modifier.height(20.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterTab(
                        text = title,
                        isSelected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Task List
            key(selectedTab) {
                val filteredTasks = tasks.filter { task ->
                    when (selectedTab) {
                        0 -> task.status == "To Do"
                        1 -> task.status == "In Progress"
                        2 -> task.status == "Completed"
                        3 -> task.status == "Pending"
                        else -> true
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks found for ${tabs[selectedTab]}.",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredTasks.size) { index ->
                            val task = filteredTasks[index]
                            AnimatedListItem(index = index) {
                                TaskCard(
                                    category = task.category,
                                    categoryColor = when (task.category) {
                                        "Work" -> Color(0xFFE0E7FF)
                                        "Personal" -> Color(0xFFDCFCE7)
                                        "Design" -> Color(0xFFDBEAFE)
                                        "Health" -> Color(0xFFF3F4F6)
                                        else -> Color(0xFFF3F4F6)
                                    },
                                    categoryTextColor = when (task.category) {
                                        "Work" -> Color(0xFF4338CA)
                                        "Personal" -> Color(0xFF15803D)
                                        "Design" -> Color(0xFF1D4ED8)
                                        "Health" -> Color(0xFF374151)
                                        else -> Color(0xFF374151)
                                    },
                                    title = task.title,
                                    description = task.description,
                                    date = task.dueDate,
                                    isCompleted = task.status == "Completed",
                                    onCompleteClick = {
                                        if (userId != null && task.id != null) {
                                            val isNowCompleted = task.status != "Completed"
                                            val updates = mutableMapOf<String, Any>(
                                                "status" to if (isNowCompleted) "Completed" else "To Do"
                                            )
                                            if (isNowCompleted) {
                                                updates["completedAt"] = System.currentTimeMillis()
                                            } else {
                                                updates["completedAt"] = com.google.firebase.database.ServerValue.TIMESTAMP // This will be removed on next fetch or we can use null
                                                // Using updateChildren with null doesn't delete the key, but we can handle it
                                            }
                                            
                                            // More reliable: replace null with a removal if needed, but for simplicity:
                                            database.child("users").child(userId).child("tasks").child(task.id).updateChildren(updates)
                                            if (isNowCompleted) {
                                                notificationHelper.cancelTaskNotifications(task.id)
                                                database.child("users").child(userId).child("tasks").child(task.id).child("pendingAt").removeValue()
                                            } else {
                                                database.child("users").child(userId).child("tasks").child(task.id).child("completedAt").removeValue()
                                                database.child("users").child(userId).child("tasks").child(task.id).child("pendingAt").removeValue()
                                                // Reschedule if moved back to To Do
                                                notificationHelper.scheduleTaskNotifications(task.copy(status = "To Do"))
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        if (userId != null && task.id != null) {
                                            database.child("users").child(userId).child("tasks").child(task.id).removeValue()
                                        }
                                    },
                                    onEditClick = { 
                                        if (task.status == "In Progress") {
                                            showInProgressAlert = true
                                        } else {
                                            task.id?.let { onEditTaskClick(it) } 
                                        }
                                    },
                                    subTasks = task.subTasks,
                                    onClick = {
                                        if (task.status != "Completed") {
                                            task.id?.let { onTaskClick(it) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
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
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -100f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
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
fun FilterTab(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = text,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun TaskCard(
    category: String,
    categoryColor: Color,
    categoryTextColor: Color,
    title: String,
    description: String,
    date: String,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
    subTasks: List<com.example.to_dotasktracker.model.SubTaskRequest> = emptyList(),
    onCompleteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val context = androidx.compose.ui.platform.LocalContext.current

    fun shareTask() {
        val shareText = """
            Focus Task: $title
            Category: $category
            Due: $date
            Details: $description
        """.trimIndent()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Task: $title")
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Task via"))
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = {
                Text(
                    text = if (isCompleted) "Reopen Task?" else "Complete Task?",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (isCompleted) "Are you sure you want to mark this task as incomplete?" else "Are you sure task completed?",
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        onCompleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isCompleted) "Reopen" else "Complete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Task?",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task? This action cannot be undone.",
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isCompleted) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 20.dp, start = 20.dp, end = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = categoryColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = TextStyle(fontSize = 12.sp, color = categoryTextColor, fontWeight = FontWeight.Bold)
                    )
                }
                
                if (!isCompleted) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp) // Minimum touch target size
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isCompleted) "Mark as Incomplete" else "Task Completed", fontSize = 14.sp) },
                                onClick = { 
                                    showMenu = false
                                    showCompleteDialog = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle, 
                                        contentDescription = null, 
                                        tint = if (isCompleted) Color.Gray else Color(0xFF10B981), 
                                        modifier = Modifier.size(18.dp)
                                    ) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit", fontSize = 14.sp) },
                                onClick = { 
                                    showMenu = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", fontSize = 14.sp, color = Color.Red) },
                                onClick = { 
                                    showMenu = false 
                                    showDeleteDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                Text(
                    text = description,
                    style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val completedSubTasks = subTasks.filter { it.isChecked }
                if (completedSubTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { completedSubTasks.size.toFloat() / subTasks.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = primaryColor,
                        trackColor = primaryColor.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${completedSubTasks.size} of ${subTasks.size} subtasks completed",
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    )
                    
                    completedSubTasks.take(2).forEach { subTask ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subTask.title, 
                                style = TextStyle(
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                ), 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.ErrorOutline else Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = date,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }

                    if (!isCompleted) {
                        IconButton(
                            onClick = { shareTask() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityTaskCard(title: String, description: String, onClick: () -> Unit = {}) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "PRIORITY TASK",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                )
            }
            
            Text(
                text = title,
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White),
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            
            Text(
                text = description,
                style = TextStyle(fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 24.sp),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(Color(0xFF60A5FA))
                    Spacer(modifier = Modifier.width((-12).dp))
                    Avatar(Color(0xFF34D399))
                    Spacer(modifier = Modifier.width((-12).dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+3", style = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
                
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("Start Session", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun Avatar(color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp).align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    TasksScreen()
}

package com.neelpatel.todo.tasktracker.screen.dashboard

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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neelpatel.todo.tasktracker.model.TaskRequest
import com.neelpatel.todo.tasktracker.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.airbnb.lottie.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String = "",
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    val primaryBlue = Color(0xFF0056D2)
    val lightGrayBg = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }

    var task by remember { mutableStateOf<TaskRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(taskId) {
        if (userId != null && taskId.isNotEmpty()) {
            database.child("users").child(userId).child("tasks").child(taskId).get()
                .addOnSuccessListener { snapshot ->
                    task = snapshot.getValue(TaskRequest::class.java)
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showInProgressAlert by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.neelpatel.todo.tasktracker.R.raw.success))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = showSuccessDialog
    )

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onBackClick()
            },
            title = {
                Text(
                    text = "Task Completed",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(120.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSuccessDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Great!", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
        
        // Auto-dismiss after animation
        LaunchedEffect(progress) {
            if (progress >= 1f) {
                kotlinx.coroutines.delay(500)
                showSuccessDialog = false
                onBackClick()
            }
        }
    }

    if (showInProgressAlert) {
        AlertDialog(
            onDismissRequest = { showInProgressAlert = false },
            title = { Text("Task In Progress", style = TextStyle(fontWeight = FontWeight.Bold)) },
            text = { Text("Your task is in progress so you can not modify it") },
            confirmButton = {
                TextButton(onClick = { showInProgressAlert = false }) {
                    Text("OK", color = primaryBlue, fontWeight = FontWeight.Bold)
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
                        if (userId != null && taskId.isNotEmpty()) {
                            database.child("users").child(userId).child("tasks").child(taskId).removeValue()
                                .addOnSuccessListener { onBackClick() }
                        }
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

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = {
                Text(
                    text = if (task?.status == "Completed") "Reopen Task?" else "Complete Task?",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (task?.status == "Completed") "Are you sure you want to mark this task as incomplete?" else "Are you sure task completed?",
                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        if (userId != null && taskId.isNotEmpty() && task != null) {
                            val isNowCompleted = task?.status != "Completed"
                            val updates = mutableMapOf<String, Any>(
                                "status" to if (isNowCompleted) "Completed" else "To Do"
                            )
                            if (isNowCompleted) {
                                updates["completedAt"] = System.currentTimeMillis()
                            }

                            database.child("users").child(userId).child("tasks").child(taskId).updateChildren(updates)
                                .addOnSuccessListener {
                                    if (isNowCompleted) {
                                        notificationHelper.cancelTaskNotifications(taskId)
                                        showSuccessDialog = true
                                    } else {
                                        database.child("users").child(userId).child("tasks").child(taskId).child("completedAt").removeValue()
                                        // Reschedule if moved back to To Do
                                        notificationHelper.scheduleTaskNotifications(task?.copy(status = "To Do") ?: return@addOnSuccessListener)
                                        onBackClick()
                                    }
                                    task = task?.copy(status = updates["status"] as String)
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (task?.status == "Completed") "Reopen" else "Complete", color = Color.White, fontWeight = FontWeight.Bold)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Focus", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryBlue), modifier = Modifier.padding(end = 40.dp)) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (task != null) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedIconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                if (task?.status == "In Progress") {
                                    showInProgressAlert = true
                                } else {
                                    onEditClick()
                                }
                            },
                            modifier = Modifier.weight(0.9f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                "Edit Details", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp, 
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Button(
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.weight(2.1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (task?.status == "Completed") "Mark as Incomplete" else "Mark as Complete", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp, 
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        },
        containerColor = lightGrayBg
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryBlue)
            }
        } else if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
            ) {
                // Category & Priority
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val catColor = when (task?.category) {
                            "Work" -> Color(0xFFE0E7FF)
                            "Personal" -> Color(0xFFDCFCE7)
                            "Design" -> Color(0xFFDBEAFE)
                            "Health" -> Color(0xFFF3F4F6)
                            else -> primaryBlue
                        }
                        val catText = when (task?.category) {
                            "Work" -> Color(0xFF4338CA)
                            "Personal" -> Color(0xFF15803D)
                            "Design" -> Color(0xFF1D4ED8)
                            "Health" -> Color(0xFF374151)
                            else -> Color.White
                        }

                        Surface(
                            color = catColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = task?.category ?: "",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = TextStyle(fontSize = 11.sp, color = catText, fontWeight = FontWeight.ExtraBold)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${task?.priority} Priority",
                                style = TextStyle(fontSize = 12.sp, color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Title
                item {
                    Text(
                        text = task?.title ?: "",
                        style = TextStyle(
                            fontSize = 28.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onBackground,
                            textDecoration = if (task?.status == "Completed") TextDecoration.LineThrough else null
                        )
                    )
                }

                // Start Date Section
                item {
                    OutlinedInfoCard(
                        label = "START DATE & TIME",
                        value = "${task?.startDate} at ${task?.startTime}",
                        icon = Icons.Default.AccessTime,
                        primaryBlue = primaryBlue
                    )
                }

                // Deadline Section
                item {
                    OutlinedInfoCard(
                        label = "DEADLINE",
                        value = "${task?.dueDate} at ${task?.dueTime}",
                        icon = Icons.Default.CalendarToday,
                        primaryBlue = primaryBlue
                    )
                }

                // Project Section (Using Category as Project if project is not in model)
                item {
                    OutlinedInfoCard(
                        label = "PROJECT",
                        value = task?.category ?: "General",
                        icon = Icons.Outlined.Folder,
                        primaryBlue = primaryBlue
                    )
                }

                // Description
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Description",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = task?.description?.ifEmpty { "No description provided." } ?: "No description provided.",
                                style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                            )
                        }
                    }
                }

                // Subtasks
                if (task?.subTasks?.isNotEmpty() == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val completedCount = task?.subTasks?.count { it.isChecked } ?: 0
                                val totalCount = task?.subTasks?.size ?: 0
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "Subtasks",
                                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    Text(
                                        text = "$completedCount of $totalCount completed",
                                        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                task?.subTasks?.forEachIndexed { index, subTask ->
                                    SubTaskDetailItem(
                                        text = subTask.title, 
                                        isChecked = subTask.isChecked,
                                        onCheckedChange = { checked ->
                                            if (userId != null && taskId.isNotEmpty()) {
                                                database.child("users").child(userId).child("tasks").child(taskId)
                                                    .child("subTasks").child(index.toString()).child("checked").setValue(checked)
                                                    .addOnSuccessListener {
                                                        val updatedSubTasks = task?.subTasks?.toMutableList()?.apply {
                                                            this[index] = subTask.copy(isChecked = checked)
                                                        }
                                                        task = task?.copy(subTasks = updatedSubTasks ?: emptyList())
                                                    }
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
}

@Composable
fun OutlinedInfoCard(label: String, value: String, icon: ImageVector, primaryBlue: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = value, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}

@Composable
fun SubTaskDetailItem(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp).clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.5.dp, 
                    color = if (isChecked) Color(0xFF10B981) else Color(0xFFD1D5DB), 
                    shape = RoundedCornerShape(6.dp)
                )
                .background(if (isChecked) Color(0xFF10B981).copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text, 
            style = TextStyle(
                fontSize = 14.sp, 
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        )
    }
}

@Composable
fun ActivityItem(title: String, time: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface))
            Text(time, style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

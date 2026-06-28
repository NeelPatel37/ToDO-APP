package com.example.to_dotasktracker.screen.dashboard

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.to_dotasktracker.model.TaskRequest
import com.example.to_dotasktracker.model.SubTaskRequest
import com.example.to_dotasktracker.notification.NotificationHelper

data class SubTask(val id: Long, val title: String, var isChecked: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    taskId: String? = null,
    onCancelClick: () -> Unit = {},
    onCreateClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val backgroundColor = colorScheme.background
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    val borderColor = colorScheme.outline

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    val userId = auth.currentUser?.uid
    val notificationHelper = remember { NotificationHelper(context) }

    // Form States
    var taskTitle by rememberSaveable { mutableStateOf("") }
    var taskDescription by rememberSaveable { mutableStateOf("") }
    var selectedPriorityIndex by rememberSaveable { mutableIntStateOf(1) }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    var pushReminder by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by rememberSaveable { mutableStateOf("Work") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isFetchingTask by remember { mutableStateOf(taskId != null) }

    // Subtasks State
    val subTasks = remember { mutableStateListOf<SubTask>() }
    var showAddSubTaskDialog by remember { mutableStateOf(false) }

    // Date & Time states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingFor by remember { mutableStateOf("due") } // "start" or "due"

    val calendar = Calendar.getInstance()
    var selectedDateText by rememberSaveable {
        mutableStateOf(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time))
    }
    var selectedTimeText by rememberSaveable {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time))
    }
    var startTaskDateText by rememberSaveable {
        mutableStateOf(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time))
    }
    var startTaskTimeText by rememberSaveable {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time))
    }

    // Fetch task details if in edit mode
    LaunchedEffect(taskId) {
        if (taskId != null && userId != null) {
            database.child("users").child(userId).child("tasks").child(taskId).get()
                .addOnSuccessListener { snapshot ->
                    val task = snapshot.getValue(TaskRequest::class.java)
                    if (task != null) {
                        taskTitle = task.title
                        taskDescription = task.description
                        selectedCategory = task.category
                        selectedPriorityIndex = when (task.priority) {
                            "Low" -> 0
                            "High" -> 2
                            else -> 1
                        }
                        startTaskDateText = task.startDate
                        startTaskTimeText = task.startTime
                        selectedDateText = task.dueDate
                        selectedTimeText = task.dueTime
                        isRecurring = task.recurring
                        pushReminder = task.pushReminder
                        subTasks.clear()
                        task.subTasks.forEachIndexed { index, sub ->
                            subTasks.add(SubTask(index.toLong(), sub.title, sub.isChecked))
                        }
                    }
                    isFetchingTask = false
                }
                .addOnFailureListener {
                    isFetchingTask = false
                }
        }
    }

    // Disallow past dates
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                return utcTimeMillis >= today
            }
        }
    )

    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    // Theme for Date Picker
    val customDatePickerColors = DatePickerDefaults.colors(
        containerColor = surfaceColor,
        titleContentColor = primaryColor,
        headlineContentColor = primaryColor,
        selectedDayContainerColor = primaryColor,
        selectedDayContentColor = colorScheme.onPrimary,
        todayContentColor = primaryColor,
        todayDateBorderColor = primaryColor,
        weekdayContentColor = onSurfaceVariantColor,
        navigationContentColor = primaryColor
    )

    // Add Subtask Dialog
    if (showAddSubTaskDialog) {
        var newSubTaskTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubTaskDialog = false },
            title = { Text("Add Sub-task", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurfaceColor)) },
            text = {
                OutlinedTextField(
                    value = newSubTaskTitle,
                    onValueChange = { newSubTaskTitle = it },
                    placeholder = { Text("Enter sub-task title...", color = onSurfaceVariantColor.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = borderColor,
                        unfocusedTextColor = onSurfaceColor,
                        focusedTextColor = onSurfaceColor,
                        unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubTaskTitle.isNotBlank()) {
                            subTasks.add(SubTask(System.currentTimeMillis(), newSubTaskTitle))
                            showAddSubTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold, color = colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubTaskDialog = false }) {
                    Text("Cancel", color = onSurfaceVariantColor, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = surfaceColor,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Date(it)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        if (pickingFor == "start") {
                            startTaskDateText = formatter.format(date)
                        } else {
                            selectedDateText = formatter.format(date)
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = onSurfaceVariantColor)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = surfaceColor),
            tonalElevation = 0.dp
        ) {
            DatePicker(
                state = datePickerState,
                colors = customDatePickerColors
            )
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min),
                color = surfaceColor,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )

                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = colorScheme.surfaceVariant,
                            clockDialSelectedContentColor = colorScheme.onPrimary,
                            clockDialUnselectedContentColor = onSurfaceColor,
                            selectorColor = primaryColor,
                            periodSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.15f),
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = primaryColor,
                            periodSelectorUnselectedContentColor = onSurfaceColor,
                            timeSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.15f),
                            timeSelectorUnselectedContainerColor = colorScheme.surfaceVariant,
                            timeSelectorSelectedContentColor = primaryColor,
                            timeSelectorUnselectedContentColor = onSurfaceColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel", color = onSurfaceVariantColor)
                        }
                        TextButton(onClick = {
                            val now = Calendar.getInstance()
                            val selectedDate = Calendar.getInstance().apply {
                                timeInMillis = datePickerState.selectedDateMillis ?: timeInMillis
                            }

                            val isToday = now.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                    now.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)

                            if (isToday) {
                                val selectedHour = timePickerState.hour
                                val selectedMinute = timePickerState.minute
                                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                                val currentMinute = now.get(Calendar.MINUTE)

                                if (selectedHour < currentHour || (selectedHour == currentHour && selectedMinute < currentMinute)) {
                                    Toast.makeText(context, "Cannot select past time for today", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                            }

                            val hour = timePickerState.hour
                            val minute = timePickerState.minute
                            val amPm = if (hour >= 12) "PM" else "AM"
                            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                            if (pickingFor == "start") {
                                startTaskTimeText = timeStr
                            } else {
                                selectedTimeText = timeStr
                            }
                            showTimePicker = false
                        }) {
                            Text("OK", fontWeight = FontWeight.Bold, color = primaryColor)
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
                        if (taskId != null) "Update Task" else "New Task",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancelClick) {
                        Text("Cancel", color = onSurfaceVariantColor, fontSize = 14.sp)
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            color = primaryColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = {
                                if (taskTitle.isBlank()) {
                                    Toast.makeText(context, "Please enter task title", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (userId == null) {
                                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                val finalTaskId = taskId ?: database.child("users").child(userId).child("tasks").push().key ?: ""
                                val priorities = listOf("Low", "Medium", "High")
                                
                                val taskRequest = TaskRequest(
                                    id = finalTaskId,
                                    title = taskTitle,
                                    description = taskDescription,
                                    category = selectedCategory,
                                    priority = priorities[selectedPriorityIndex],
                                    startDate = startTaskDateText,
                                    startTime = startTaskTimeText,
                                    dueDate = selectedDateText,
                                    dueTime = selectedTimeText,
                                    recurring = isRecurring,
                                    pushReminder = pushReminder,
                                    subTasks = subTasks.map { SubTaskRequest(it.title, it.isChecked) }
                                )

                                database.child("users").child(userId).child("tasks").child(finalTaskId).setValue(taskRequest)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        // Schedule notifications
                                        notificationHelper.scheduleTaskNotifications(taskRequest)

                                        Toast.makeText(context, if (taskId != null) "Task updated successfully" else "Task created successfully", Toast.LENGTH_SHORT).show()
                                        onCreateClick()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text(if (taskId != null) "Update" else "Create", color = colorScheme.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = surfaceColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        if (isFetchingTask) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
            ) {
                // Main Input Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            BasicTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(primaryColor),
                                decorationBox = { innerTextField ->
                                    if (taskTitle.isEmpty()) {
                                        Text(
                                            text = "What needs to be done?",
                                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = taskDescription,
                                onValueChange = { taskDescription = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = onSurfaceColor),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(primaryColor),
                                decorationBox = { innerTextField ->
                                    if (taskDescription.isEmpty()) {
                                        Text(
                                            text = "Add notes or details...",
                                            style = TextStyle(fontSize = 15.sp, color = onSurfaceVariantColor)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                // Sub-tasks Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "SUB-TASKS",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Render subtasks
                            subTasks.forEachIndexed { index, subTask ->
                                SubTaskItem(
                                    text = subTask.title,
                                    isCheckedInitial = subTask.isChecked,
                                    onCheckedChange = { checked ->
                                        subTasks[index] = subTask.copy(isChecked = checked)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { showAddSubTaskDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Sub-task", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Date and Time Row
                item {
                    Text(
                        text = "START DATE & TIME",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DateTimeCard(
                            icon = Icons.Default.CalendarToday,
                            label = "Start Date",
                            value = startTaskDateText,
                            modifier = Modifier.weight(1f),
                            primaryColor = primaryColor,
                            onClick = { 
                                pickingFor = "start"
                                showDatePicker = true 
                            }
                        )
                        DateTimeCard(
                            icon = Icons.Default.AccessTime,
                            label = "Time",
                            value = startTaskTimeText,
                            modifier = Modifier.weight(1f),
                            primaryColor = primaryColor,
                            onClick = { 
                                pickingFor = "start"
                                showTimePicker = true 
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "DUE DATE & TIME",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DateTimeCard(
                            icon = Icons.Default.CalendarToday,
                            label = "Due Date",
                            value = selectedDateText,
                            modifier = Modifier.weight(1f),
                            primaryColor = primaryColor,
                            onClick = { 
                                pickingFor = "due"
                                showDatePicker = true 
                            }
                        )
                        DateTimeCard(
                            icon = Icons.Default.AccessTime,
                            label = "Time",
                            value = selectedTimeText,
                            modifier = Modifier.weight(1f),
                            primaryColor = primaryColor,
                            onClick = { 
                                pickingFor = "due"
                                showTimePicker = true 
                            }
                        )
                    }
                }

                // Options Card (Priority, Category, Recurring)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Priority
                            Text(
                                text = "PRIORITY",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SegmentedControl(
                                options = listOf("Low", "Medium", "High"),
                                selectedIndex = selectedPriorityIndex,
                                onOptionSelected = { selectedPriorityIndex = it },
                                primaryColor = primaryColor
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Category
                            Text(
                                text = "CATEGORY",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box {
                                OutlinedCard(
                                    onClick = { showCategoryMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, borderColor),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val categoryColor = when(selectedCategory) {
                                            "Work" -> Color(0xFF059669)
                                            "Personal" -> Color(0xFF3B82F6)
                                            else -> Color(0xFFF59E0B)
                                        }
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(selectedCategory, fontSize = 14.sp, color = onSurfaceColor, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = onSurfaceVariantColor)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false },
                                    modifier = Modifier.background(surfaceColor)
                                ) {
                                    listOf("Work", "Personal", "Design", "Health").forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category, color = onSurfaceColor) },
                                            onClick = {
                                                selectedCategory = category
                                                showCategoryMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Recurring
                            Text(
                                text = "RECURRING",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariantColor, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, borderColor),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Daily", fontSize = 14.sp, color = onSurfaceColor, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = isRecurring,
                                        onCheckedChange = { isRecurring = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                                    )
                                }
                            }
                        }
                    }
                }


                // Reminder Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Push Reminder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                Text("15 min before", fontSize = 12.sp, color = onSurfaceVariantColor)
                            }
                            Switch(
                                checked = pushReminder,
                                onCheckedChange = { pushReminder = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                            )
                        }
                    }
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Deep work mode active • Focus v2.4",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 12.sp, color = onSurfaceVariantColor, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
fun SubTaskItem(text: String, isCheckedInitial: Boolean = false, onCheckedChange: (Boolean) -> Unit = {}) {
    var isChecked by rememberSaveable { mutableStateOf(isCheckedInitial) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { 
                isChecked = !isChecked 
                onCheckedChange(isChecked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isChecked) primaryColor else onSurfaceVariantColor.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .background(if (isChecked) primaryColor else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = if (isChecked) onSurfaceVariantColor else onSurfaceColor)
    }
}

@Composable
fun DateTimeCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onOptionSelected: (Int) -> Unit, primaryColor: Color) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(surfaceVariantColor, RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else surfaceColor
                    } else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "bg"
                )
                val shadowElevation by animateDpAsState(
                    targetValue = if (isSelected) 3.dp else 0.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "elevation"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(shadowElevation, RoundedCornerShape(8.dp))
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .selectable(
                            selected = isSelected,
                            onClick = { onOptionSelected(index) },
                            role = Role.RadioButton
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) primaryColor else onSurfaceVariantColor
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateTaskScreenPreview() {
    CreateTaskScreen()
}

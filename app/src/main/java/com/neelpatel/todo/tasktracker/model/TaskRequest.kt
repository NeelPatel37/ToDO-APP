package com.neelpatel.todo.tasktracker.model


data class TaskRequest(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val dueDate: String = "",
    val dueTime: String = "",
    val recurring: Boolean = false,
    val pushReminder: Boolean = false,
    val subTasks: List<SubTaskRequest> = emptyList(),
    val status: String = "To Do",
    val completedAt: Long? = null,
    val pendingAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class SubTaskRequest(
    val title: String = "",
    val isChecked: Boolean = false
)

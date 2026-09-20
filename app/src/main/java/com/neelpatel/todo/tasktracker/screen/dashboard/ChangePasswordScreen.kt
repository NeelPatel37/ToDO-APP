package com.neelpatel.todo.tasktracker.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neelpatel.todo.tasktracker.common.CommonAlertDialog
import com.neelpatel.todo.tasktracker.common.FormLabel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
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

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Status states
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableStateOf("Attention") }

    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Password Validation Logic
    val isLengthMet = newPassword.length >= 8
    val hasNumber = newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { !it.isLetterOrDigit() }
    
    val metCount = listOf(isLengthMet, hasNumber, hasSpecialChar).count { it }
    val strengthText = when (metCount) {
        3 -> "STRONG"
        2 -> "MEDIUM"
        else -> "WEAK"
    }
    val strengthColor = when (metCount) {
        3 -> Color(0xFF10B981) // Green
        2 -> primaryColor      // Primary Blue
        else -> Color(0xFFEAB308) // Amber/Yellow
    }
    val strengthProgress = when (metCount) {
        3 -> 1.0f
        2 -> 0.66f
        1 -> 0.33f
        else -> 0.05f
    }

    fun validateAndUpdatePassword() {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            dialogTitle = "Missing Fields"
            dialogMessage = "Please fill in all password fields."
            showDialog = true
            return
        }

        if (newPassword != confirmPassword) {
            dialogTitle = "Mismatch"
            dialogMessage = "New password and confirmation do not match."
            showDialog = true
            return
        }

        if (metCount < 3) {
            dialogTitle = "Weak Password"
            dialogMessage = "Please ensure your new password meets all security requirements."
            showDialog = true
            return
        }

        if (user != null && user.email != null) {
            isLoading = true
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        isLoading = false
                        if (updateTask.isSuccessful) {
                            dialogTitle = "Success"
                            dialogMessage = "Your password has been updated successfully."
                            showDialog = true
                            // Clear fields
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            dialogTitle = "Update Failed"
                            dialogMessage = updateTask.exception?.message ?: "Failed to update password."
                            showDialog = true
                        }
                    }
                } else {
                    isLoading = false
                    dialogTitle = "Authentication Failed"
                    dialogMessage = "Current password incorrect. Please try again."
                    showDialog = true
                }
            }
        }
    }

    CommonAlertDialog(
        show = showDialog,
        onDismiss = { 
            showDialog = false 
            if (dialogTitle == "Success") {
                onBackClick()
            }
        },
        title = dialogTitle,
        message = dialogMessage,
        primaryColor = primaryColor
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Change Password",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = surfaceColor
                )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ensure your account stays secure by using a strong password with at least 8 characters, including numbers and symbols.",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Password Fields Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Password
                        Column {
                            FormLabel("Current Password")
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                        Icon(
                                            if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = onSurfaceVariant
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = outlineColor,
                                    focusedBorderColor = primaryColor
                                )
                            )
                        }

                        // New Password
                        Column {
                            FormLabel("New Password")
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter new password", fontSize = 14.sp) },
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                        Icon(
                                            if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = onSurfaceVariant
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = outlineColor,
                                    focusedBorderColor = primaryColor
                                )
                            )
                        }

                        // Password Strength
                        Column {
                            LinearProgressIndicator(
                                progress = { strengthProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = strengthColor,
                                trackColor = outlineColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "PASSWORD STRENGTH: $strengthText",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceVariant
                                )
                            )
                        }

                        // Confirm New Password
                        Column {
                            FormLabel("Confirm New Password")
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Repeat new password", fontSize = 14.sp) },
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = onSurfaceVariant
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = outlineColor,
                                    focusedBorderColor = primaryColor
                                )
                            )
                        }
                    }
                }
            }

            // Requirements Section
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "PASSWORD REQUIREMENTS",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    )
                    
                    RequirementItem(text = "At least 8 characters", isMet = isLengthMet)
                    RequirementItem(text = "At least one number", isMet = hasNumber)
                    RequirementItem(text = "At least one special character", isMet = hasSpecialChar)
                }
            }

            // Update Button
            item {
                Button(
                    onClick = { validateAndUpdatePassword() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Update Password",
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen()
}

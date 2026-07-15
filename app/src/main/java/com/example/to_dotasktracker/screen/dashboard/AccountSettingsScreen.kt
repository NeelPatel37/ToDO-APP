package com.example.to_dotasktracker.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_dotasktracker.common.FormLabel
import com.example.to_dotasktracker.common.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onSessionManagementClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val context = androidx.compose.ui.platform.LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val userNamePref = preferenceManager.getUserName().ifEmpty { "User" }
    val userEmailPref = preferenceManager.getUserEmail().ifEmpty { "user@example.com" }

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    var fullName by remember { mutableStateOf(userNamePref) }
    var email by remember { mutableStateOf(userEmailPref) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var activeSessionsCount by remember { mutableIntStateOf(0) }
    var showEmailAlert by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId != null) {
            val sessionsRef = database.child("users").child(userId).child("sessions")
            sessionsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    activeSessionsCount = snapshot.childrenCount.toInt()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    if (showEmailAlert) {
        AlertDialog(
            onDismissRequest = { showEmailAlert = false },
            title = { Text("Update Email", style = TextStyle(fontWeight = FontWeight.Bold)) },
            text = { Text("You cannot change your email address. Please contact the support team for assistance.") },
            confirmButton = {
                TextButton(onClick = { showEmailAlert = false }) {
                    Text("OK", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = surfaceColor
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
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
            // Profile Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fullName,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Standard Plan • $email",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            // Personal Information
            item {
                SettingsSection(
                    title = "Personal Information",
                    icon = Icons.Outlined.Person
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            FormLabel("Full Name")
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = outlineColor,
                                    focusedBorderColor = primaryColor
                                )
                            )
                        }

                        Column {
                            FormLabel("Email Address")
                            Box(modifier = Modifier.fillMaxWidth().clickable { showEmailAlert = true }) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = TextStyle(fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = outlineColor,
                                        disabledTextColor = onSurfaceVariant,
                                        disabledContainerColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val user = auth.currentUser
                                if (fullName.isNotBlank() && userId != null && user != null) {
                                    isSaving = true
                                    
                                    // 1. Update Firebase Auth Profile
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build()
                                    
                                    user.updateProfile(profileUpdates).addOnCompleteListener { authTask ->
                                        if (authTask.isSuccessful) {
                                            // 2. Update Realtime Database
                                            database.child("users").child(userId).child("fullName").setValue(fullName)
                                                .addOnCompleteListener { dbTask ->
                                                    isSaving = false
                                                    if (dbTask.isSuccessful) {
                                                        // 3. Update Local Preferences
                                                        preferenceManager.saveUserName(fullName)
                                                    }
                                                }
                                        } else {
                                            isSaving = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, outlineColor)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = primaryColor, strokeWidth = 2.dp)
                            } else {
                                Text("Save Changes", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Security
            item {
                SettingsSection(
                    title = "Security",
                    icon = Icons.Outlined.Shield
                ) {
                    Column {
                        SecurityItem(
                            icon = Icons.Outlined.Lock,
                            title = "Change Password",
                            subtitle = "Last changed 3 months ago",
                            onClick = onChangePasswordClick
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor)
                        SecurityItem(
                            icon = Icons.Outlined.PhonelinkLock,
                            title = "Two-Factor Auth",
                            subtitle = "Currently Enabled",
                            trailingContent = {
                                Switch(
                                    checked = twoFactorEnabled,
                                    onCheckedChange = { twoFactorEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor)
                        SecurityItem(
                            icon = Icons.Outlined.Devices,
                            title = "Session Management",
                            subtitle = if (activeSessionsCount == 1) "1 active device" else "$activeSessionsCount active devices",
                            onClick = onSessionManagementClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            content()
        }
    }
}

@Composable
fun SecurityItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingsScreenPreview() {
    AccountSettingsScreen()
}

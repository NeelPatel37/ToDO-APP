package com.example.to_dotasktracker.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_dotasktracker.common.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class DeviceSession(
    val id: String = "",
    val deviceName: String = "",
    val location: String = "",
    val lastActive: String = "",
    val isCurrentDevice: Boolean = false,
    val icon: ImageVector = Icons.Outlined.Smartphone
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(
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

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val currentSessionId = preferenceManager.getSessionId()

    var sessions by remember { mutableStateOf<List<DeviceSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId != null) {
            val sessionsRef = database.child("users").child(userId).child("sessions")
            sessionsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sessionList = mutableListOf<DeviceSession>()
                    for (sessionSnapshot in snapshot.children) {
                        val deviceName = sessionSnapshot.child("deviceName").getValue(String::class.java) ?: ""
                        val location = sessionSnapshot.child("location").getValue(String::class.java) ?: ""
                        val lastActive = sessionSnapshot.child("lastActive").getValue(String::class.java) ?: ""
                        val id = sessionSnapshot.key ?: ""
                        
                        sessionList.add(
                            DeviceSession(
                                id = id,
                                deviceName = deviceName,
                                location = location,
                                lastActive = lastActive,
                                isCurrentDevice = id == currentSessionId,
                                icon = if (deviceName.contains("MacBook", true) || deviceName.contains("Laptop", true)) 
                                    Icons.Outlined.Laptop 
                                else if (deviceName.contains("iPad", true) || deviceName.contains("Tablet", true))
                                    Icons.Outlined.TabletAndroid
                                else Icons.Outlined.Smartphone
                            )
                        )
                    }
                    sessions = sessionList
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

    if (showLogoutAllDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutAllDialog = false },
            title = { Text("Log Out from Other Devices", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) },
            text = { Text("Are you sure you want to log out from all other active sessions?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutAllDialog = false
                        if (userId != null) {
                            val sessionsRef = database.child("users").child(userId).child("sessions")
                            sessionsRef.get().addOnSuccessListener { snapshot ->
                                for (sessionSnapshot in snapshot.children) {
                                    val id = sessionSnapshot.key
                                    if (id != currentSessionId && id != null) {
                                        sessionsRef.child(id).removeValue()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
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
                        "Session Management",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Active Sessions",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                items(sessions) { session ->
                    SessionItem(
                        session = session, 
                        primaryColor = primaryColor,
                        onLogoutClick = {
                            if (userId != null) {
                                database.child("users").child(userId).child("sessions").child(session.id).removeValue()
                            }
                        }
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No device found",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showLogoutAllDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                            contentColor = Color(0xFFEF4444)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Log out from all other devices", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: DeviceSession, primaryColor: Color, onLogoutClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(session.icon, contentDescription = null, tint = primaryColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.deviceName,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    if (session.isCurrentDevice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "THIS DEVICE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                            )
                        }
                    }
                }
                Text(
                    text = "${session.location} • ${session.lastActive}",
                    style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            
            if (!session.isCurrentDevice) {
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out device", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

package com.neelpatel.todo.tasktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.neelpatel.todo.tasktracker.screen.dashboard.AccountSettingsScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.CalendarScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.ChangePasswordScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.CreateTaskScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.DashboardScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.ProfileScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.TaskDetailScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.ProductivityPreferencesScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.SessionManagementScreen
import com.neelpatel.todo.tasktracker.screen.dashboard.SupportHelpScreen
import com.neelpatel.todo.tasktracker.screen.TasksScreen
import com.neelpatel.todo.tasktracker.screen.auth.LoginScreen
import com.neelpatel.todo.tasktracker.screen.auth.RegisterScreen
import com.neelpatel.todo.tasktracker.ui.theme.ToDoTaskTrackerTheme
import com.neelpatel.todo.tasktracker.common.PreferenceManager
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val primaryBlue = Color(0xFF0056D2)

        
        setContent {
            val context = LocalContext.current
            val preferenceManager = remember { PreferenceManager(context) }
            
            // Notification Permission Handling
            var hasNotificationPermission by remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                } else {
                    mutableStateOf(true)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    hasNotificationPermission = isGranted
                }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            var selectedAppearance by rememberSaveable { mutableStateOf("Default") }
            var currentScreen by remember { 
                mutableStateOf(if (preferenceManager.isLoggedIn()) "dashboard" else "login") 
            }
            var selectedTaskId by rememberSaveable { mutableStateOf("") }
            val isAuthScreen = currentScreen == "login" || currentScreen == "register"

            // Auto-logout Logic: Check if current session still exists in Firebase
            DisposableEffect(currentScreen) {
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid
                val currentSessionId = preferenceManager.getSessionId()
                
                Log.d("MainActivity", "Auto-logout Check: Screen=$currentScreen, UID=$userId, Session=$currentSessionId")

                var sessionRef: com.google.firebase.database.DatabaseReference? = null
                var sessionListener: ValueEventListener? = null

                if (userId != null && currentSessionId.isNotEmpty() && !isAuthScreen) {
                    sessionRef = FirebaseDatabase.getInstance().reference
                        .child("users").child(userId).child("sessions")
                    
                    sessionListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.hasChild(currentSessionId)) {
                                Log.w("MainActivity", "Session $currentSessionId not found! Logging out.")
                                // Session has been removed remotely
                                auth.signOut()
                                preferenceManager.clear()
                                currentScreen = "login"
                            } else {
                                Log.d("MainActivity", "Session $currentSessionId is valid.")
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("MainActivity", "Session listener cancelled: ${error.message}")
                        }
                    }
                    
                    sessionRef.addValueEventListener(sessionListener)
                }

                onDispose {
                    if (sessionRef != null && sessionListener != null) {
                        Log.d("MainActivity", "Removing session listener for $currentSessionId")
                        sessionRef.removeEventListener(sessionListener)
                    }
                }
            }

            val darkTheme = when (selectedAppearance) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            // Adaptive status bar
            LaunchedEffect(darkTheme, isAuthScreen) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        if (isAuthScreen) {
                            SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                        } else {
                            SystemBarStyle.dark(primaryBlue.toArgb())
                        }
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
                    }
                )
            }

            ToDoTaskTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(500)) + 
                                     slideInHorizontally(animationSpec = tween(500), initialOffsetX = { if (targetState == "login" || targetState == "register") -it else it }))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(500)) + 
                                        slideOutHorizontally(animationSpec = tween(500), targetOffsetX = { if (targetState == "login" || targetState == "register") it else -it })
                                    )
                                },
                                label = "ScreenTransition"
                            ) { targetScreen ->
                                when (targetScreen) {
                                    "login" -> LoginScreen(
                                        onRegisterClick = { currentScreen = "register" },
                                        onLoginSuccess = { currentScreen = "dashboard" }
                                    )
                                    "register" -> RegisterScreen(
                                        onSignInClick = { currentScreen = "login" },
                                        onRegisterSuccess = { currentScreen = "dashboard" }
                                    )
                                    "dashboard" -> DashboardScreen(
                                        onTasksClick = { currentScreen = "tasks" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                        onProfileClick = { currentScreen = "profile" },
                                        onTaskClick = { taskId -> 
                                            selectedTaskId = taskId
                                            currentScreen = "taskDetail" 
                                        },
                                        onCreateTaskClick = {
                                            selectedTaskId = ""
                                            currentScreen = "createTask"
                                        }
                                    )
                                    "tasks" -> TasksScreen(
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                        onProfileClick = { currentScreen = "profile" },
                                        onCreateTaskClick = { 
                                            selectedTaskId = ""
                                            currentScreen = "createTask" 
                                        },
                                        onEditTaskClick = { taskId ->
                                            selectedTaskId = taskId
                                            currentScreen = "createTask"
                                        },
                                        onTaskClick = { taskId -> 
                                            selectedTaskId = taskId
                                            currentScreen = "taskDetail" 
                                        }
                                    )
                                    "calendar" -> CalendarScreen(
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onTasksClick = { currentScreen = "tasks" },
                                        onProfileClick = { currentScreen = "profile" }
                                    )
                                    "profile" -> ProfileScreen(
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onTasksClick = { currentScreen = "tasks" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                    onLogoutClick = { currentScreen = "login" },
                                    onAccountSettingsClick = { currentScreen = "accountSettings" },
                                    onSupportHelpClick = { currentScreen = "supportHelp" },
                                    selectedAppearance = selectedAppearance,
                                        onAppearanceChange = { selectedAppearance = it }
                                    )
                                    "accountSettings" -> AccountSettingsScreen(
                                        onBackClick = { currentScreen = "profile" },
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onTasksClick = { currentScreen = "tasks" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                        onProfileClick = { currentScreen = "profile" },
                                        onChangePasswordClick = { currentScreen = "changePassword" },
                                        onSessionManagementClick = { currentScreen = "sessionManagement" }
                                    )
                                    "sessionManagement" -> SessionManagementScreen(
                                        onBackClick = { currentScreen = "accountSettings" },
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onTasksClick = { currentScreen = "tasks" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                        onProfileClick = { currentScreen = "profile" }
                                    )
                                    "productivityPreferences" -> ProductivityPreferencesScreen(
                                        onBackClick = { currentScreen = "profile" },
                                        onDashboardClick = { currentScreen = "dashboard" },
                                        onTasksClick = { currentScreen = "tasks" },
                                        onCalendarClick = { currentScreen = "calendar" },
                                        onProfileClick = { currentScreen = "profile" }
                                    )
                                    "changePassword" -> ChangePasswordScreen(
                                    onBackClick = { currentScreen = "accountSettings" },
                                    onDashboardClick = { currentScreen = "dashboard" },
                                    onTasksClick = { currentScreen = "tasks" },
                                    onCalendarClick = { currentScreen = "calendar" },
                                    onProfileClick = { currentScreen = "profile" }
                                )
                                "supportHelp" -> SupportHelpScreen(
                                    onBackClick = { currentScreen = "profile" }
                                )
                                "createTask" -> CreateTaskScreen(
                                        taskId = if (selectedTaskId.isEmpty()) null else selectedTaskId,
                                        onCancelClick = { currentScreen = "tasks" },
                                        onCreateClick = { currentScreen = "tasks" }
                                    )
                                    "taskDetail" -> TaskDetailScreen(
                                        taskId = selectedTaskId,
                                        onBackClick = { currentScreen = "tasks" },
                                        onEditClick = { currentScreen = "createTask" }
                                    )
                                }
                            }
                        }

                        // Overlay for status bar in light mode (non-auth screens)
                        if (!darkTheme && !isAuthScreen) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(primaryBlue)
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                            )
                        }
                    }
                }
            }
        }
    }
}

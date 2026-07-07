package com.example.to_dotasktracker.screen.auth

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_dotasktracker.common.CommonAlertDialog
import com.example.to_dotasktracker.common.FormLabel
import com.example.to_dotasktracker.common.SocialButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.example.to_dotasktracker.R
import com.example.to_dotasktracker.common.PreferenceManager
import android.util.Log

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Focus manager to handle keyboard actions
    val focusManager = LocalFocusManager.current

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Duplicate device handling
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var pendingSessionId by remember { mutableStateOf("") }
    var pendingSessionData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var pendingUid by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid ?: ""
                        
                        // Fetch user info from database
                        database.child("users").child(uid).get().addOnSuccessListener { snapshot ->
                            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: user?.displayName ?: "User"
                            val userEmail = user?.email ?: ""
                            
                            Log.d("LoginScreen", "Fetched user profile: Name=$fullName, Email=$userEmail")
                            
                            preferenceManager.saveUserName(fullName)
                            preferenceManager.saveUserEmail(userEmail)
                            preferenceManager.setLoginState(true)

                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            
                            // Check for existing session with same device name
                            var existingSessionId: String? = null
                            val sessionsSnapshot = snapshot.child("sessions")
                            for (session in sessionsSnapshot.children) {
                                if (session.child("deviceName").getValue(String::class.java) == deviceName) {
                                    existingSessionId = session.key
                                    break
                                }
                            }

                            val sessionId = existingSessionId ?: database.child("users").child(uid).child("sessions").push().key ?: ""
                            val sessionData = mapOf(
                                "deviceName" to deviceName,
                                "location" to "Mumbai, India", // Placeholder
                                "lastActive" to "Active now",
                                "timestamp" to System.currentTimeMillis()
                            )
                            
                            Log.d("LoginScreen", "Creating session: ID=$sessionId")

                            if (existingSessionId != null) {
                                // Prepare for dialog
                                Log.d("LoginScreen", "Duplicate session found, showing dialog")
                                pendingUid = uid
                                pendingSessionId = sessionId
                                pendingSessionData = sessionData
                                showDuplicateDialog = true
                                isLoading = false
                            } else {
                                // Proceed normally
                                database.child("users").child(uid).child("sessions").child(sessionId).setValue(sessionData)
                                    .addOnSuccessListener {
                                        Log.d("LoginScreen", "Session created successfully, navigating to dashboard")
                                        preferenceManager.saveSessionId(sessionId)
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener {
                                        Log.e("LoginScreen", "Failed to create session: ${it.message}")
                                        dialogMessage = "Failed to create session. Please try again."
                                        showDialog = true
                                        isLoading = false
                                    }
                            }
                        }.addOnFailureListener {
                            Log.e("LoginScreen", "Failed to fetch profile: ${it.message}")
                            dialogMessage = "Failed to fetch user profile."
                            showDialog = true
                            isLoading = false
                        }
                    } else {
                        dialogMessage = authResult.exception?.message ?: "Google Sign-In failed."
                        showDialog = true
                        isLoading = false
                    }
                }
        } catch (e: ApiException) {
            dialogMessage = "Google sign in failed: ${e.message}"
            showDialog = true
        }
    }

    val primaryBlue = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline
    val grayText = MaterialTheme.colorScheme.onSurfaceVariant

    fun validateAndLogin() {
        if (email.isBlank()) {
            dialogMessage = "The email address field cannot be empty. Please provide your email to sign in."
            showDialog = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            dialogMessage = "This doesn't look like a valid email. Please check for typos."
            showDialog = true
        } else if (password.isBlank()) {
            dialogMessage = "Please enter your password to authenticate."
            showDialog = true
        } else {
            isLoading = true
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid ?: ""

                        // Fetch user info from database
                        database.child("users").child(uid).get().addOnSuccessListener { snapshot ->
                            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: user?.displayName ?: "User"
                            val userEmail = user?.email ?: ""

                            Log.d("LoginScreen", "Fetched user profile (Email Login): Name=$fullName, Email=$userEmail")

                            preferenceManager.saveUserName(fullName)
                            preferenceManager.saveUserEmail(userEmail)
                            preferenceManager.setLoginState(true)

                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            
                            // Check for existing session with same device name
                            var existingSessionId: String? = null
                            val sessionsSnapshot = snapshot.child("sessions")
                            for (session in sessionsSnapshot.children) {
                                if (session.child("deviceName").getValue(String::class.java) == deviceName) {
                                    existingSessionId = session.key
                                    break
                                }
                            }

                            val sessionId = existingSessionId ?: database.child("users").child(uid).child("sessions").push().key ?: ""
                            val sessionData = mapOf(
                                "deviceName" to deviceName,
                                "location" to "Mumbai, India", // Placeholder
                                "lastActive" to "Active now",
                                "timestamp" to System.currentTimeMillis()
                            )
                            
                            Log.d("LoginScreen", "Creating session (Email Login): ID=$sessionId")

                            if (existingSessionId != null) {
                                // Prepare for dialog
                                Log.d("LoginScreen", "Duplicate session found (Email Login), showing dialog")
                                pendingUid = uid
                                pendingSessionId = sessionId
                                pendingSessionData = sessionData
                                showDuplicateDialog = true
                                isLoading = false
                            } else {
                                // Proceed normally
                                database.child("users").child(uid).child("sessions").child(sessionId).setValue(sessionData)
                                    .addOnSuccessListener {
                                        Log.d("LoginScreen", "Session created successfully (Email Login), navigating to dashboard")
                                        preferenceManager.saveSessionId(sessionId)
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener {
                                        Log.e("LoginScreen", "Failed to create session (Email Login): ${it.message}")
                                        dialogMessage = "Failed to create session. Please try again."
                                        showDialog = true
                                        isLoading = false
                                    }
                            }
                        }.addOnFailureListener {
                            Log.e("LoginScreen", "Failed to fetch profile (Email Login): ${it.message}")
                            dialogMessage = "Failed to fetch user profile."
                            showDialog = true
                            isLoading = false
                        }
                    } else {
                        dialogMessage = task.exception?.message ?: "Login failed. Please check your credentials."
                        showDialog = true
                        isLoading = false
                    }
                }
        }
    }

    // Common AlertDialog integration
    CommonAlertDialog(
        show = showDialog,
        onDismiss = { showDialog = false },
        title = "Attention Needed",
        message = dialogMessage,
        primaryColor = primaryBlue
    )
    
    // Duplicate Device Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Device Already Linked", style = TextStyle(fontWeight = FontWeight.Bold)) },
            text = { Text("This device is already logged in with this account. Would you like to refresh your session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        isLoading = true
                        if (pendingSessionData != null) {
                            database.child("users").child(pendingUid).child("sessions").child(pendingSessionId).setValue(pendingSessionData)
                                .addOnSuccessListener {
                                    Log.d("LoginScreen", "Session refreshed successfully: ID=$pendingSessionId")
                                    preferenceManager.setLoginState(true)
                                    preferenceManager.saveSessionId(pendingSessionId)
                                    onLoginSuccess()
                                }
                                .addOnFailureListener {
                                    Log.e("LoginScreen", "Failed to refresh session: ${it.message}")
                                    dialogMessage = "Failed to refresh session."
                                    showDialog = true
                                    isLoading = false
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Text("Refresh Session", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and App Name (Matched with RegisterScreen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(primaryBlue, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◎", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Focus",
                    style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome back",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Systematic progress starts here.",
                style = TextStyle(fontSize = 14.sp, color = grayText, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card (Matched with RegisterScreen background)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Email Field
                    FormLabel("EMAIL ADDRESS")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("name@company.com", color = grayText.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = borderColor,
                            focusedBorderColor = primaryBlue,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = TextStyle(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Label
                    FormLabel("PASSWORD")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = grayText.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = icon, contentDescription = null, tint = grayText, modifier = Modifier.size(20.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = borderColor,
                            focusedBorderColor = primaryBlue,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = TextStyle(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                focusManager.clearFocus()
                                validateAndLogin()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign In Button
                    Button(
                        onClick = { validateAndLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Sign In",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f), 
                            thickness = 1.dp, 
                            color = borderColor
                        )
                        Text(
                            text = "Or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 12.sp,
                            color = grayText
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f), 
                            thickness = 1.dp, 
                            color = borderColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social Buttons
                    SocialButton(
                        text = "Google",
                        icon = "G",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            launcher.launch(googleSignInClient.signInIntent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = grayText,
                    fontSize = 14.sp
                )
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = primaryBlue, fontWeight = FontWeight.Bold)) {
                        append("Register")
                    }
                }
                ClickableText(
                    text = annotatedString,
                    onClick = { onRegisterClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onRegisterClick = {})
}

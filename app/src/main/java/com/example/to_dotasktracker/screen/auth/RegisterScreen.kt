package com.example.to_dotasktracker.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.focus.FocusDirection
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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.example.to_dotasktracker.R
import com.example.to_dotasktracker.common.PreferenceManager

@Composable
fun RegisterScreen(
    onSignInClick: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Focus manager to handle keyboard actions
    val focusManager = LocalFocusManager.current

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
                        val name = user?.displayName ?: ""
                        
                        // Save Google user to database if new
                        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                        val sessionId = database.child("users").child(uid).child("sessions").push().key ?: ""
                        val sessionData = mapOf(
                            "deviceName" to deviceName,
                            "location" to "Mumbai, India", // Placeholder
                            "lastActive" to "Active now",
                            "timestamp" to System.currentTimeMillis()
                        )

                        val userData = mapOf(
                            "fullName" to name,
                            "email" to (user?.email ?: ""),
                            "profileImage" to (user?.photoUrl?.toString() ?: "")
                        )
                        
                        database.child("users").child(uid).updateChildren(userData).addOnSuccessListener {
                            database.child("users").child(uid).child("sessions").child(sessionId).setValue(sessionData)
                                .addOnSuccessListener {
                                    preferenceManager.saveUserName(name)
                                    preferenceManager.saveUserEmail(user?.email ?: "")
                                    preferenceManager.setLoginState(true)
                                    preferenceManager.saveSessionId(sessionId)
                                    onRegisterSuccess()
                                }
                                .addOnFailureListener {
                                    dialogMessage = "Failed to create session. Please try again."
                                    showDialog = true
                                    isLoading = false
                                }
                        }.addOnFailureListener {
                            dialogMessage = "Failed to update user profile."
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
            isLoading = false
        }
    }

    val primaryBlue = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline
    val grayText = MaterialTheme.colorScheme.onSurfaceVariant

    fun validateAndRegister() {
        if (fullName.isBlank()) {
            dialogMessage = "Please enter your full name."
            showDialog = true
        } else if (email.isBlank()) {
            dialogMessage = "Please enter your email address."
            showDialog = true
        } else if (password.length < 6) {
            dialogMessage = "Password must be at least 6 characters."
            showDialog = true
        } else {
            isLoading = true
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()
                            
                            user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                                val sessionId = database.child("users").child(uid).child("sessions").push().key ?: ""
                                val sessionData = mapOf(
                                    "deviceName" to deviceName,
                                    "location" to "Mumbai, India", // Placeholder
                                    "lastActive" to "Active now",
                                    "timestamp" to System.currentTimeMillis()
                                )

                                val userData = mapOf(
                                    "fullName" to fullName,
                                    "email" to email
                                )
                                
                                database.child("users").child(uid).setValue(userData).addOnSuccessListener {
                                    database.child("users").child(uid).child("sessions").child(sessionId).setValue(sessionData)
                                        .addOnSuccessListener {
                                            preferenceManager.saveUserName(fullName)
                                            preferenceManager.saveUserEmail(email)
                                            preferenceManager.setLoginState(true)
                                            preferenceManager.saveSessionId(sessionId)
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener {
                                            dialogMessage = "Failed to create session. Please try again."
                                            showDialog = true
                                            isLoading = false
                                        }
                                }.addOnFailureListener {
                                    dialogMessage = "Failed to create user profile."
                                    showDialog = true
                                    isLoading = false
                                }
                            }
                        } else {
                            isLoading = false
                        }
                    } else {
                        dialogMessage = task.exception?.message ?: "Registration failed."
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
            // Logo and App Name
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
                text = "Create Account",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Start your deep work journey today.",
                style = TextStyle(fontSize = 14.sp, color = grayText, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FormLabel("Full Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { input ->
                            // Allow only characters and spaces
                            fullName = input.filter { it.isLetter() || it == ' ' }
                        },
                        placeholder = { Text("John Doe", color = grayText.copy(alpha = 0.6f), fontSize = 14.sp) },
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FormLabel("Email")
                    Spacer(modifier = Modifier.height(8.dp))

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FormLabel("Password")

                    Spacer(modifier = Modifier.height(8.dp))

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            validateAndRegister()
                        })
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { validateAndRegister() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Create Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = borderColor)
                        Text(text = "Or", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp, color = grayText)
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = borderColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Already have an account? ", color = grayText, fontSize = 14.sp)
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = primaryBlue, fontWeight = FontWeight.Bold)) {
                        append("Sign In")
                    }
                }
                ClickableText(text = annotatedString, onClick = { onSignInClick() })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}

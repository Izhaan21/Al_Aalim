package com.example.al_aalim.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.al_aalim.R
import com.example.al_aalim.viewmodel.AuthState
import com.example.al_aalim.viewmodel.AuthViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.ui.permissions.LocationPermissionActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AlAalimTheme {
                RegisterScreen(
                    onBackClick = { finish() },
                    onNavigateToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onRegisterSuccess = {
                        val intent = Intent(this, LocationPermissionActivity::class.java)
                        intent.putExtra("from_auth", true)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationEmail by remember { mutableStateOf("") }
    var verificationPassword by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
            .imePadding()
    ) {
        AdaptiveContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text("Create Account", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "Join our spiritual community",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Full Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full Name", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_account_main), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_account_email), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Gold) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Task 2: Password Strength Indicator
            if (password.isNotEmpty()) {
                val strength = when {
                    password.length < 8 -> "Weak"
                    password.any { it.isDigit() } && password.any { it.isUpperCase() } && password.any { !it.isLetterOrDigit() } -> "Strong"
                    else -> "Medium"
                }
                val strengthColor = when (strength) {
                    "Weak" -> Color(0xFFEF5350)
                    "Medium" -> Color(0xFFFFCA28)
                    else -> Color(0xFF66BB6A)
                }
                Text(
                    text = "Password Strength: $strength",
                    color = strengthColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Task 3: Register Button with Loading Icon
            Button(
                onClick = { viewModel.registerWithEmail(email, password, name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                enabled = authState !is AuthState.Loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                if (authState is AuthState.Loading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch to Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = Color.White.copy(alpha = 0.7f))
                Text(
                    "Login",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Verification dialog
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = {
                showVerificationDialog = false
                viewModel.resetState()
            },
            containerColor = androidx.compose.ui.graphics.Color(0xFF0D4A4C),
            title = {
                Text(
                    "Verify Your Email",
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "A verification link has been sent to $verificationEmail.\n\nPlease check your inbox (and spam folder) and click the link to activate your account before logging in.",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (verificationEmail.isNotBlank() && verificationPassword.isNotBlank()) {
                            viewModel.sendEmailVerification(verificationEmail, verificationPassword)
                            Toast.makeText(context, "Verification email resent!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Resend Email", color = Gold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVerificationDialog = false
                        viewModel.resetState()
                        onNavigateToLogin()
                    }
                ) {
                    Text("Go to Login", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Auth state observer
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Welcome to Al-Aalim! Let's set up your preferences.", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
            }
            is AuthState.UnverifiedEmail -> {
                // Registration succeeded — user must verify their email before logging in
                verificationEmail = (authState as AuthState.UnverifiedEmail).email
                verificationPassword = password // capture current password for resend
                showVerificationDialog = true
            }
            is AuthState.Error -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Register Screen")
@Composable
fun RegisterScreenPreview() {
    AlAalimTheme {
        RegisterScreen(
            onBackClick = {},
            onNavigateToLogin = {},
            onRegisterSuccess = {}
        )
    }
}

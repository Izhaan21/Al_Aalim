package com.example.al_aalim.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.al_aalim.R
import com.example.al_aalim.viewmodel.AuthState
import com.example.al_aalim.viewmodel.AuthViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.ui.main.ContainerActivity
import com.example.al_aalim.ui.permissions.LocationPermissionActivity
import com.example.al_aalim.ui.auth.ForgotPasswordActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AlAalimTheme {
                LoginScreen(
                    onBackClick = { finish() },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                        finish()
                    },
                    onLoginSuccess = {
                        val intent = Intent(this, LocationPermissionActivity::class.java)
                        intent.putExtra("from_auth", true)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    },
                    onForgotPassword = { prefillEmail ->
                        val intent = Intent(this, ForgotPasswordActivity::class.java)
                        if (prefillEmail.isNotBlank()) {
                            intent.putExtra("prefill_email", prefillEmail)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPassword: (String) -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var isGoogleLoading by remember { mutableStateOf(false) }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        } else {
            isGoogleLoading = false
            viewModel.resetState()
        }
    }

    // imePadding on the outer Box so the whole screen shifts up when keyboard opens
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
            Text("Welcome Back", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "Login to continue your spiritual journey",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
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

            Spacer(modifier = Modifier.height(8.dp))
            
            // Forgot Password — navigates to dedicated screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Forgot Password?",
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onForgotPassword(email) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Task 3: Login Button with Loading Icon
            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
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
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── OR divider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.25f)
                )
                Text(
                    "  OR  ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.25f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    isGoogleLoading = true
                    val intent = viewModel.getGoogleSignInIntent()
                    if (intent != null) {
                        googleSignInLauncher.launch(intent)
                    } else {
                        isGoogleLoading = false
                        Toast.makeText(context, "Google Sign-In unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = authState !is AuthState.Loading && !isGoogleLoading,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            ) {
                if (isGoogleLoading || (authState is AuthState.Loading && isGoogleLoading)) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        "Continue with Google",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Switch to Register
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = Color.White.copy(alpha = 0.7f))
                Text(
                    "Register",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Auth state observer
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                isGoogleLoading = false
                onLoginSuccess()
            }
            is AuthState.UnverifiedEmail -> {
                isGoogleLoading = false
                val state = authState as AuthState.UnverifiedEmail
                Toast.makeText(
                    context,
                    state.message + "\nTap 'Forgot Password?' to resend if needed.",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                isGoogleLoading = false
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

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen")
@Composable
fun LoginScreenPreview() {
    com.example.al_aalim.ui.theme.AlAalimTheme {
        LoginScreen(
            onBackClick = {},
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}

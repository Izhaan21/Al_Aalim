package com.example.al_aalim.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.al_aalim.ui.components.AdaptiveContainer
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.al_aalim.R
import com.example.al_aalim.viewmodel.AuthState
import com.example.al_aalim.viewmodel.AuthViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd

class ForgotPasswordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Pre-fill email passed from LoginActivity, if any
        val prefillEmail = intent.getStringExtra("prefill_email") ?: ""

        setContent {
            AlAalimTheme {
                ForgotPasswordScreen(
                    prefillEmail = prefillEmail,
                    onBackClick = { finish() },
                    onEmailSent = { finish() }
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    prefillEmail: String = "",
    onBackClick: () -> Unit,
    onEmailSent: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    var email by remember { mutableStateOf(prefillEmail) }
    var emailSent by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Observe auth state only for the password-reset result
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                val msg = (authState as AuthState.Error).message
                // FirebaseAuthManager returns a success message disguised as Error when no user is logged in
                if (msg.contains("sent successfully", ignoreCase = true)) {
                    emailSent = true
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                    onEmailSent() // Finish activity immediately
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
            is AuthState.Success -> {
                // Logged-in user case: reset email was sent with a user session
                emailSent = true
                Toast.makeText(
                    context,
                    "Password reset email sent to $email",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
                onEmailSent() // Finish activity immediately
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ComposeBrush.verticalGradient(
                    listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
            .imePadding()
    ) {
        AdaptiveContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {

            // ── Back button ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
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

            Spacer(modifier = Modifier.height(32.dp))

            // ── Icon ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.15f))
                    .border(1.dp, Gold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title ────────────────────────────────────────────────────────
            Text(
                "Forgot Password?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter your email address and we'll send you a link to reset your password.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (!emailSent) {
                // ── Email field ──────────────────────────────────────────────
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Gold
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Send button ──────────────────────────────────────────────
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter your email address",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.sendPasswordReset(email.trim())
                        }
                    },
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Send Reset Link",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

            } else {
                // ── Success state ────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF66BB6A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color(0xFF66BB6A), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Email Sent!",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "A password reset link has been sent to\n$email\n\nCheck your inbox (and spam folder).",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onEmailSent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                brush = ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            "Back to Login",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

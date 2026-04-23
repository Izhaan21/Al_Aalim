package com.example.al_aalim.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.al_aalim.R

// ─────────────────── Shared visual constants ───────────────────────────────

private val GlassBackground = Color(0xFF0A3D3F)
private val GlassBorder     = Color(0x33FFFFFF)
private val Gold            = Color(0xFFD4A843)
private val TextWhite       = Color(0xFFFFFFFF)
private val TextMuted       = Color(0xAAFFFFFF)
private val DialogShape     = RoundedCornerShape(20.dp)

private val GlassBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1B6B6E), Color(0xFF0A3D3F))
)

/**
 * Glass-style dialog shell used by all dialog composables below.
 */
@Composable
private fun GlassDialogShell(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(DialogShape)
                .background(GlassBrush)
                .border(1.dp, GlassBorder, DialogShape)
                .padding(24.dp)
                .animateContentSize(),
            content = content
        )
    }
}

// ─────────────────── Confirmation Dialog ───────────────────────────────────

/**
 * Compose replacement for CustomDialog.showConfirmation()
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    iconRes: Int? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogShell(onDismiss = onDismiss) {
        // Optional icon
        iconRes?.let { res ->
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = Gold,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = title,
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(cancelText, color = TextWhite, fontSize = 14.sp)
            }

            // Confirm button (gold)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Gold)
                    .clickable { onConfirm(); onDismiss() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────── Permission Dialog ─────────────────────────────────────

/**
 * Dialog for requesting permissions or showing permission rationale.
 */
@Composable
fun PermissionDialog(
    title: String,
    message: String,
    iconRes: Int? = null,
    confirmText: String = "Allow",
    cancelText: String = "Skip",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogShell(onDismiss = onDismiss) {
        iconRes?.let {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FFFFFF))
                    .border(1.dp, Gold.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = title,
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onCancel() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(cancelText, color = TextWhite, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Gold)
                    .clickable { onConfirm() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Compose replacement for CustomDialog.showSelectionList()
 */
@Composable
fun SelectionListDialog(
    title: String,
    subtitle: String? = null,
    iconRes: Int? = null,
    options: List<String>,
    optionIconRes: List<Int>? = null,
    selectedIndex: Int = -1,
    autoApply: Boolean = false,
    showRadio: Boolean = true,
    onSelect: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedIndex) }

    GlassDialogShell(onDismiss = onDismiss) {
        // Icon
        iconRes?.let { res ->
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = Gold,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = title,
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Items list (max 5 visible before scroll)
        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
            itemsIndexed(options) { index, option ->
                val isSelected = index == currentSelection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0x22D4A843) else Color.Transparent)
                        .clickable {
                            currentSelection = index
                            if (autoApply) {
                                onSelect(index, option)
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Optional per-item icon
                    optionIconRes?.getOrNull(index)?.let { res ->
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Text(
                        text = option,
                        color = if (isSelected) Gold else TextWhite,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (showRadio) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Gold else GlassBorder,
                                    shape = CircleShape
                                )
                                .background(if (isSelected) Gold else Color.Transparent)
                        )
                    }
                }

                if (index < options.lastIndex) {
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                }
            }
        }

        // Show Apply button only when not auto-applying
        if (!autoApply) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextWhite, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gold)
                        .clickable {
                            if (currentSelection >= 0) {
                                onSelect(currentSelection, options[currentSelection])
                            }
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────── About Dialog ──────────────────────────────────────────

@Composable
fun AboutDialog(
    appName: String = "Al-Aalim",
    version: String = "1.0.0",
    description: String = "Your comprehensive Islamic companion app.",
    onDismiss: () -> Unit
) {
    GlassDialogShell(onDismiss = onDismiss) {
        // App icon placeholder with gold crescent
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x11FFFFFF))
                .border(1.dp, Gold.copy(alpha = 0.3f), CircleShape)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = appName,
            color = Gold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Version $version",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = description,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Gold)
                .clickable { onDismiss() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ─────────────────── Support Us Dialog ─────────────────────────────────────

@Composable
fun SupportUsDialog(
    onTierSelected: (tier: String, price: String) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogShell(onDismiss = onDismiss) {
        Text(
            text = "Support Al-Aalim",
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Your support helps us keep this app free and growing",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // Tier buttons
        listOf(
            Triple("coffee",    "☕ Buy us a Coffee",  "$1.99"),
            Triple("supporter", "🌙 Become a Supporter","$4.99"),
            Triple("champion",  "⭐ Champion of the Deen","$9.99"),
        ).forEach { (tier, label, price) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onTierSelected(tier, price); onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(price, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .clickable { onDismiss() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Maybe Later", color = TextMuted, fontSize = 14.sp)
        }
    }
}

// ─────────────────── Glass Text Field (Internal Use) ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (isPassword) androidx.compose.ui.text.input.KeyboardType.Password else androidx.compose.ui.text.input.KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = Color(0x11FFFFFF),
            unfocusedContainerColor = Color(0x11FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

// ─────────────────── Change Email Dialog ───────────────────────────────────

@Composable
fun ChangeEmailDialog(
    isLoading: Boolean,
    isGoogleUser: Boolean = false,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }

    GlassDialogShell(onDismiss = onDismiss) {
        Text(
            text = "Change Email",
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        if (!isGoogleUser) {
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                label = "Current Password",
                isPassword = true
            )

            Spacer(Modifier.height(12.dp))
        }

        GlassTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            label = "New Email",
            isPassword = false
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = !isLoading) { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextWhite, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLoading) Color.Gray else Gold)
                    .clickable(enabled = !isLoading) {
                        onConfirm(if (isGoogleUser) "" else password, newEmail)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Change", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────── Change Password Dialog ────────────────────────────────

@Composable
fun ChangePasswordDialog(
    isLoading: Boolean,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    GlassDialogShell(onDismiss = onDismiss) {
        Text(
            text = "Change Password",
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        GlassTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = "Current Password",
            isPassword = true
        )
        Spacer(Modifier.height(12.dp))
        GlassTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "New Password",
            isPassword = true
        )
        Spacer(Modifier.height(12.dp))
        GlassTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            isPassword = true
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = !isLoading) { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextWhite, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLoading) Color.Gray else Gold)
                    .clickable(enabled = !isLoading) {
                        onConfirm(currentPassword, newPassword, confirmPassword)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Change", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────── Edit Profile Dialog ───────────────────────────────────

@Composable
fun EditProfileDialog(
    initialName: String,
    profileImageBitmap: android.graphics.Bitmap?,
    initials: String,
    onSave: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    GlassDialogShell(onDismiss = onDismiss) {
        Text(
            text = "Edit Profile",
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        // Profile Photo section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D3D3F))
                    .border(2.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = profileImageBitmap.asImageBitmap(),
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = initials, color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Change Photo",
                color = Gold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onChangePhoto() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        GlassTextField(
            value = name,
            onValueChange = { name = it },
            label = "Display Name"
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextWhite, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Gold)
                    .clickable { onSave(name) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}


// ------------------- Confirm With Password Dialog --------------------------

/**
 * Prompts the user to re-enter their password to confirm a destructive action.
 */
@Composable
fun ConfirmWithPasswordDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    isLoading: Boolean = false,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    GlassDialogShell(onDismiss = onDismiss) {
        Text(
            text = title,
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        GlassTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = !isLoading) { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextWhite, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLoading || password.isBlank()) Color.Gray else Color(0xFFEF5350))
                    .clickable(enabled = !isLoading && password.isNotBlank()) {
                        onConfirm(password)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

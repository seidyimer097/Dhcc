package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    currentUser: UserEntity?,
    onUpdateProfile: (name: String, avatarUrl: String, callback: (Boolean, String) -> Unit) -> Unit,
    onUploadToCloudinary: (filename: String, onUrl: (String) -> Unit) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayName by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var avatarUrl by remember(currentUser) { mutableStateOf(currentUser?.avatarUrl ?: "") }
    var customImageFilename by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val sampleAvatars = remember {
        listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_profile_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("button_profile_back")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (currentUser != null) {
                    Badge(
                        containerColor = when (currentUser.role.lowercase()) {
                            "teacher" -> PrimaryBlue
                            "admin" -> AccentViolet
                            else -> SecondaryTeal
                        },
                        contentColor = Color.White
                    ) {
                        Text(
                            text = currentUser.role.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Picture Section
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_profile_picture_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profile Picture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Avatar Circle Display
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("img_profile_avatar"),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.take(1).uppercase().ifBlank { "U" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (isUploading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cloudinary Upload Controls
                    Text(
                        text = "Upload Picture to Cloudinary",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customImageFilename,
                            onValueChange = { customImageFilename = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_cloudinary_filename"),
                            placeholder = { Text("e.g. avatar_profile_pic.png") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val nameToUpload = customImageFilename.ifBlank { "user_avatar_${System.currentTimeMillis()}.png" }
                                isUploading = true
                                onUploadToCloudinary(nameToUpload) { url ->
                                    isUploading = false
                                    avatarUrl = url
                                    statusMessage = "Image uploaded to Cloudinary!"
                                    isSuccess = true
                                }
                            },
                            enabled = !isUploading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("button_upload_cloudinary")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = "Upload",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }

                    if (avatarUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cloudinary URL: $avatarUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("text_cloudinary_url")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Sample Avatars Picker
                    Text(
                        text = "Or pick a sample avatar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleAvatars) { url ->
                            Surface(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .clickable { avatarUrl = url }
                                    .border(
                                        width = if (avatarUrl == url) 3.dp else 1.dp,
                                        color = if (avatarUrl == url) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Sample Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Form Section
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_user_info_form")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Profile Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Display Name Input
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_name"),
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email Address (Disabled / Read-only)
                    OutlinedTextField(
                        value = currentUser?.email ?: "No email registered",
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_email"),
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Status Message Banner
                    AnimatedVisibility(visible = statusMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Save Profile Button
                    Button(
                        onClick = {
                            if (displayName.isBlank()) {
                                statusMessage = "Display name cannot be blank."
                                isSuccess = false
                                return@Button
                            }
                            isSaving = true
                            statusMessage = null
                            onUpdateProfile(displayName, avatarUrl) { success, msg ->
                                isSaving = false
                                isSuccess = success
                                statusMessage = msg
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_save_profile"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving Changes...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

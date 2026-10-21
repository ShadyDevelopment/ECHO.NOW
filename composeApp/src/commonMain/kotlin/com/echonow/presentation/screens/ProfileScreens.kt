package com.echonow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.echonow.domain.repository.ProfileAvatars
import com.echonow.domain.repository.ProfileRepository
import com.echonow.domain.repository.ProfileState
import com.echonow.domain.repository.UserProfile
import com.echonow.presentation.components.EchoButton
import com.echonow.presentation.components.EchoOutlinedButton
import com.echonow.presentation.components.EchoTextButton
import com.echonow.presentation.components.LocalEchoImageLoader
import com.echonow.theme.LocalButtonCustomization
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject

@Composable
fun ProfileAvatar(
    avatarId: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val borderColor = if (selected) {
        LocalButtonCustomization.current.borderColor ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = .55f)
    }
    Box(
        modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (selected) 2.dp else 1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = ProfileAvatars.imageUrl(avatarId)
        if (imageUrl != null) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            AsyncImage(
                model = imageUrl,
                imageLoader = LocalEchoImageLoader.current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(legacyAvatarEmoji(avatarId), fontSize = 23.sp)
        }
    }
}

@Composable
fun ProfileSetupScreen() {
    val profiles: ProfileRepository = koinInject()
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        ProfileEditorForm(
            title = "Who's watching?",
            description = "Create a profile to keep favorites and viewing progress separate.",
            submitLabel = "OK",
            onSave = { nickname, avatarId -> profiles.create(nickname, avatarId) },
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(20.dp),
        )
    }
}

@Composable
private fun ProfileEditorForm(
    title: String,
    description: String,
    initialNickname: String = "",
    initialAvatarId: String = ProfileAvatars.defaultId,
    submitLabel: String,
    onCancel: (() -> Unit)? = null,
    onSave: suspend (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nickname by remember(initialNickname) { mutableStateOf(initialNickname) }
    var avatarId by remember(initialAvatarId) { mutableStateOf(initialAvatarId) }
    var avatarPickerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val buttonStyle = LocalButtonCustomization.current

    Surface(
        modifier = modifier,
        shape = LocalButtonCustomization.current.shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            buttonStyle.borderColor?.copy(alpha = .48f) ?: MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 390.dp) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AvatarPreviewButton(avatarId, onClick = { avatarPickerOpen = true })
                        NicknameField(nickname, onNicknameChange = {
                            nickname = it.take(24)
                            errorMessage = null
                        })
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarPreviewButton(avatarId, onClick = { avatarPickerOpen = true })
                        NicknameField(
                            nickname,
                            modifier = Modifier.weight(1f),
                            onNicknameChange = {
                                nickname = it.take(24)
                                errorMessage = null
                            },
                        )
                    }
                }
            }
            Text(
                "Tap the profile picture to choose another avatar.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onCancel != null) {
                    EchoTextButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
                }
                EchoButton(
                    enabled = !saving && nickname.isNotBlank(),
                    onClick = {
                        saving = true
                        errorMessage = null
                        scope.launch {
                            runCatching { onSave(nickname.trim(), avatarId) }
                                .onFailure { errorMessage = it.message ?: "Could not save this profile." }
                            saving = false
                        }
                    },
                ) {
                    Text(if (saving) "Saving…" else submitLabel)
                }
            }
        }
    }

    if (avatarPickerOpen) {
        AvatarPickerDialog(
            selectedAvatarId = avatarId,
            onSelect = {
                avatarId = it
                avatarPickerOpen = false
            },
            onDismiss = { avatarPickerOpen = false },
        )
    }
}

@Composable
private fun NicknameField(
    nickname: String,
    modifier: Modifier = Modifier,
    onNicknameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        label = { Text("Profile name") },
        placeholder = { Text("Enter a nickname") },
        singleLine = true,
        supportingText = { Text("${nickname.length}/24") },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun AvatarPreviewButton(avatarId: String, onClick: () -> Unit) {
    val buttonStyle = LocalButtonCustomization.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(104.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    3.dp,
                    buttonStyle.borderColor ?: MaterialTheme.colorScheme.primary,
                    CircleShape,
                )
                .clickable(onClick = onClick)
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            ProfileAvatar(avatarId, Modifier.size(96.dp), selected = true)
        }
        Text("Change picture", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AvatarPickerDialog(
    selectedAvatarId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedAvatarParts = selectedAvatarId.split(':')
    val initialCategory = selectedAvatarParts.getOrNull(1)
        ?.takeIf { it in ProfileAvatars.categories }
        ?: ProfileAvatars.categories.first()
    val initialIndex = selectedAvatarParts.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 999) ?: 0
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var visibleCount by remember(selectedCategory) {
        mutableIntStateOf(if (selectedCategory == initialCategory) maxOf(48, initialIndex + 1) else 48)
    }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()
                    .heightIn(max = (availableHeight - 24.dp).coerceAtLeast(240.dp)),
                shape = LocalButtonCustomization.current.shape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
                shadowElevation = 20.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    LocalButtonCustomization.current.borderColor?.copy(alpha = .42f)
                        ?: MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Choose a profile picture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Pick an image for your profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileAvatars.categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = (LocalButtonCustomization.current.fillColor
                                        ?: MaterialTheme.colorScheme.primary).copy(alpha = .22f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedLeadingIconColor = LocalButtonCustomization.current.textColor
                                        ?: MaterialTheme.colorScheme.primary,
                                ),
                                label = { Text(category.toDisplayName()) },
                            )
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 68.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxWidth().heightIn(
                            min = 120.dp,
                            max = (availableHeight - 220.dp).coerceIn(120.dp, 360.dp),
                        ),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visibleCount) { index ->
                            val avatarId = ProfileAvatars.id(selectedCategory, index)
                            val targetIndex = if (selectedCategory == initialCategory) initialIndex else 0
                            val focusedModifier = if (index == targetIndex) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            }
                            Box(
                                focusedModifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        if (avatarId == selectedAvatarId) 3.dp else 1.dp,
                                        if (avatarId == selectedAvatarId) {
                                            LocalButtonCustomization.current.borderColor
                                                ?: MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = .5f)
                                        },
                                        CircleShape,
                                    )
                                    .clickable { onSelect(avatarId) }
                                    .focusable(),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = ProfileAvatars.imageUrl(avatarId),
                                    imageLoader = LocalEchoImageLoader.current,
                                    contentDescription = "${selectedCategory.toDisplayName()} avatar ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().padding(3.dp).clip(CircleShape),
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Images from piccdn · ${visibleCount.coerceAtMost(1000)} of 1,000",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EchoTextButton(onClick = onDismiss) { Text("Cancel") }
                            EchoButton(onClick = onDismiss) { Text("Done") }
                        }
                    }
                    LaunchedEffect(selectedCategory) {
                        val targetIndex = if (selectedCategory == initialCategory) initialIndex else 0
                        gridState.scrollToItem(targetIndex.coerceAtMost(visibleCount - 1))
                        focusRequester.requestFocus()
                    }
                    LaunchedEffect(selectedCategory, visibleCount) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        }.distinctUntilChanged().collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null &&
                                lastVisibleIndex >= visibleCount - 8 &&
                                visibleCount < 1_000
                            ) {
                                visibleCount = (visibleCount + 48).coerceAtMost(1_000)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSwitcherDialog(onDismiss: () -> Unit) {
    val profiles: ProfileRepository = koinInject()
    val state by profiles.observe().collectAsState(initial = ProfileState())
    var selectedId by remember(state.activeProfileId) { mutableStateOf(state.activeProfileId) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmRemoval by remember { mutableStateOf<UserProfile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selected = state.profiles.firstOrNull { it.id == selectedId }
    val profileFocusRequester = remember { FocusRequester() }
    val buttonStyle = LocalButtonCustomization.current
    val profileAccent = buttonStyle.fillColor ?: MaterialTheme.colorScheme.primary
    LaunchedEffect(state.activeProfileId) {
        if (state.activeProfileId != null) profileFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = buttonStyle.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Who's watching?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    state.profiles.forEach { profile ->
                        Column(
                            Modifier.width(82.dp).clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (profile.id == selectedId) profileAccent.copy(alpha = .16f)
                                    else Color.Transparent,
                                )
                                .border(
                                    if (profile.id == selectedId) 1.dp else 0.dp,
                                    if (profile.id == selectedId) {
                                        buttonStyle.borderColor ?: profileAccent
                                    } else {
                                        Color.Transparent
                                    },
                                    buttonStyle.shape,
                                )
                                .then(
                                    if (profile.id == state.activeProfileId) Modifier.focusRequester(profileFocusRequester)
                                    else Modifier,
                                )
                                .clickable {
                                    selectedId = profile.id
                                    scope.launch {
                                        runCatching { profiles.select(profile.id) }
                                            .onFailure { errorMessage = it.message ?: "Could not switch profiles." }
                                    }
                                }
                                .focusable()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                ProfileAvatar(profile.avatarId, selected = profile.id == state.activeProfileId)
                                if (profile.id == state.activeProfileId) {
                                    Box(
                                        Modifier.size(18.dp).clip(CircleShape)
                                            .background(profileAccent),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Active profile",
                                            tint = buttonStyle.textColor ?: MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                profile.nickname,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    if (state.profiles.size < ProfileAvatars.MAX_PROFILES) {
                        Column(
                            Modifier.width(82.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { creating = true }
                                .focusable()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                            }
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                selected?.let { profile ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EchoOutlinedButton(onClick = { editingProfile = profile }) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Text(" Edit")
                        }
                        EchoTextButton(onClick = { confirmRemoval = profile }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                            Text(" Remove")
                        }
                    }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { EchoButton(onClick = onDismiss) { Text("Done") } },
    )

    if (creating) {
        ProfileEditorDialog(
            title = "Create profile",
            description = "Give this profile a name and picture.",
            submitLabel = "OK",
            onDismiss = { creating = false },
            onSave = { nickname, avatarId ->
                profiles.create(nickname, avatarId)
                creating = false
            },
        )
    }
    editingProfile?.let { profile ->
        ProfileEditorDialog(
            title = "Edit profile",
            description = "Update this profile's name or picture.",
            initialNickname = profile.nickname,
            initialAvatarId = profile.avatarId,
            submitLabel = "OK",
            onDismiss = { editingProfile = null },
            onSave = { nickname, avatarId ->
                profiles.update(profile.copy(nickname = nickname, avatarId = avatarId))
                editingProfile = null
            },
        )
    }
    confirmRemoval?.let { profile ->
        AlertDialog(
            onDismissRequest = { confirmRemoval = null },
            shape = buttonStyle.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Remove ${profile.nickname}?") },
            text = { Text("This removes this profile and its saved settings, favorites, and watch progress from this device.") },
            confirmButton = {
                EchoButton(
                    onClick = {
                        scope.launch {
                            runCatching { profiles.remove(profile.id) }
                                .onFailure { errorMessage = it.message ?: "Could not remove this profile." }
                            confirmRemoval = null
                        }
                    },
                ) { Text("Remove") }
            },
            dismissButton = { EchoTextButton(onClick = { confirmRemoval = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    title: String,
    description: String,
    initialNickname: String = "",
    initialAvatarId: String = ProfileAvatars.defaultId,
    submitLabel: String,
    onDismiss: () -> Unit,
    onSave: suspend (String, String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProfileEditorForm(
                title = title,
                description = description,
                initialNickname = initialNickname,
                initialAvatarId = initialAvatarId,
                submitLabel = submitLabel,
                onCancel = onDismiss,
                onSave = onSave,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight),
            )
        }
    }
}

private fun String.toDisplayName(): String =
    replace('-', ' ').replaceFirstChar { it.uppercase() }

private fun legacyAvatarEmoji(avatarId: String): String = when (avatarId) {
    "film" -> "🎬"
    "rocket" -> "🚀"
    "star" -> "🌟"
    "panda" -> "🐼"
    "fox" -> "🦊"
    "frog" -> "🐸"
    "music" -> "🎧"
    "ball" -> "🏀"
    else -> "👤"
}

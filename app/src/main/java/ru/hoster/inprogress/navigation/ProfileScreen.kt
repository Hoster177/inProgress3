package ru.hoster.inprogress.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import ru.hoster.inprogress.features.profile.ProfileScreenUiState
import ru.hoster.inprogress.features.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToAchievements: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToUserSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiState.collectLatest { state ->
            if (state.saveSuccessMessage != null) {
                snackbarHostState.showSnackbar(
                    message = state.saveSuccessMessage,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSaveSuccessMessage()
            }
            if (state.error != null) {
                snackbarHostState.showSnackbar(
                    message = "Ошибка: ${state.error}",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToUserSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Общие настройки"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                AvatarSection(
                    selectedAvatarId = uiState.selectedAvatarId,
                    predefinedAvatars = viewModel.predefinedAvatars,
                    onAvatarSelected = viewModel::onAvatarSelected
                )
                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = uiState.editableName,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Имя пользователя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email: ${uiState.email}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::saveProfileChanges,
                    enabled = !uiState.isSaving && (uiState.editableName != uiState.currentName || uiState.selectedAvatarId != uiState.currentAvatarId)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Сохранить изменения")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))


                Text("Дополнительная информация", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToAchievements, modifier = Modifier.fillMaxWidth()) { Text("Достижения") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateToHelp, modifier = Modifier.fillMaxWidth()) { Text("Справка (FAQ)") }
            }
        }
    }
}

@Composable
fun AvatarSection(
    selectedAvatarId: String,
    predefinedAvatars: List<String>,
    onAvatarSelected: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        AvatarImage(
            avatarId = selectedAvatarId,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Выберите аватар:", style = MaterialTheme.typography.titleMedium)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            items(predefinedAvatars) { avatarId ->
                AvatarImage(
                    avatarId = avatarId,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { onAvatarSelected(avatarId) }
                        .border(
                            width = 2.dp,
                            color = if (avatarId == selectedAvatarId) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun AvatarImage(avatarId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resourceId = remember(avatarId) {
        context.resources.getIdentifier(avatarId, "drawable", context.packageName)
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "Аватар $avatarId",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarId.firstOrNull()?.uppercase() ?: "A", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        val previewState = ProfileScreenUiState(
            currentName = "Тестовый Пользователь",
            editableName = "Тестовый Пользователь",
            email = "test@example.com",
            currentAvatarId = "avatar_1",
            selectedAvatarId = "avatar_1"
        )
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Профиль") },
                    actions = {
                        IconButton(onClick = { /*  */ }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Общие настройки"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarSection(
                    selectedAvatarId = previewState.selectedAvatarId,
                    predefinedAvatars = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_default"),
                    onAvatarSelected = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = previewState.editableName,
                    onValueChange = {},
                    label = { Text("Имя пользователя") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email: ${previewState.email}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {}) { Text("Сохранить изменения") }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Дополнительная информация", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Достижения") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Справка (FAQ)") }
            }
        }
    }
}
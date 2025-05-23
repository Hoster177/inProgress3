package ru.hoster.inprogress.navigation.addeditactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController // For preview

// Predefined colors list - this should be accessible, e.g., from a common constants file
// or passed to ColorSelector. For this example, assume it's defined globally or imported.
val predefinedColorsHex = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#FED766", "#2AB7CA",
    "#F0B67F", "#8A9B0F", "#C34A36", "#7F7EFF", "#FF96AD"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityScreen(
    navController: NavController,
    // activityId is handled by ViewModel's SavedStateHandle
    viewModel: AddEditActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            navController.popBackStack()
            viewModel.onSaveCompletedHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveActivity() }) {
                Icon(Icons.Filled.Save, contentDescription = "Сохранить занятие")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading && !uiState.initialActivityLoaded && uiState.isEditing) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
                Text("Загрузка данных занятия...")
            } else if (uiState.isLoading) { // General loading for save or other operations
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
                Text("Сохранение...")
            } else {
                OutlinedTextField(
                    value = uiState.activityName,
                    onValueChange = { viewModel.setActivityName(it) },
                    label = { Text("Название занятия") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null && uiState.error!!.contains("Название") // More specific error check
                )
                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Выберите цвет:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ColorSelector( // ColorSelector and ColorDot remain the same as before
                    colors = predefinedColorsHex,
                    selectedColorHex = uiState.selectedColorHex,
                    onColorSelected = { viewModel.setSelectedColor(it) }
                )
            }
        }
    }
}

// ColorSelector and ColorDot composables (as defined previously in AddEditActivityScreen.kt)
// Ensure they are present or imported.
@Composable
fun ColorSelector(
    colors: List<String>,
    selectedColorHex: String?,
    onColorSelected: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.take(5).forEach { colorHex ->
                ColorDot(
                    colorHex = colorHex,
                    isSelected = colorHex == selectedColorHex,
                    onClick = { onColorSelected(colorHex) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (colors.size > 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.drop(5).take(5).forEach { colorHex ->
                    ColorDot(
                        colorHex = colorHex,
                        isSelected = colorHex == selectedColorHex,
                        onClick = { onColorSelected(colorHex) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorDot(colorHex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}


@Preview(showBackground = true, name = "Add New Activity (Hilt VM)")
@Composable
fun AddActivityScreenHiltPreview() {
    MaterialTheme {
        // For Hilt previews, you often need a more complex setup or to pass
        // a manually created ViewModel with fakes if @Preview doesn't work directly
        // with @HiltViewModel. For now, this might not render correctly without Hilt context.
        // A common pattern is to have a Content composable that takes state and lambdas.
        AddEditActivityScreen(
            navController = rememberNavController()
            // ViewModel is obtained via hiltViewModel()
        )
    }
}

@Preview(showBackground = true, name = "Edit Existing Activity (Hilt VM)")
@Composable
fun EditActivityScreenHiltPreview() {
    MaterialTheme {
        // To preview edit mode with Hilt, you'd need to ensure SavedStateHandle
        // can be pre-populated for the preview, or use a test harness.
        // This will likely show the "Add" mode unless SavedStateHandle is mocked.
        AddEditActivityScreen(
            navController = rememberNavController()
            // ViewModel is obtained via hiltViewModel(); it will try to get "activityId"
        )
    }
}
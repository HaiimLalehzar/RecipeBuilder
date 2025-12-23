package com.example.recipebuilder.ui.screens.create

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.recipebuilder.mvi.createRecipe.CreateRecipeViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineStage
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.mvi.createRecipe.CreateRecipeAction
import com.example.recipebuilder.ui.theme.PureWhite
import com.example.recipebuilder.ui.theme.SkyBlueMedium
import com.example.recipebuilder.viewModel.CreateRecipeUIState
import com.example.recipebuilder.viewModel.RecipeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.emptyList

@Composable
fun CreateRecipeScreen(
    mainViewModel: RecipeViewModel,
    onBack: () -> Unit = {},
    onRecipeSaved: () -> Unit = {}
) {
    val createViewModel = remember { CreateRecipeViewModel(mainViewModel) }
    var activeTab by remember { mutableStateOf(CreateRecipeTab.Manual) }
    val uploadStatus by mainViewModel.uploadStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uploadStatus) {
        if (uploadStatus.isNotBlank()) {
            snackbarHostState.showSnackbar(uploadStatus)
        }
    }




    Scaffold(
        contentColor = PureWhite,
        snackbarHost = {
            SnackbarHost(
                hostState =  snackbarHostState
            )
        },
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .offset(y = 10.dp)
                        .background(PureWhite),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Recipe", color = Black, fontSize = 25.sp, modifier = Modifier.weight(4f))

                    IconButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CreateRecipeTab.entries.forEach { tab ->
                    val isActive = (tab == activeTab)
                    Surface(
                        color = if (isActive) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = tab }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentColor = MaterialTheme.colorScheme.onBackground,
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            ) {
                when (activeTab) {
                    CreateRecipeTab.TikTok -> TikTokUploadSection(createViewModel)
                    CreateRecipeTab.Upload -> UploadSection(createViewModel, mainViewModel)
                    CreateRecipeTab.Record -> RecordSection(createViewModel)
                    CreateRecipeTab.Manual -> ManualSection(createViewModel, onRecipeSaved)
                    CreateRecipeTab.Spoonacular -> SpoonacularSection(createViewModel, mainViewModel)
                }
            }
        }
    }
}
enum class CreateRecipeTab(val title: String) {
    TikTok("TikTok"),
    Upload("Upload"),
    Record("Record"),
    Manual("Manual"),
    Spoonacular("Generator");
}

@Composable
fun SpoonacularSection(viewModel: CreateRecipeViewModel, recipeViewModel: RecipeViewModel) {
    val pullToRefreshState = rememberPullToRefreshState()
    val state by viewModel.state.collectAsState()

    Column(Modifier.padding(16.dp)) {

        Button(
            onClick = { viewModel.handleAction(CreateRecipeAction.LoadRandomRecipe) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Random Recipe")
        }
            PullToRefreshBox(
                state = pullToRefreshState,
                onRefresh = { viewModel.handleAction(CreateRecipeAction.RefreshRecipes) },
                isRefreshing = state.isLoading,

                ) {
                when (state.discovery.apiState) {
                    is CreateRecipeUIState.Success -> {
                        val recipes = state.discovery.generatedRecipes
                        if (recipes.isNotEmpty()) {
                            LazyColumn {
                                items(recipes.size) { index ->
                                    CollapsableCard(
                                        title = recipes[index].title,
                                        summary = recipes[index].summary,
                                        viewModel = recipeViewModel,
                                        ingredient = recipes[index].extendedIngredients.map { it.name },
                                        instructions = recipes[index].analyzedInstructions[0].steps.map { it.step }


                                    )

                                }
                            }
                        } else Text("No recipes found.")
                    }
                    is CreateRecipeUIState.Error -> Text("Failed to load recipes. Something is wrong with your internet connection.")
                    is CreateRecipeUIState.Loading -> Text("Loading recipes...")
                        CreateRecipeUIState.Empty -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                        ) {
                            Text("Click Get Random Recipes to get started!")
                        }

                }
            }
         }
    }


@Composable
fun CollapsableCard(title: String, summary: String, viewModel: RecipeViewModel, ingredient: List<String>, instructions: List<String>) {
    var isCollapsed by remember { mutableStateOf(true) }
    Card(
        elevation = elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(PureWhite),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(true, onClick = { isCollapsed = !isCollapsed })

    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                    .clickable(
                        onClick = { isCollapsed = !isCollapsed }
                    )
                    .weight(5f))
                IconButton(
                    onClick = {
                        val recipe = Recipe(
                            name = title,
                            ingredients = ingredient,
                            instructions = instructions)
                        viewModel.addRecipe(recipe)

                    },
                    modifier = Modifier
                        .size(24.dp)
                        .weight(1f)

                ){
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)

                }
            }


            AnimatedVisibility(visible = !isCollapsed) {
                Text(text = summary)

            }
        }

    }
}



@Composable
fun TikTokUploadSection(viewModel: CreateRecipeViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = state.form.tiktokUrl,
            onValueChange = { viewModel.handleAction(CreateRecipeAction.UpdateTikTokUrl(it)) },
            label = {Text("TikTok video URL")} ,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { /*TODO Implement Importing Video */ }) {
            Text("Import Audio")
        }
    }
}

@Composable
fun UploadSection(
    mainViewModel: CreateRecipeViewModel,
    recipeViewModel: RecipeViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // PIPELINE OUTPUTS
    val rawText by recipeViewModel.rawTranscriptionText.collectAsState()
    val sentences by recipeViewModel.completedSentences.collectAsState(emptyList())
    val instructions by recipeViewModel.finalInstructions.collectAsState()
    val isTranscribing by recipeViewModel.pipelineStage.collectAsState()

    LaunchedEffect(instructions) {
        Log.d("UI", "INSTRUCTIONS UPDATED: count=${instructions.size}")
        instructions.forEachIndexed { i, inst ->
            Log.d("UI", "Instruction[$i] -> ${inst.stepTextRaw}")
        }
    }

    // File picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
            val temp = File(context.cacheDir, "audio_input.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            recipeViewModel.transcribeVideoAudio(temp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // ← MAIN FIX
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Pick file button
        Button(
            onClick = { launcher.launch("video/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Pick Video/Audio")
        }

        // Pipeline status indicator
        if (isTranscribing == PipelineStage.TRANSCRIBING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
                Text("Processing audio and parsing recipe…")
            }
        }

        HorizontalDivider()

        // ---------- RAW WHISPER OUTPUT ----------
        Text("Raw Whisper Output:", style = MaterialTheme.typography.titleMedium)

        val rawDisplay = when {
            rawText.isNotBlank() -> rawText
            isTranscribing == PipelineStage.TRANSCRIBING -> "Listening / transcribing…"
            else -> "(Tap \"Pick Video/Audio\" to start)"
        }

        ScrollableBox(rawDisplay)

        // ---------- DETECTED SENTENCES ----------
        if (sentences.isNotEmpty()) {
            Text("Detected Sentences:", style = MaterialTheme.typography.titleMedium)
            ScrollableBox(sentences.joinToString("\n"))
        }

        // ---------- PARSED RECIPE STEPS ----------
        if (instructions.isNotEmpty()) {
            Text("Parsed Recipe Steps:", style = MaterialTheme.typography.titleMedium)

            ScrollableBox(
                instructions.joinToString("\n\n") { inst ->
                    buildString {
                        append("STEP: ${inst.stepTextRaw}\n")
                        if (inst.preConditionsRaw.isNotEmpty())
                            append("PRE: ${inst.preConditionsRaw.joinToString()}\n")
                        if (inst.postConditionsRaw.isNotEmpty())
                            append("POST: ${inst.postConditionsRaw.joinToString()}\n")
                        if (inst.purposeRaw.isNotEmpty())
                            append("PURPOSE: ${inst.purposeRaw.joinToString()}\n")
                        if (inst.followupStepRaw != null)
                            append("FOLLOW-UP: ${inst.followupStepRaw}\n")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
private fun ScrollableBox(text: String) {
    val scroll = rememberScrollState()

    LaunchedEffect(text) {
        // auto-scroll to bottom when text changes
        scroll.animateScrollTo(scroll.maxValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scroll)
        ) {
            Text(text)
        }
    }
}







@Composable
fun RecordSection(viewModel: CreateRecipeViewModel) {
    //TODO Be able to record an audio file
    Column(Modifier.padding(16.dp)) {
        Button(onClick = { }) { Text("Record Audio") }
    }
}

@Composable
fun ManualSection(
    viewModel: CreateRecipeViewModel,
    onRecipeSaved: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize()){


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),

            ) {

            item {
                Section(viewModel, "Recipe Name")
            }
            item {
                IngridentSection(viewModel)


//                Text("Ingredients", style = MaterialTheme.typography.headlineMedium)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    TextField(
//                        value = viewModel.currentIngredient,
//                        onValueChange = { viewModel.updateCurrentIngredient(it) },
//                        label = { Text("Add ingredient") },
//                        modifier = Modifier.weight(1f).border(2.dp, SkyBlueMedium),
//                        colors = TextFieldDefaults.colors(
//                            focusedContainerColor = PureWhite,
//                            unfocusedContainerColor = PureWhite
//                        )
//                    )
//                    Button(onClick = viewModel::addIngredient) {
//                        Text("Add")
//                    }
//                }

                if (state.form.ingredients.isNotEmpty()) {

                    AnimatedVisibility(
                        visible = state.form.ingredients.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { }
                    Card(
                        elevation = elevatedCardElevation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.form.ingredients.forEachIndexed { index, ingredient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = ingredient,
                                        onValueChange = { newValue -> viewModel.handleAction(CreateRecipeAction.UpdateCurrentIngredient(newValue))},
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(fontSize = 14.sp)

                                    )
                                    IconButton(onClick = { viewModel.handleAction(CreateRecipeAction.RemoveIngredient(index))}) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null
                                        )

                                    }

                                }
                            }
                        }


                    }
                }
            }
            item {


                Text("Instructions", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = state.form.currentInstructionInput,
                        onValueChange = { viewModel.handleAction(CreateRecipeAction.UpdateCurrentInstruction(it)) },
                        label = { Text("Add instruction") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, SkyBlueMedium)
                    )
                    Button(onClick = { viewModel.handleAction(CreateRecipeAction.AddInstruction) }) {
                        Text("Add")
                    }
                }

                if (state.form.instructions.isNotEmpty()) {
                    Card(
                        elevation = elevatedCardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.form.instructions.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                    )
                                    TextField(
                                        value = step,
                                        onValueChange = { newValue -> viewModel.handleAction(CreateRecipeAction.UpdateCurrentInstruction(newValue)) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(fontSize = 14.sp)
                                    )
                                    IconButton(onClick = { viewModel.handleAction(CreateRecipeAction.RemoveInstruction(index))}) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null
                                        )


                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.handleAction(CreateRecipeAction.SaveRecipe)
                        if (state.errorMessage == null) onRecipeSaved()
                    },
                    enabled = !state.isSaving && state.form.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save Recipe")
                }

                state.errorMessage?.let {
                    Text(
                        text = it,
                        color = Red,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
@Composable
fun CollapsableCard(isActive : Boolean, title: String, content: @Composable () -> Unit){
    AnimatedVisibility(visible = isActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = elevatedCardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    content()
                }

            }
        }


    }
}



@Composable
fun LinedPaper(){
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),

        ) {
        // convert spacing to pixels once
        val spacingPx = 40.dp.toPx()

        // compute how many lines will fit
        val lineCount = (size.height / spacingPx).toInt()

        for (i in 0..lineCount) {
            val y: Float = i * spacingPx
            drawLine(
                color = SkyBlueMedium,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun Section(viewModel : CreateRecipeViewModel, title: String){
    val state by viewModel.state.collectAsState()
    Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    TextField(
        value = state.form.name,
        onValueChange = { viewModel.handleAction(CreateRecipeAction.UpdateName(it)) },
        label = { Text("Recipe Name") },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
        ),
        modifier = Modifier
            .border(2.dp, SkyBlueMedium)
    )
}
@Composable
fun IngridentSection(viewModel : CreateRecipeViewModel){
    val state by viewModel.state.collectAsState()
    Text("Ingredients", style = MaterialTheme.typography.headlineMedium,  color = MaterialTheme.colorScheme.onBackground)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = state.form.currentIngredientInput,
                        onValueChange = { viewModel.handleAction(CreateRecipeAction.UpdateCurrentIngredient(it)) },
                        label = { Text("Add ingredient") },
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, SkyBlueMedium),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                    Button(onClick = { viewModel.handleAction(CreateRecipeAction.AddIngredient) }) {
                        Text("Add")
                    }
                }
}

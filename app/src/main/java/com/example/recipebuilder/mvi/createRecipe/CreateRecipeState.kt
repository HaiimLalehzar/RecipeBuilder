package com.example.recipebuilder.mvi.createRecipe

import android.net.Uri
import com.example.recipebuilder.ResponseModel.Recipe
import com.example.recipebuilder.ui.screens.create.CreateRecipeTab
import com.example.recipebuilder.viewModel.CreateRecipeUIState

data class CreateRecipeState(

    val activeTab: CreateRecipeTab = CreateRecipeTab.Manual,
    val form: RecipeFormData = RecipeFormData(),

    val discovery: RecipeDiscoveryData = RecipeDiscoveryData(),

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    )
data class RecipeFormData(
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),

    val currentIngredientInput: String = "",
    val currentInstructionInput: String = "",

    val audioUri: Uri? = null,
    val tiktokUrl: String = "",
    val transcription: String = "",
    val isRecording: Boolean = false,

    val rawTranscriptionOutput: String = "",
    val detectedSentences: List<String> = emptyList(),
    val parsedInstructions: List<String> = emptyList(),

    val isTranscribing: Boolean = false,
    val transcriptionError: String? = null
)
data class RecipeDiscoveryData(
    val generatedRecipes: List<Recipe> = emptyList(),

    val apiState: CreateRecipeUIState = CreateRecipeUIState.Empty
)

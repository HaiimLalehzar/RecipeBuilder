package com.example.recipebuilder.mvi.createRecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.network.service
import com.example.recipebuilder.viewModel.CreateRecipeUIState
import com.example.recipebuilder.viewModel.RecipeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreateRecipeViewModel(
    private val recipeViewModel: RecipeViewModel
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRecipeState())
    val state = _state.asStateFlow()

    fun handleAction(action: CreateRecipeAction) {
        _state.update { currentState ->
            RecipeReducer(currentState, action)
        }

        when (action) {
            is CreateRecipeAction.LoadRandomRecipe -> fetchRandomRecipes()
            is CreateRecipeAction.RefreshRecipes -> fetchRandomRecipes()
            is CreateRecipeAction.SaveRecipe -> saveRecipe()
            else -> {}
        }
    }

    private fun fetchRandomRecipes() {
        viewModelScope.launch {

            try {
                val result = service.getRandomRecipe()

                _state.update {
                    it.copy(
                        discovery = it.discovery.copy(
                            generatedRecipes = result.recipes,
                            apiState = CreateRecipeUIState.Success(result.recipes)
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        discovery = it.discovery.copy(apiState = CreateRecipeUIState.Error(e))
                    )
                }
            }
        }
    }

    private fun saveRecipe() {
        val currentForm = _state.value.form
        if (currentForm.name.isBlank()) {
            _state.update { it.copy(errorMessage = "Recipe name is required", isSaving = false) }
            return
        }

        viewModelScope.launch {
            try {
                val newRecipe = Recipe(
                    id = UUID.randomUUID(),
                    name = currentForm.name,
                    ingredients = currentForm.ingredients,
                    instructions = currentForm.instructions,
                    audioPath = currentForm.audioUri?.toString(),
                    tiktokUrl = currentForm.tiktokUrl.ifBlank { null },
                    transcription = currentForm.transcription.ifBlank { null }
                )

                recipeViewModel.addRecipe(newRecipe)

                _state.update {
                    it.copy(
                        form = CreateRecipeState().form,
                        errorMessage = null,
                        isSaving = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
package com.example.recipebuilder.viewModel

sealed interface CreateRecipeUIState {
    data class Success(val listOfRecipes: List<com.example.recipebuilder.ResponseModel.Recipe>) : CreateRecipeUIState
    data class Error(val exception: Throwable) : CreateRecipeUIState
    object Loading : CreateRecipeUIState
    object Empty : CreateRecipeUIState
}



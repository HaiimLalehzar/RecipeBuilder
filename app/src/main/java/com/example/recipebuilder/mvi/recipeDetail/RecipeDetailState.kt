package com.example.recipebuilder.mvi.recipeDetail


import com.example.recipebuilder.model.Recipe

data class RecipeDetailState(
    val isLoading: Boolean = false,
    val recipe: Recipe? = null,
    val errorMessage: String? = null
)

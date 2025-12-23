package com.example.recipebuilder.mvi.recipeList


import com.example.recipebuilder.model.Recipe

data class RecipeListState(
    val isLoading: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val error: String? = null,
    val selectedRecipe: Recipe? = null
)


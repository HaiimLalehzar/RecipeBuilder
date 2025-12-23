package com.example.recipebuilder.mvi.recipeEditor

import com.example.recipebuilder.model.Recipe

data class RecipeEditorState(
    val selectedRecipe: Recipe? = null,

    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val currentIngredient: String = "",

    val instructions: List<String> = emptyList(),
    val currentInstruction: String = "",

    val recipes: List<Recipe> = emptyList()
)

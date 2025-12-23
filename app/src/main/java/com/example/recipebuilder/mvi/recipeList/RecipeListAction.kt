package com.example.recipebuilder.mvi.recipeList

import com.example.recipebuilder.model.Recipe

sealed interface RecipeListAction {
    object Load : RecipeListAction
    data class Loaded(val recipes: List<Recipe>) : RecipeListAction
    data class Failed(val message: String) : RecipeListAction
    data class SetSelectedRecipe(val recipe: Recipe) : RecipeListAction
}

package com.example.recipebuilder.mvi.recipeDetail


import java.util.UUID
import com.example.recipebuilder.model.Recipe

sealed interface RecipeDetailAction {

    // UI triggers this
    data class Load(val id: UUID) : RecipeDetailAction

    // VM triggers these
    data class Loaded(val recipe: Recipe) : RecipeDetailAction
    data class Failed(val message: String) : RecipeDetailAction
}

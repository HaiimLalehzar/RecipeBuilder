package com.example.recipebuilder.mvi.recipeShare



import com.example.recipebuilder.model.Recipe
import java.util.UUID

sealed class ShareAction {
    object Load : ShareAction()
    data class RecipesLoaded(val recipes: List<Recipe>) : ShareAction()

    data class ToggleRecipeSelection(val recipeId: UUID) : ShareAction()
    object ClearSelection : ShareAction()
}

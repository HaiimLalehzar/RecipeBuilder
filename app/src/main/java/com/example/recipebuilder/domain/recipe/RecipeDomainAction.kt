package com.example.recipebuilder.domain.recipe


import com.example.recipebuilder.model.Recipe
import java.util.UUID

sealed interface RecipeDomainAction {

    // ===== Intents (called from UI / ViewModels) =====
    object LoadAll : RecipeDomainAction
    data class SelectRecipe(val id: UUID) : RecipeDomainAction

    data class CreateRecipe(val recipe: Recipe) : RecipeDomainAction
    data class UpdateRecipe(val recipe: Recipe) : RecipeDomainAction
    data class DeleteRecipe(val recipe: Recipe?) : RecipeDomainAction

    object ClearSelection : RecipeDomainAction

    // ===== Internal results (used inside domain only) =====
    object Loading : RecipeDomainAction
    data class Loaded(val recipes: List<Recipe>) : RecipeDomainAction
    data class Failed(val message: String) : RecipeDomainAction
    data class Selected(val recipe: Recipe?) : RecipeDomainAction
}

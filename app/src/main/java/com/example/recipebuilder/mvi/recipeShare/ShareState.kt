package com.example.recipebuilder.mvi.recipeShare



import com.example.recipebuilder.model.Recipe
import java.util.UUID

data class ShareState(
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeIds: Set<UUID> = emptySet(),
    val isLoading: Boolean = false
) {
    val hasSelection: Boolean get() = selectedRecipeIds.isNotEmpty()
}

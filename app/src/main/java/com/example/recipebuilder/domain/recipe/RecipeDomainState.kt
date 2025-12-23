package com.example.recipebuilder.domain.recipe

import com.example.recipebuilder.model.Recipe
import java.util.UUID

data class RecipeDomainState(
    val isLoading: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeId: UUID? = null,
    val selectedRecipe: Recipe? = null,
    val error: String? = null
)

package com.example.recipebuilder.domain.recipe


import androidx.compose.runtime.LaunchedEffect
import com.example.recipebuilder.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class RecipeDomainFlow(
    private val repo: RecipeRepository
) {

    private val _state = MutableStateFlow(RecipeDomainState())
    val state: StateFlow<RecipeDomainState> = _state


    /**
     * Handle a domain action.
     * This is suspend because it may call the repository.
     * The *caller* (ViewModel) is responsible for running this inside a coroutine.
     */
    suspend fun handle(action: RecipeDomainAction) {
        when (action) {

            // ====== Intents from UI ======

            RecipeDomainAction.LoadAll -> {
                // set loading
                _state.update { s -> recipeDomainReducer(s, RecipeDomainAction.Loading) }
                try {
                    val recipes = repo.getAllRecipes()
                    _state.update { s -> recipeDomainReducer(s, RecipeDomainAction.Loaded(recipes)) }
                } catch (e: Exception) {
                    _state.update { s ->
                        recipeDomainReducer(
                            s,
                            RecipeDomainAction.Failed(e.message ?: "Failed to load recipes")
                        )
                    }
                }
            }

            is RecipeDomainAction.SelectRecipe -> {
                val recipe = repo.getRecipeById(action.id)
                _state.update { s -> recipeDomainReducer(s, RecipeDomainAction.Selected(recipe)) }
            }

            is RecipeDomainAction.CreateRecipe -> {
                repo.addRecipe(action.recipe)
                // reload list
                handle(RecipeDomainAction.LoadAll)
            }

            is RecipeDomainAction.UpdateRecipe -> {
                repo.updateRecipe(action.recipe)
                handle(RecipeDomainAction.LoadAll)
            }

            is RecipeDomainAction.DeleteRecipe -> {
                repo.deleteRecipe(action.recipe)
                handle(RecipeDomainAction.LoadAll)
            }

            RecipeDomainAction.ClearSelection -> {
                _state.update { s -> recipeDomainReducer(s, RecipeDomainAction.Selected(null)) }
            }

            // ====== Internal result actions (already processed in reducer) ======
            RecipeDomainAction.Loading,
            is RecipeDomainAction.Loaded,
            is RecipeDomainAction.Failed,
            is RecipeDomainAction.Selected -> {
                // No extra work here; reducer already updated state.
            }
        }
    }
}

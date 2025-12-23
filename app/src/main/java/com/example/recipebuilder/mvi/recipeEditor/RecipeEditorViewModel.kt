package com.example.recipebuilder.mvi.recipeEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import com.example.recipebuilder.domain.recipe.RecipeDomainAction.*
import com.example.recipebuilder.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    private val recipeFlow: RecipeDomainFlow
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeEditorState())
    val state = _state.asStateFlow()

    init {
        observeDomain()
    }

    fun dispatch(action: RecipeEditorAction) {
        // First, update local UI state via reducer
        _state.value = recipeEditorReducer(_state.value, action)

        when (action) {
            is RecipeEditorAction.SaveChanges -> {
                viewModelScope.launch {
                    val current = state.value
                    val selected = current.selectedRecipe
                    if (selected != null) {
                        val updated = Recipe(
                            id = selected.id,
                            name = current.name,
                            ingredients = current.ingredients,
                            instructions = current.instructions
                        )
                        recipeFlow.handle(UpdateRecipe(updated))
                        action.onDone()
                    }
                }
            }

            RecipeEditorAction.BackToList -> {
                // Optionally tell domain to clear selection, if you have such an action.
                // recipeFlow.handle(ClearSelected)  // if defined
            }

            RecipeEditorAction.Load -> {
                viewModelScope.launch {
                    recipeFlow.handle(LoadAll)
                }
            }

            else -> Unit
        }
    }

    private fun observeDomain() {
        viewModelScope.launch {
            recipeFlow.state.collect { domainState ->
                // 1) Keep recipes list in sync
                dispatch(
                    RecipeEditorAction.RecipesLoaded(domainState.recipes)
                )

                // 2) Sync selected recipe if domain exposes one
                domainState.selectedRecipe?.let { selected ->
                    dispatch(RecipeEditorAction.PopulateFromExisting(selected))
                }
            }
        }
    }
}

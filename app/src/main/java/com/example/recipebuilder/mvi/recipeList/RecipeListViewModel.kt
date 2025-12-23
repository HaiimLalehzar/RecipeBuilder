package com.example.recipebuilder.mvi.recipeList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebuilder.domain.recipe.RecipeDomainAction
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import com.example.recipebuilder.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val recipeFlow: RecipeDomainFlow
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeListState())
    val state = _state.asStateFlow()

    init {
        observeDomain()
        dispatch(RecipeListAction.Load)
    }

    fun dispatch(action: RecipeListAction) {
        // 1) Update UI state via reducer
        _state.update { recipeListReducer(it, action) }

        // 2) Trigger domain operations
        when (action) {

            RecipeListAction.Load -> {
                viewModelScope.launch {
                    recipeFlow.handle(RecipeDomainAction.LoadAll)
                }
            }

            // Domain results → UI reducer receives results through action
            is RecipeListAction.Loaded -> Unit
            is RecipeListAction.Failed -> Unit
            is RecipeListAction.SetSelectedRecipe -> Unit
        }
    }

    /**
     * Optional: call this when domain state changes
     * so UI reacts to domain results.
     */
    fun observeDomain() {
        viewModelScope.launch {
            recipeFlow.state.collect { domainState ->
                // translate domain state → MVI UI state action
                when {
                    domainState.error != null ->
                        dispatch(RecipeListAction.Failed(domainState.error))

                    domainState.recipes != emptyList<Recipe>() ->
                        dispatch(RecipeListAction.Loaded(domainState.recipes))
                }
            }
        }
    }
}

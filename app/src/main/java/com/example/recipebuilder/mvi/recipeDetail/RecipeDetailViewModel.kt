package com.example.recipebuilder.mvi.recipeDetail


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import com.example.recipebuilder.domain.recipe.RecipeDomainAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeFlow: RecipeDomainFlow
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state = _state.asStateFlow()

    init {
        observeDomain()
   }

    fun dispatch(action: RecipeDetailAction) {
        _state.update { recipeDetailReducer(it, action) }

        when (action) {

            is RecipeDetailAction.Load -> {
                viewModelScope.launch {
                    recipeFlow.handle(
                        RecipeDomainAction.SelectRecipe(action.id)
                    )
                }
            }

            is RecipeDetailAction.Loaded -> Unit
            is RecipeDetailAction.Failed -> Unit
        }
    }

    private fun observeDomain() {
        viewModelScope.launch {
            recipeFlow.state.collect { domainState ->

                domainState.selectedRecipe?.let {
                    dispatch(RecipeDetailAction.Loaded(it))
                }

                domainState.error?.let {
                    dispatch(RecipeDetailAction.Failed(it))
                }
            }
        }
    }
}

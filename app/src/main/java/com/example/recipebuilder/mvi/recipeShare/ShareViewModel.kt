package com.example.recipebuilder.mvi.recipeShare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebuilder.domain.recipe.RecipeDomainAction
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val recipeFlow: RecipeDomainFlow
) : ViewModel() {

    private val _state = MutableStateFlow(ShareState())
    val state = _state.asStateFlow()

    init {
        observeDomain()
        dispatch(ShareAction.Load)
    }

    fun dispatch(action: ShareAction) {
        _state.value = shareReducer(_state.value, action)

        when (action) {

            ShareAction.Load -> {
                viewModelScope.launch {
                    recipeFlow.handle(RecipeDomainAction.LoadAll)
                }
            }

            else -> Unit
        }
    }

    private fun observeDomain() {
        viewModelScope.launch {
            recipeFlow.state.collect { domainState ->

                val recipes = domainState.recipes ?: emptyList()

                dispatch(
                    ShareAction.RecipesLoaded(recipes)
                )
            }
        }
    }
}

package com.example.recipebuilder.mvi.recipeShare

// package com.example.recipebuilder.mvi.share


fun shareReducer(
    state: ShareState,
    action: ShareAction
): ShareState {
    return when (action) {

        ShareAction.Load ->
            state.copy(isLoading = true)

        is ShareAction.RecipesLoaded ->
            state.copy(
                recipes = action.recipes,
                isLoading = false
            )

        is ShareAction.ToggleRecipeSelection -> {
            val current = state.selectedRecipeIds
            val newSet = if (current.contains(action.recipeId)) {
                current - action.recipeId
            } else {
                current + action.recipeId
            }
            state.copy(selectedRecipeIds = newSet)
        }

        ShareAction.ClearSelection ->
            state.copy(selectedRecipeIds = emptySet())
    }
}

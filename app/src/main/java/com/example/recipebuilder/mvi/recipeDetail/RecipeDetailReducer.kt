package com.example.recipebuilder.mvi.recipeDetail


fun recipeDetailReducer(
    state: RecipeDetailState,
    action: RecipeDetailAction
): RecipeDetailState {

    return when (action) {

        is RecipeDetailAction.Load ->
            state.copy(
                isLoading = true,
                errorMessage = null
            )

        is RecipeDetailAction.Loaded ->
            state.copy(
                isLoading = false,
                recipe = action.recipe,
                errorMessage = null
            )

        is RecipeDetailAction.Failed ->
            state.copy(
                isLoading = false,
                errorMessage = action.message
            )
    }
}

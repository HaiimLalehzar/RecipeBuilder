package com.example.recipebuilder.mvi.recipeList


fun recipeListReducer(
    state: RecipeListState,
    action: RecipeListAction
): RecipeListState =
    when (action) {

        RecipeListAction.Load ->
            state.copy(isLoading = true, error = null)

        is RecipeListAction.Loaded ->
            state.copy(
                isLoading = false,
                recipes = action.recipes
            )

        is RecipeListAction.Failed ->
            state.copy(
                isLoading = false,
                error = action.message
            )
        is RecipeListAction.SetSelectedRecipe ->
            state.copy(
                selectedRecipe = action.recipe
            )
    }

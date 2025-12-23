package com.example.recipebuilder.domain.recipe


fun recipeDomainReducer(
    state: RecipeDomainState,
    action: RecipeDomainAction
): RecipeDomainState =
    when (action) {

        // Intents: no state change directly – they trigger work elsewhere
        RecipeDomainAction.LoadAll,
        is RecipeDomainAction.CreateRecipe,
        is RecipeDomainAction.UpdateRecipe,
        is RecipeDomainAction.DeleteRecipe,
        is RecipeDomainAction.SelectRecipe,
        RecipeDomainAction.ClearSelection -> {
            // These are handled by the store with repo calls; reducer stays pure.
            state
        }

        // Internal result actions ↓

        RecipeDomainAction.Loading ->
            state.copy(
                isLoading = true,
                error = null
            )

        is RecipeDomainAction.Loaded ->
            state.copy(
                isLoading = false,
                recipes = action.recipes,
                error = null
            )

        is RecipeDomainAction.Failed ->
            state.copy(
                isLoading = false,
                error = action.message
            )

        is RecipeDomainAction.Selected ->
            state.copy(
                selectedRecipe = action.recipe,
                selectedRecipeId = action.recipe?.id
            )
    }

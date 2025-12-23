package com.example.recipebuilder.mvi.recipeEditor

import com.example.recipebuilder.model.Recipe

fun recipeEditorReducer(
    state: RecipeEditorState,
    action: RecipeEditorAction
): RecipeEditorState {
    return when (action) {

        is RecipeEditorAction.PopulateFromExisting -> {
            state.copy(
                selectedRecipe = action.recipe,
                name = action.recipe.name,
                ingredients = action.recipe.ingredients,
                instructions = action.recipe.instructions,
                currentIngredient = "",
                currentInstruction = ""
            )
        }

        is RecipeEditorAction.RecipesLoaded -> {
            state.copy(recipes = action.recipes)
        }

        is RecipeEditorAction.UpdateName ->
            state.copy(name = action.name)

        is RecipeEditorAction.UpdateNewIngredientText ->
            state.copy(currentIngredient = action.text)

        RecipeEditorAction.AddNewIngredient -> {
            val text = state.currentIngredient.trim()
            if (text.isEmpty()) state
            else state.copy(
                ingredients = state.ingredients + text,
                currentIngredient = ""
            )
        }

        is RecipeEditorAction.UpdateNewInstructionText ->
            state.copy(currentInstruction = action.text)

        RecipeEditorAction.AddNewInstruction -> {
            val text = state.currentInstruction.trim()
            if (text.isEmpty()) state
            else state.copy(
                instructions = state.instructions + text,
                currentInstruction = ""
            )
        }


        is RecipeEditorAction.SetSelectedRecipe -> {
            // Selecting a recipe also populates fields for editing
            state.copy(
                selectedRecipe = action.recipe,
                name = action.recipe.name,
                ingredients = action.recipe.ingredients,
                instructions = action.recipe.instructions,
                currentIngredient = "",
                currentInstruction = ""
            )
        }

        RecipeEditorAction.BackToList -> {
            // Clear selection & editor fields, keep recipes list
            RecipeEditorState(
                recipes = state.recipes
            )
        }

        RecipeEditorAction.Load -> {
            // Could set a loading flag here if you add one later
            state
        }

        is RecipeEditorAction.SaveChanges -> {
            // State changes are handled in ViewModel side-effect
            state
        }

        is RecipeEditorAction.UpdateIngredient -> {
            val updated = state.ingredients.toMutableList()
            updated[action.index] = action.text
            state.copy(ingredients = updated)
        }

        is RecipeEditorAction.RemoveIngredient -> {
            val updated = state.ingredients.toMutableList()
            updated.removeAt(action.index)
            state.copy(ingredients = updated)
        }

        is RecipeEditorAction.UpdateInstruction -> {
            val updated = state.instructions.toMutableList()
            updated[action.index] = action.text
            state.copy(instructions = updated)
        }

        is RecipeEditorAction.RemoveInstruction -> {
            val updated = state.instructions.toMutableList()
            updated.removeAt(action.index)
            state.copy(instructions = updated)
        }

    }
}

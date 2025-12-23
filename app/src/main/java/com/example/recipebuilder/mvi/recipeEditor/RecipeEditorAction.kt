package com.example.recipebuilder.mvi.recipeEditor

import com.example.recipebuilder.model.Recipe

sealed class RecipeEditorAction {

    // UI / domain-driven population
    data class PopulateFromExisting(val recipe: Recipe) : RecipeEditorAction()
    data class RecipesLoaded(val recipes: List<Recipe>) : RecipeEditorAction()

    // Editing fields
    data class UpdateName(val name: String) : RecipeEditorAction()

    data class UpdateIngredient(val index: Int, val text: String) : RecipeEditorAction()

    data class RemoveIngredient(val index: Int) : RecipeEditorAction()

    data class UpdateInstruction(val index: Int, val text: String) : RecipeEditorAction()
    data class RemoveInstruction(val index: Int) : RecipeEditorAction()

    data class UpdateNewIngredientText(val text: String) : RecipeEditorAction()
    object AddNewIngredient : RecipeEditorAction()

    data class UpdateNewInstructionText(val text: String) : RecipeEditorAction()
    object AddNewInstruction : RecipeEditorAction()



    // Save / navigation
    data class SaveChanges(val onDone: () -> Unit) : RecipeEditorAction()
    object BackToList : RecipeEditorAction()

    // Selection
    data class SetSelectedRecipe(val recipe: Recipe) : RecipeEditorAction()

    // Initial load
    object Load : RecipeEditorAction()
}

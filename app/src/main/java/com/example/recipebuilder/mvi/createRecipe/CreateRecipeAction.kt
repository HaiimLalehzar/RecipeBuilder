package com.example.recipebuilder.mvi.createRecipe

import java.io.File


sealed interface CreateRecipeAction {
    data class UpdateName(val name: String) : CreateRecipeAction
    data class UpdateTikTokUrl(val url: String) : CreateRecipeAction
    data class UpdateCurrentIngredient( val input: String) : CreateRecipeAction
    data class UpdateCurrentInstruction(val input: String) : CreateRecipeAction

    data object AddIngredient : CreateRecipeAction
    data class RemoveIngredient(val index: Int) : CreateRecipeAction
    data object AddInstruction : CreateRecipeAction
    data class RemoveInstruction(val index: Int) : CreateRecipeAction

    data object LoadRandomRecipe : CreateRecipeAction

    data object RefreshRecipes : CreateRecipeAction

    data object SaveRecipe : CreateRecipeAction

    data object ClearFields : CreateRecipeAction

    data class TranscribeFile(val file: File) : CreateRecipeAction
    data object ResetTranscription : CreateRecipeAction
}





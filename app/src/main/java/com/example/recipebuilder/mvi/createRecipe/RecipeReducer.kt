package com.example.recipebuilder.mvi.createRecipe

import com.example.recipebuilder.viewModel.CreateRecipeUIState

fun RecipeReducer(oldState: CreateRecipeState, action: CreateRecipeAction): CreateRecipeState {
    return when (action) {
        is CreateRecipeAction.UpdateName -> oldState.copy(
            form = oldState.form.copy(name = action.name)
        )
        is CreateRecipeAction.UpdateTikTokUrl -> oldState.copy(
            form = oldState.form.copy(tiktokUrl = action.url)
        )
        is CreateRecipeAction.UpdateCurrentIngredient -> oldState.copy(
            form = oldState.form.copy(currentIngredientInput = action.input)
        )
        is CreateRecipeAction.UpdateCurrentInstruction -> oldState.copy(
            form = oldState.form.copy(currentInstructionInput = action.input)
        )

        is CreateRecipeAction.AddIngredient -> {
            val input = oldState.form.currentIngredientInput
            if (input.isNotBlank()) {
                oldState.copy(
                    form = oldState.form.copy(
                        ingredients = oldState.form.ingredients + input.trim(),
                        currentIngredientInput = ""
                    )
                )
            } else oldState
        }
        is CreateRecipeAction.RemoveIngredient -> {
            val newList = oldState.form.ingredients.toMutableList().apply {
                if (action.index in indices) removeAt(action.index)
            }
            oldState.copy(form = oldState.form.copy(ingredients = newList))
        }

        is CreateRecipeAction.AddInstruction -> {
            val input = oldState.form.currentInstructionInput
            if (input.isNotBlank()) {
                oldState.copy(
                    form = oldState.form.copy(
                        instructions = oldState.form.instructions + input.trim(),
                        currentInstructionInput = ""
                    )
                )
            } else oldState
        }
        is CreateRecipeAction.RemoveInstruction -> {
            val newList = oldState.form.instructions.toMutableList().apply {
                if (action.index in indices) removeAt(action.index)
            }
            oldState.copy(form = oldState.form.copy(instructions = newList))
        }


        is CreateRecipeAction.ClearFields -> oldState.copy(
            form = RecipeFormData(),
            errorMessage = null,
            isSaving = false
        )

        is CreateRecipeAction.LoadRandomRecipe -> oldState.copy(
            discovery = oldState.discovery.copy(apiState = CreateRecipeUIState.Loading)
        )
        is CreateRecipeAction.RefreshRecipes -> oldState.copy(
            discovery = oldState.discovery.copy(apiState = CreateRecipeUIState.Loading)
        )

        is CreateRecipeAction.SaveRecipe -> oldState.copy(
            isSaving = true,
            errorMessage = null
        )
        is CreateRecipeAction.TranscribeFile -> {
            oldState.copy(
                form = oldState.form.copy(
                    isTranscribing = true,
                    rawTranscriptionOutput = "",
                    detectedSentences = emptyList(),
                    parsedInstructions = emptyList(),
                    transcriptionError = null
                )
            )
        }

        is CreateRecipeAction.ResetTranscription -> {
            oldState.copy(
                form = oldState.form.copy(
                    isTranscribing = false,
                    rawTranscriptionOutput = "",
                    detectedSentences = emptyList(),
                    parsedInstructions = emptyList(),
                    transcriptionError = null
                )
            )
        }

    }
}

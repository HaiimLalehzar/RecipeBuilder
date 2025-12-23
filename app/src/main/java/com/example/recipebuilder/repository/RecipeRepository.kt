package com.example.recipebuilder.repository

import com.example.recipebuilder.db.RecipeDao
import com.example.recipebuilder.model.Recipe
import java.util.UUID

class RecipeRepository(private val dao: RecipeDao) {
    suspend fun getAllRecipes(): List<Recipe> {

        return dao.getAllRecipes()
    }

    suspend fun getRecipeById(id: UUID): Recipe? {
        return dao.getRecipeById(id)
    }

    suspend fun addRecipe(recipe: Recipe) {
        dao.addRecipe(recipe)
    }

    suspend fun updateRecipe(recipe: Recipe) {
        dao.updateRecipe(recipe)
    }

    fun deleteRecipe(recipe: Recipe?) {
        dao.deleteRecipe(recipe?: return)
    }
    }
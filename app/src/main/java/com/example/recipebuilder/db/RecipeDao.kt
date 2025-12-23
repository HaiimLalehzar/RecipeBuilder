package com.example.recipebuilder.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.recipebuilder.model.Recipe
import java.util.UUID

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    suspend fun  getAllRecipes(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun  getRecipeById(id: UUID): Recipe?

    @Insert
    suspend fun  addRecipe(recipe: Recipe)

    @Update
    suspend fun  updateRecipe(recipe: Recipe)


    @Delete
    fun deleteRecipe(recipe: Recipe)


}
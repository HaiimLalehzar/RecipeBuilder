package com.example.recipebuilder.navigation

import android.app.Application
import com.example.recipebuilder.db.RecipeDao
import com.example.recipebuilder.db.RecipeDatabase
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import com.example.recipebuilder.repository.RecipeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecipeModule {

    @Provides
    @Singleton
    fun provideRecipeDao(app: Application): RecipeDao {
        return RecipeDatabase.getDatabase(app).recipeDao()
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(dao: RecipeDao): RecipeRepository {
        return RecipeRepository(dao)
    }

    @Provides
    @Singleton
    fun provideRecipeDomainFlow(
        repo: RecipeRepository
    ): RecipeDomainFlow {
        return RecipeDomainFlow(repo)
    }
}

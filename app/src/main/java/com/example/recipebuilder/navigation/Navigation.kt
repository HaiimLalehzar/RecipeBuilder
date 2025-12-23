package com.example.recipebuilder.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recipebuilder.db.RecipeDatabase
import com.example.recipebuilder.domain.recipe.RecipeDomainFlow
import com.example.recipebuilder.repository.RecipeRepository
import com.example.recipebuilder.ui.screens.create.CreateRecipeScreen
import com.example.recipebuilder.ui.screens.HomeScreen
import com.example.recipebuilder.ui.screens.detail.RecipeDetailScreen
import com.example.recipebuilder.ui.screens.editor.RecipeEditorScreen
import com.example.recipebuilder.ui.screens.list.RecipeListScreen
import com.example.recipebuilder.ui.screens.share.ShareRecipeScreen
import com.example.recipebuilder.viewModel.RecipeViewModel


@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: RecipeViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
    val repo = remember {
        RecipeRepository(
            RecipeDatabase.getDatabase(application).recipeDao()
        )
    }

    val recipeFlow = remember {
        RecipeDomainFlow(repo)
    }

    val navController = rememberNavController()

    val navActions = remember(navController) {
        NavActions(
            toRecipeList = { navController.navigate("recipeList") },
            toRecipeDetail = { recipeId -> navController.navigate("recipeDetail/$recipeId") },
            toRecipeEditor = { navController.navigate("RecipeEditor") },
            toCreateRecipe = { navController.navigate("CreateRecipe") },
            toShareRecipe = { navController.navigate("ShareRecipe") }
        )
    }
    NavHost(navController = navController, startDestination = "home"){

        composable(route = "home"){
            //We normally just pass NavController, but this keeps a separation of concerns
            HomeScreen(
                modifier = modifier,
                onNavigateToRecipeList = { navActions.toRecipeList() },
                onNavigateToRecipeDetail = { navActions.toRecipeDetail(it) },
                onNavigateToRecipeEditor = { navActions.toRecipeEditor()},
                onNavigateToCreateRecipe = { navActions.toCreateRecipe() },
                viewModel = viewModel,
                onNavigateToRecipeShare = { navActions.toShareRecipe() }
            )
        }
        composable(route = "recipeList"){
            RecipeListScreen(modifier = modifier, onBack = { navController.popBackStack()}, navigateToRecipeDetail = { navActions.toRecipeDetail(it.toString()) } )
        }
        composable(
            route = "recipeDetail/{recipeId}"
        ) { backStackEntry ->

            val recipeId = backStackEntry
                .arguments
                ?.getString("recipeId")
                ?: error("Missing recipeId")

            RecipeDetailScreen(
                recipeId = recipeId,
                onBack = { navController.popBackStack() }
            )
        }

            composable(route = "recipeEditor"){
            RecipeEditorScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }
        composable(route = "CreateRecipe") {
            CreateRecipeScreen(mainViewModel = viewModel, onBack = { navController.popBackStack() }, onRecipeSaved = { navController.popBackStack() })

        }
        composable(route = "ShareRecipe") {
            ShareRecipeScreen(modifier = modifier, onBack = { navController.popBackStack() })
        }


    }



}
data class NavActions(
    val toRecipeList: () -> Unit,
    val toRecipeDetail: (String) -> Unit,
    val toRecipeEditor: () -> Unit,
    val toCreateRecipe: () -> Unit,
    val toShareRecipe: () -> Unit
)



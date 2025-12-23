package com.example.recipebuilder.ui.screens.share

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.recipebuilder.mvi.recipeShare.ShareAction
import com.example.recipebuilder.mvi.recipeShare.ShareViewModel



import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.ui.screens.list.RecipeListItem
import com.example.recipebuilder.ui.theme.PureWhite

@Composable
fun ShareRecipeScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    vm: ShareViewModel = hiltViewModel()
) {
    val uiState by vm.state.collectAsState()
    val context = LocalContext.current

    // Derive selected recipes from state
    val selectedRecipes: List<Recipe> =
        uiState.recipes.filter { recipe ->
            uiState.selectedRecipeIds.contains(recipe.id)
        }

    Scaffold(
        contentColor = PureWhite,
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .offset(y = 10.dp)
                        .background(PureWhite),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Share",
                        color = Black,
                        fontSize = 25.sp,
                        modifier = Modifier.weight(4f)
                    )

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", color = Black)
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedRecipes.isNotEmpty()) {
                Button(
                    onClick = { shareRecipes(context, selectedRecipes) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Text("Share", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.padding(paddingValues)
        ) {
            item {
                Text(
                    "Select Which Recipes to Share!",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    color = Black
                )
            }

            items(uiState.recipes.size) { index ->
                val recipe = uiState.recipes[index]
                val isSelected = uiState.selectedRecipeIds.contains(recipe.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            if (isSelected) Color(0xFFE91E63)
                            else PureWhite
                        )
                ) {
                    RecipeListItem(
                        recipe = recipe,
                        onClickName = {
                            vm.dispatch(ShareAction.ToggleRecipeSelection(recipe.id))
                        }
                    )
                }
            }
        }
    }
}
fun shareRecipes(context: Context, recipes: List<Recipe>) {
    val shareText = buildString {
        recipes.forEach { recipe ->
            appendLine("Title: ${recipe.name}")
            appendLine()
            appendLine("Ingredients:")
            recipe.ingredients.forEach { appendLine("• $it") }
            appendLine()
            appendLine("Instructions:")
            recipe.instructions.forEachIndexed { i, step ->
                appendLine("${i + 1}. $step")
            }
            appendLine("\n----------------------------\n")
        }
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share recipes via")
    context.startActivity(shareIntent)
}

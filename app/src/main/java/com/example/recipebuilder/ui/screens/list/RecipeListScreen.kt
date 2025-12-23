package com.example.recipebuilder.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.recipebuilder.viewModel.RecipeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.mvi.recipeList.RecipeListAction
import com.example.recipebuilder.mvi.recipeList.RecipeListViewModel
import com.example.recipebuilder.ui.theme.PureWhite
import java.util.UUID

@Composable
fun RecipeListScreen(modifier: Modifier,
                     onBack: () -> Unit = {},
                     navigateToRecipeDetail: (String) -> Unit = {}) {

    val listVM: RecipeListViewModel = hiltViewModel()
    val uiState = listVM.state.collectAsState()

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
                        .padding(horizontal = 16.dp, vertical = 8.dp).offset(y = (10).dp).background(PureWhite),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Recipes", color = Black, fontSize = 25.sp, modifier = Modifier.weight(4f))

                    IconButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = modifier.padding(paddingValues)
        ) {
            items(uiState.value.recipes.size) { index ->
                RecipeListItem(
                    recipe = uiState.value.recipes[index],
                    onClickName = {listVM.dispatch(RecipeListAction.SetSelectedRecipe(uiState.value.recipes[index])) ;navigateToRecipeDetail(uiState.value.recipes[index].id.toString()) }
                )


            }
        }
    }

}

@Composable
fun RecipeListItem(recipe: Recipe, onClickName: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(
                onClick = {
                    onClickName()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )

    ){
        Text(text = recipe.name)

    }
}
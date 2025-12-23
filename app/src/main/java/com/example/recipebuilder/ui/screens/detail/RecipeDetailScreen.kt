package com.example.recipebuilder.ui.screens.detail

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipebuilder.ui.theme.PureWhite
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.recipebuilder.mvi.recipeDetail.*
import java.util.UUID
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun RecipeDetailScreen(
    modifier: Modifier = Modifier,
    recipeId: String,
    onBack: () -> Unit = {},
){
    val viewModel: RecipeDetailViewModel = hiltViewModel(key = "RecipeDetail-$recipeId")
    val uiState by viewModel.state.collectAsState()


    // Trigger data load when ID arrives
    LaunchedEffect(recipeId) {
        if (uiState.recipe?.id?.toString() != recipeId) {

            viewModel.dispatch(
                RecipeDetailAction.Load(UUID.fromString(recipeId))

            )
        }
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
                        text = uiState.recipe?.name ?: "Loading...",
                        color = Black,
                        fontSize = 25.sp,
                        modifier = Modifier.weight(4f)
                    )

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    ) { paddingValues ->
        Log.d("RecipeDetailScreen", "UI state: $uiState")


        when {
            uiState.isLoading -> {
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            uiState.errorMessage != null -> {
                Text(
                    text = "⚠ Error: ${uiState.errorMessage}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red
                )
            }

            uiState.recipe != null -> {
                LazyColumn(
                    modifier = modifier
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxSize()
                ) {
                    item {
                        // INGREDIENT CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {

                            Text(
                                text = "Ingredients",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(8.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            uiState.recipe!!.ingredients.forEach { ingredient ->
                                Row {
                                    AnimatedCheckbox(ingredient)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // INSTRUCTIONS CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {

                            Text(
                                text = "Instructions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(8.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            uiState.recipe!!.instructions.forEachIndexed { index, instruction ->
                                Text(
                                    text = "${index + 1}. $instruction",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun AnimatedCheckbox(text: String) {
    var checked by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(5.dp)
            .clickable { checked = !checked }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        AnimatedVisibility(visible = checked) {}
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

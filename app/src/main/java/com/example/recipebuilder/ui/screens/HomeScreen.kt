package com.example.recipebuilder.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.viewModel.RecipeViewModel
import java.util.Collections.emptyList

@Composable
fun HomeScreen(
    modifier: Modifier,
    onNavigateToRecipeList: (String) -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToRecipeEditor: (String) -> Unit,
    viewModel: RecipeViewModel,
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipeShare: (String) -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()


    val transition = rememberInfiniteTransition(label = "bgTransition")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFF90CAF9),
                        Color(0xFF42A5F5)
                    ),
                    startX = offset,
                    endX = offset + 1000f
                )
            )
    ) {


        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Welcome to Recipe Builder",
                        lineHeight = 20.sp,
                        fontSize = 35.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                        color = Color(0xFF0D47A1),
                        fontWeight = FontWeight.Bold
                    )


                }

            }
            HomeSearchOverlay(
                modifier = Modifier.weight(.7f),
                allRecipes = recipes,
                onRecipeClick = { recipe -> onNavigateToRecipeDetail("recipeDetail") },
                viewModel = viewModel
            )

            Box(
                modifier = Modifier.weight(5f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {


                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                        MenuButton(
                            modifier = Modifier.weight(1f),
                            text = "Create New Recipe",
                            onClick = { onNavigateToCreateRecipe() })
                        MenuButton(
                            modifier = Modifier.weight(1f),
                            text = "View Recipes",
                            onClick = { onNavigateToRecipeList("recipeList") })



                        MenuButton(
                            modifier = Modifier.weight(1f),
                            text = "Share Recipe",
                            onClick = { onNavigateToRecipeShare("recipeShare") })
                        MenuButton(
                            modifier = Modifier.weight(1f),
                            text = "Edit Recipe",
                            onClick = { onNavigateToRecipeEditor("recipeEditor") })

                }
            }


        }
    }
}

@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFBBDEFB), RoundedCornerShape(16.dp))


            ,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E342E)
            )
        }
    }
}
@Composable
fun HomeSearchOverlay(
    allRecipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeViewModel
) {
    var query by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val filtered = remember(query, allRecipes) {
        if (query.isBlank()) emptyList()
        else allRecipes.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFBBDEFB), RoundedCornerShape(24.dp))
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    isActive = it.isNotBlank()
                },
                placeholder = { Text("Search recipes...", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color(0xFF42A5F5)
                ),
                textStyle = TextStyle(color = Color(0xFF0D47A1), fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .onFocusChanged(onFocusChanged = { isActive = it.isFocused }
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                    onNext = {
                        focusManager.clearFocus()
                    },
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),

                )
        }
        if (isActive) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { }
            ) {

                AnimatedVisibility(
                    visible = isActive,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp),
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 300),
                        expandFrom = Alignment.Top
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 250),
                        shrinkTowards = Alignment.Top
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .heightIn(max = 300.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (query.isEmpty()) "Start typing to search..." else "No results found",
                                    color = Color(0xFF0D47A1),
                                    fontSize = 14.sp,

                                    )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp),

                            ) {
                                items(filtered) { recipe ->


                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSelectedRecipe(recipe)
                                                    onRecipeClick(recipe)
                                                    isActive = false
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = recipe.name,
                                                fontSize = 16.sp,
                                                color = Color(0xFF0D47A1),
                                                fontWeight = FontWeight.SemiBold

                                            )
                                        }

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ShareRecipeButton(recipe: Recipe) {
    val context = LocalContext.current

    IconButton(onClick = {
        val shareText = buildString {
            appendLine("🍴 ${recipe.name}")
            appendLine()
            appendLine("Ingredients:")
            recipe.ingredients.forEach { appendLine("• $it") }
            appendLine()
            appendLine("Instructions:")
            recipe.instructions.forEachIndexed { i, step ->
                appendLine("${i + 1}. $step")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share recipe via")
        context.startActivity(shareIntent)
    }) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share recipe"
        )
    }
}

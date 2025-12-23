package com.example.recipebuilder.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.recipebuilder.mvi.recipeEditor.RecipeEditorAction
import com.example.recipebuilder.mvi.recipeEditor.RecipeEditorViewModel
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.mvi.recipeEditor.RecipeEditorState
import com.example.recipebuilder.ui.screens.list.RecipeListItem
import com.example.recipebuilder.ui.theme.PureWhite

@Composable
fun RecipeEditorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val editorVM: RecipeEditorViewModel = hiltViewModel()
    val editorState by editorVM.state.collectAsState()

    LaunchedEffect(Unit) {
        editorVM.dispatch(RecipeEditorAction.Load)
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
                        .offset(y = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editorState.selectedRecipe == null) "Select Recipe"
                        else "Editing Recipe",
                        color = Black,
                        fontSize = 25.sp,
                        modifier = Modifier.weight(4f)
                    )

                    IconButton(
                        onClick = {
                            if (editorState.selectedRecipe == null) onBack()
                            else editorVM.dispatch(RecipeEditorAction.BackToList)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    ) { padding ->

        if (editorState.selectedRecipe == null) {
            RecipeEditorSelectList(
                modifier = modifier.padding(padding),
                recipes = editorState.recipes,
                onSelect = {
                    // Selecting a recipe fully populates editor via reducer
                    editorVM.dispatch(RecipeEditorAction.SetSelectedRecipe(it))
                }
            )
        } else {
            RecipeEditorDetail(
                modifier = modifier.padding(padding),
                onFinish = {
                    editorVM.dispatch(RecipeEditorAction.BackToList)
                },
                vm = editorVM,
                uiState = editorState
            )
        }
    }
}

@Composable
fun RecipeEditorSelectList(
    modifier: Modifier,
    recipes: List<Recipe>,
    onSelect: (Recipe) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(recipes.size) { index ->
            RecipeListItem(
                recipe = recipes[index],
                onClickName = { onSelect(recipes[index]) }
            )
        }
    }
}

@Composable
fun RecipeEditorDetail(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit,
    vm: RecipeEditorViewModel,
    uiState: RecipeEditorState
) {
    val recipe = uiState.selectedRecipe
    if (recipe == null) {
        Text("No recipe selected")
        return
    }

    // NO LaunchedEffect here – selection already populated via reducer

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        ManualSectionEditor(
            name = uiState.name,
            ingredients = uiState.ingredients,
            instructions = uiState.instructions,
            currentIngredient = uiState.currentIngredient,
            currentInstruction = uiState.currentInstruction,
            onNameChange = { vm.dispatch(RecipeEditorAction.UpdateName(it)) },
            onIngredientChange = { vm.dispatch(RecipeEditorAction.UpdateNewIngredientText(it)) },
            onAddIngredient = { vm.dispatch(RecipeEditorAction.AddNewIngredient) },
            onInstructionChange = { vm.dispatch(RecipeEditorAction.UpdateNewInstructionText(it)) },
            onAddInstruction = { vm.dispatch(RecipeEditorAction.AddNewInstruction) },
            onUpdateIngredient = { idx, text -> vm.dispatch(RecipeEditorAction.UpdateIngredient(idx, text)) },
            onRemoveIngredient = { idx ->
                vm.dispatch(RecipeEditorAction.RemoveIngredient(idx))
            },
            onUpdateInstruction = { idx, text ->
                vm.dispatch(RecipeEditorAction.UpdateInstruction(idx, text))
            },
            onRemoveInstruction = { idx ->
                vm.dispatch(RecipeEditorAction.RemoveInstruction(idx))
            },
            onSave = { vm.dispatch(RecipeEditorAction.SaveChanges(onFinish)) }
        )
    }

}

@Composable
fun ManualSectionEditor(
    name: String,
    ingredients: List<String>,
    instructions: List<String>,
    currentIngredient: String,
    currentInstruction: String,
    onNameChange: (String) -> Unit,
    onIngredientChange: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onInstructionChange: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateIngredient: (Int, String) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onUpdateInstruction: (Int, String) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSave: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Recipe Name", color = Black) },
                textStyle = LocalTextStyle.current.copy(color = Black),
                modifier = Modifier.fillMaxWidth().background(Color.LightGray)
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Text("Ingredients", color = Black, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(ingredients) { index, ing ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ing,
                    onValueChange = { onUpdateIngredient(index, it) },
                    textStyle = LocalTextStyle.current.copy(color = Black),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp).background(Color.LightGray)
                )
                IconButton(onClick = { onRemoveIngredient(index) }) {
                    Text("X", color = Black)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = currentIngredient,
                onValueChange = onIngredientChange,
                textStyle = LocalTextStyle.current.copy(color = Black),
                label = { Text("Add ingredient", color = Black) },
                modifier = Modifier.fillMaxWidth().background(Color.LightGray)
            )
            Button(
                onClick = onAddIngredient,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Add Ingredient")
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text("Instructions", color = Black, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(instructions) { index, ins ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ins,
                    onValueChange = { onUpdateInstruction(index, it) },
                    textStyle = LocalTextStyle.current.copy(color = Black),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp).background(Color.LightGray)
                )
                IconButton(onClick = { onRemoveInstruction(index) }) {
                    Text("X", color = Black)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = currentInstruction,
                onValueChange = onInstructionChange,
                textStyle = LocalTextStyle.current.copy(color = Black),
                label = { Text("Add instruction", color = Black) },
                modifier = Modifier.fillMaxWidth().background(Color.LightGray)
            )
            Button(
                onClick = onAddInstruction,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Add Instruction")
            }
            Spacer(Modifier.height(32.dp))
        }

        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

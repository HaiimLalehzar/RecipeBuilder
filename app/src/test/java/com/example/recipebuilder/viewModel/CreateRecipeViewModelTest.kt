package com.example.recipebuilder.viewModel

import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.mvi.createRecipe.CreateRecipeAction
import com.example.recipebuilder.mvi.createRecipe.CreateRecipeViewModel

import com.example.recipebuilder.network.service
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelTest {

    private lateinit var viewModel: CreateRecipeViewModel
    private lateinit var mockRecipeViewModel: RecipeViewModel

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockRecipeViewModel = mockk(relaxed = true)
        mockkObject(service)

        viewModel = CreateRecipeViewModel(
            recipeViewModel = mockRecipeViewModel
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }


    @Test
    fun `initialState Correct`() = runTest {
        val state = viewModel.state.value

        assertEquals("", state.form.name)
        assertTrue(state.discovery.generatedRecipes.isEmpty())
        assertEquals(null, state.errorMessage)
        assertEquals(false, state.isSaving)
    }


    @Test
    fun `saveRecipe empty Name validation fail`() = runTest {
        viewModel.handleAction(CreateRecipeAction.SaveRecipe)

        val state = viewModel.state.value
        assertEquals("Recipe name is required", state.errorMessage)

        coVerify(exactly = 0) { mockRecipeViewModel.addRecipe(any()) }
    }


    @Test
    fun `saveRecipe success`() = runTest {
        viewModel.handleAction(CreateRecipeAction.UpdateName("Pizza"))

        val captured = slot<Recipe>()
        coEvery { mockRecipeViewModel.addRecipe(capture(captured)) } returns Unit

        viewModel.handleAction(CreateRecipeAction.SaveRecipe)
        advanceUntilIdle()

        val saved = captured.captured

        assertEquals("Pizza", saved.name)
        assertNotNull(saved.id)

        coVerify { mockRecipeViewModel.addRecipe(any()) }

        val state = viewModel.state.value
        assertEquals("", state.form.name)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `saveRecipe error set error message`() = runTest {
        viewModel.handleAction(CreateRecipeAction.UpdateName("Soup"))

        coEvery { mockRecipeViewModel.addRecipe(any()) } throws RuntimeException("DB fail")

        viewModel.handleAction(CreateRecipeAction.SaveRecipe)
        advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals("DB fail", state.errorMessage)
        coVerify { mockRecipeViewModel.addRecipe(any()) }
    }


    @Test
    fun `saveRecipe generate UUID`() = runTest {
        viewModel.handleAction(CreateRecipeAction.UpdateName("Cake"))

        val captured = slot<Recipe>()

        coEvery { mockRecipeViewModel.addRecipe(capture(captured)) } returns Unit

        viewModel.handleAction(CreateRecipeAction.SaveRecipe)
        advanceUntilIdle()

        val saved = captured.captured
        assertNotNull(saved.id)
    }


}

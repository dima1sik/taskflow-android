package com.example.cleantodoapp3

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cleantodoapp3.data.TodoDataRepository
import com.example.cleantodoapp3.data.TodoDatabase
import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.domain.TodoRepository
import com.example.cleantodoapp3.presentation.TodoViewModel
import com.example.cleantodoapp3.ui.screens.TodoScreen
import com.example.cleantodoapp3.ui.theme.CleanTodoApp3Theme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoScreenTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var database: TodoDatabase
    private lateinit var viewModel: TodoViewModel
    private lateinit var restorationTester: StateRestorationTester
    private val store = ViewModelStore()
    private var failAdding = false

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java).build()
        val dataRepository = TodoDataRepository(database.todoDao())
        val repository = object : TodoRepository by dataRepository {
            override suspend fun addTodo(todo: Todo) {
                if (failAdding) throw IllegalStateException("Simulated failed write")
                dataRepository.addTodo(todo)
            }
        }
        viewModel = TodoViewModel(repository)
        store.put("todos", viewModel)
        restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CleanTodoApp3Theme { TodoScreen(viewModel) }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { !viewModel.uiState.value.isLoading }
    }

    @After
    fun tearDown() {
        store.clear()
        database.close()
    }

    @Test
    fun newTaskDraftSurvivesStateRestoration() {
        composeRule.onNode(hasSetTextAction()).performTextInput("Unfinished draft")
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNode(hasSetTextAction()).assertTextEquals("Unfinished draft")
    }

    @Test
    fun failedAddKeepsDraftAndShowsError() {
        failAdding = true
        composeRule.onNode(hasSetTextAction()).performTextInput("Keep my draft")
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Couldn't add task. Please try again.").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertTextEquals("Keep my draft")
        assertTrue(viewModel.uiState.value.todos.isEmpty())
    }

    @Test
    fun editingCompletedTaskKeepsCompletion() {
        composeRule.onNode(hasSetTextAction()).performTextInput("Original")
        composeRule.onNodeWithText("Add").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.todos.size == 1 }
        composeRule.onNodeWithContentDescription("Change completion: Original").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.todos.single().isDone }
        composeRule.onNodeWithContentDescription("Edit task").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Original"))
            .performTextReplacement("Edited")
        composeRule.onNodeWithContentDescription("Save task").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.todos.single().title == "Edited" }
        assertTrue(viewModel.uiState.value.todos.single().isDone)
    }
}

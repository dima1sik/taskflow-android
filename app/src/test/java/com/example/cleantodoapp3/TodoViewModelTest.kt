package com.example.cleantodoapp3

import androidx.lifecycle.ViewModelStore
import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.presentation.TodoEvent
import com.example.cleantodoapp3.presentation.TodoViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    private lateinit var repository: FakeTodoRepository
    private lateinit var viewModel: TodoViewModel
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        repository = FakeTodoRepository()
        viewModel = TodoViewModel(repository)
        store.put("todos", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun observesSavedTasksAndFinishesLoading() = runTest {
        repository.todos.value = listOf(Todo(7, "Persisted task", true))
        runCurrent()

        assertEquals(repository.todos.value, viewModel.uiState.value.todos)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.loadFailed)
    }

    @Test
    fun readFailureCanBeRetriedWithoutReplacingStoredTasks() = runTest {
        repository.todos.value = listOf(Todo(7, "Existing task"))
        runCurrent()
        repository.readFailure = IllegalStateException("Database unavailable")
        viewModel.retryLoadingTodos()
        runCurrent()
        assertTrue(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Existing task", viewModel.uiState.value.todos.single().title)

        viewModel.retryLoadingTodos()
        runCurrent()
        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals("Existing task", viewModel.uiState.value.todos.single().title)
    }

    @Test
    fun announcesAddOnlyAfterSuccessfulWrite() = runTest {
        val events = collectEvents()
        viewModel.addTodo("  New task  ")
        runCurrent()

        assertEquals("New task", repository.todos.value.single().title)
        assertEquals(listOf(TodoEvent.Added("  New task  ")), events)
        assertFalse(viewModel.uiState.value.isAdding)
    }

    @Test
    fun preventsDuplicateSubmissionWhileInsertIsPending() = runTest {
        repository.addGate = CompletableDeferred()
        viewModel.addTodo("New task")
        viewModel.addTodo("New task")
        runCurrent()
        assertTrue(viewModel.uiState.value.isAdding)
        assertEquals(1, repository.addAttempts)

        repository.addGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, repository.todos.value.size)
        assertFalse(viewModel.uiState.value.isAdding)
    }

    @Test
    fun failedAddReportsErrorWithoutSuccessAndCanBeRetried() = runTest {
        val events = collectEvents()
        repository.mutationFailure = IllegalStateException("Disk full")
        viewModel.addTodo("Keep this draft")
        runCurrent()
        assertTrue(events.single() is TodoEvent.Error)
        assertTrue(repository.todos.value.isEmpty())
        assertFalse(viewModel.uiState.value.isAdding)

        repository.mutationFailure = null
        viewModel.addTodo("Keep this draft")
        runCurrent()
        assertTrue(events.last() is TodoEvent.Added)
    }

    @Test
    fun blankAddDoesNotReachRepository() = runTest {
        val events = collectEvents()
        viewModel.addTodo(" \t ")
        runCurrent()

        assertEquals(0, repository.addAttempts)
        assertTrue(events.single() is TodoEvent.Error)
    }

    @Test
    fun independentCompletionAndRenamePreserveBothChanges() = runTest {
        repository.todos.value = listOf(Todo(7, "Old title"))
        viewModel.setTodoDone(7, true)
        viewModel.editTodo(7, "New title")
        runCurrent()

        assertEquals(Todo(7, "New title", true), repository.todos.value.single())
        viewModel.setTodoDone(7, false)
        runCurrent()
        assertEquals(Todo(7, "New title", false), repository.todos.value.single())
    }

    @Test
    fun failedRenameKeepsOriginalTaskAndDoesNotCloseEditor() = runTest {
        val events = collectEvents()
        repository.todos.value = listOf(Todo(7, "Original", true))
        repository.mutationFailure = IllegalStateException("Disk full")
        viewModel.editTodo(7, "Edited")
        runCurrent()

        assertEquals(Todo(7, "Original", true), repository.todos.value.single())
        assertTrue(events.single() is TodoEvent.Error)
    }

    @Test
    fun failedCompletionAndDeletionReportErrorsAndLeaveTaskUntouched() = runTest {
        val events = collectEvents()
        repository.todos.value = listOf(Todo(7, "Original"))
        repository.mutationFailure = IllegalStateException("Disk full")
        viewModel.setTodoDone(7, true)
        viewModel.deleteTodo(7)
        runCurrent()

        assertEquals(Todo(7, "Original"), repository.todos.value.single())
        assertEquals(2, events.size)
        assertTrue(events.all { it is TodoEvent.Error })
    }

    @Test
    fun deleteUpdatesObservedList() = runTest {
        repository.todos.value = listOf(Todo(7, "Remove me"), Todo(9, "Keep me"))
        viewModel.deleteTodo(7)
        runCurrent()

        assertEquals(listOf(Todo(9, "Keep me")), viewModel.uiState.value.todos)
    }

    @Test
    fun cancellationIsNotReportedAsFailedWrite() = runTest {
        val events = collectEvents()
        repository.mutationFailure = CancellationException("ViewModel cleared")
        viewModel.addTodo("Cancelled")
        viewModel.editTodo(7, "Cancelled")
        runCurrent()

        assertTrue(events.isEmpty())
        assertFalse(viewModel.uiState.value.isAdding)
    }

    private fun TestScope.collectEvents(): MutableList<TodoEvent> {
        val events = mutableListOf<TodoEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }
        return events
    }
}

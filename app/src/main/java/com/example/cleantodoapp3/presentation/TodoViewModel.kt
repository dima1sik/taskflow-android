package com.example.cleantodoapp3.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cleantodoapp3.domain.AddTodoUseCase
import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.domain.TodoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isAdding: Boolean = false
)

sealed interface TodoEvent {
    data class Added(val submittedTitle: String) : TodoEvent
    data class Renamed(val id: Int, val submittedTitle: String) : TodoEvent
    data class Error(val message: String) : TodoEvent
}

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    private val addTodoUseCase = AddTodoUseCase(repository)
    private val mutableUiState = MutableStateFlow(TodoUiState())
    val uiState = mutableUiState.asStateFlow()

    private val eventChannel = Channel<TodoEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var observeTodosJob: Job? = null

    init {
        retryLoadingTodos()
    }

    fun retryLoadingTodos() {
        observeTodosJob?.cancel()
        mutableUiState.update { it.copy(isLoading = true, loadFailed = false) }
        observeTodosJob = viewModelScope.launch {
            try {
                repository.getTodos().collect { todos ->
                    mutableUiState.update {
                        it.copy(todos = todos, isLoading = false, loadFailed = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun addTodo(title: String) {
        if (mutableUiState.value.isAdding) return
        mutableUiState.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            try {
                addTodoUseCase.execute(title)
                eventChannel.send(TodoEvent.Added(title))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventChannel.send(TodoEvent.Error("Couldn't add task. Please try again."))
            } finally {
                mutableUiState.update { it.copy(isAdding = false) }
            }
        }
    }

    fun setTodoDone(id: Int, isDone: Boolean) = performMutation {
        repository.setTodoDone(id, isDone)
    }

    fun editTodo(id: Int, newTitle: String) = performMutation {
        repository.renameTodo(id, newTitle)
        eventChannel.send(TodoEvent.Renamed(id, newTitle))
    }

    fun deleteTodo(id: Int) = performMutation {
        repository.deleteTodo(id)
    }

    private fun performMutation(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventChannel.send(TodoEvent.Error("Couldn't update task. Please try again."))
            }
        }
    }

    companion object {
        fun factory(repository: TodoRepository) = viewModelFactory {
            initializer { TodoViewModel(repository) }
        }
    }
}

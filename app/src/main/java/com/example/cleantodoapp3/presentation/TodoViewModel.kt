package com.example.cleantodoapp3.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.cleantodoapp3.data.TodoDataRepository
import com.example.cleantodoapp3.data.TodoDatabase
import com.example.cleantodoapp3.domain.AddTodoUseCase
import com.example.cleantodoapp3.domain.Todo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        TodoDatabase::class.java,
        "my_todo_db"
    ).build()

    private val todoRepository = TodoDataRepository(db.todoDao())
    private val addTodoUseCase = AddTodoUseCase(todoRepository)

    val todos = todoRepository.getTodos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun addTodo(title: String) {
        viewModelScope.launch {
            addTodoUseCase.execute(title)
        }
    }

    fun toggleTodoDone(todo: Todo) {
        viewModelScope.launch {
            todoRepository.updateTodo(
                todo.copy(isDone = !todo.isDone)
            )
        }
    }

    fun editTodo(todo: Todo, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                todoRepository.updateTodo(todo.copy(title = newTitle))
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }
}
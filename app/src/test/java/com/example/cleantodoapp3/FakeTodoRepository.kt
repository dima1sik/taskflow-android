package com.example.cleantodoapp3

import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.domain.TodoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class FakeTodoRepository : TodoRepository {
    val todos = MutableStateFlow<List<Todo>>(emptyList())
    var readFailure: Exception? = null
    var mutationFailure: Exception? = null
    var addGate: CompletableDeferred<Unit>? = null
    var addAttempts = 0

    override fun getTodos() = flow {
        readFailure?.let {
            readFailure = null
            throw it
        }
        emitAll(todos)
    }

    override suspend fun addTodo(todo: Todo) {
        addAttempts++
        addGate?.await()
        mutationFailure?.let { throw it }
        todos.update { it + todo.copy(id = (it.maxOfOrNull(Todo::id) ?: 0) + 1) }
    }

    override suspend fun setTodoDone(id: Int, isDone: Boolean) {
        mutationFailure?.let { throw it }
        todos.update { list -> list.map { if (it.id == id) it.copy(isDone = isDone) else it } }
    }

    override suspend fun renameTodo(id: Int, title: String) {
        mutationFailure?.let { throw it }
        todos.update { list -> list.map { if (it.id == id) it.copy(title = title) else it } }
    }

    override suspend fun deleteTodo(id: Int) {
        mutationFailure?.let { throw it }
        todos.update { list -> list.filterNot { it.id == id } }
    }
}

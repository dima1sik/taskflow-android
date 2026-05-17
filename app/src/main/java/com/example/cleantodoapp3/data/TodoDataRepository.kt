package com.example.cleantodoapp3.data

import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.domain.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoDataRepository(
    private val dao: TodoDao
) : TodoRepository {

    override fun getTodos(): Flow<List<Todo>> {
        return dao.getAllTodos().map { entityList ->
            entityList.map { entity ->
                Todo(
                    id = entity.id,
                    title = entity.title,
                    isDone = entity.isDone
                )
            }
        }
    }

    override suspend fun addTodo(todo: Todo) {
        dao.insert(
            TodoEntity(
                title = todo.title,
                isDone = todo.isDone
            )
        )
    }

    override suspend fun updateTodo(todo: Todo) {
        dao.update(
            TodoEntity(
                id = todo.id,
                title = todo.title,
                isDone = todo.isDone
            )
        )
    }

    override suspend fun deleteTodo(todo: Todo) {
        dao.delete(
            TodoEntity(
                id = todo.id,
                title = todo.title,
                isDone = todo.isDone
            )
        )
    }
}
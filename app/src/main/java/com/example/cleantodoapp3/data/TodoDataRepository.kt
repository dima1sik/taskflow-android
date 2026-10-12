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

    override suspend fun setTodoDone(id: Int, isDone: Boolean) {
        check(dao.setDone(id, isDone) == 1) { "Task no longer exists" }
    }

    override suspend fun renameTodo(id: Int, title: String) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Task title cannot be blank" }
        check(dao.rename(id, normalizedTitle) == 1) { "Task no longer exists" }
    }

    override suspend fun deleteTodo(id: Int) {
        check(dao.delete(id) == 1) { "Task no longer exists" }
    }
}

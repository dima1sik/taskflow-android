package com.example.cleantodoapp3.domain

import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<List<Todo>>
    suspend fun addTodo(todo: Todo)
    suspend fun setTodoDone(id: Int, isDone: Boolean)
    suspend fun renameTodo(id: Int, title: String)
    suspend fun deleteTodo(id: Int)
}

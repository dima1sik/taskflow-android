package com.example.cleantodoapp3.domain

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend fun execute(title: String) {
        repository.addTodo(
            Todo(
                title = title,
                isDone = false
            )
        )
    }
}
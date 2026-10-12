package com.example.cleantodoapp3.domain

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend fun execute(title: String) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Task title cannot be blank" }
        repository.addTodo(
            Todo(
                title = normalizedTitle,
                isDone = false
            )
        )
    }
}

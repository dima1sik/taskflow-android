package com.example.cleantodoapp3

import android.app.Application
import androidx.room.Room
import com.example.cleantodoapp3.data.TodoDataRepository
import com.example.cleantodoapp3.data.TodoDatabase
import com.example.cleantodoapp3.domain.TodoRepository

class TodoApplication : Application() {
    // One database for the lifetime of the app, shared across activity recreation.
    private val database by lazy {
        Room.databaseBuilder(this, TodoDatabase::class.java, "my_todo_db").build()
    }

    val todoRepository: TodoRepository by lazy { TodoDataRepository(database.todoDao()) }
}

package com.example.cleantodoapp3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isDone: Boolean = false
)
package com.example.cleantodoapp3.domain

data class Todo(
    val id: Int = 0,
    val title: String,
    val isDone: Boolean = false
)
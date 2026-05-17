package com.example.cleantodoapp3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.cleantodoapp3.presentation.TodoViewModel
import com.example.cleantodoapp3.ui.screens.TodoScreen
import com.example.cleantodoapp3.ui.theme.CleanTodoApp3Theme

class MainActivity : ComponentActivity() {

    private val todoViewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanTodoApp3Theme {
                Surface {
                    TodoScreen(todoViewModel = todoViewModel)
                }
            }
        }
    }
}
package com.example.cleantodoapp3.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cleantodoapp3.domain.Todo
import com.example.cleantodoapp3.presentation.TodoEvent
import com.example.cleantodoapp3.presentation.TodoViewModel
import kotlinx.coroutines.launch

private val AppRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    todoViewModel: TodoViewModel
) {
    var task by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editedTitle by rememberSaveable { mutableStateOf("") }
    val uiState by todoViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(todoViewModel) {
        todoViewModel.events.collect { event ->
            when (event) {
                is TodoEvent.Added -> if (task == event.submittedTitle) task = ""
                is TodoEvent.Renamed -> {
                    if (editingId == event.id && editedTitle == event.submittedTitle) {
                        editingId = null
                    }
                }
                is TodoEvent.Error -> launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    LaunchedEffect(uiState.todos, uiState.isLoading, uiState.loadFailed) {
        if (!uiState.isLoading && !uiState.loadFailed &&
            uiState.todos.none { it.id == editingId }
        ) {
            editingId = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppRed,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    placeholder = { Text("Enter task") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isAdding,
                    singleLine = true
                )

                Button(
                    onClick = {
                        todoViewModel.addTodo(task)
                    },
                    enabled = task.isNotBlank() && !uiState.isAdding &&
                        !uiState.isLoading && !uiState.loadFailed,
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppRed)
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.loadFailed) {
                Text("Couldn't load tasks. Your saved tasks have not been removed.")
                Button(onClick = todoViewModel::retryLoadingTodos) { Text("Retry") }
            } else if (uiState.todos.isEmpty()) {
                Text("No tasks yet. Add your first task above.")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = uiState.todos,
                    key = { it.id }
                ) { todo ->
                    TodoRow(
                        todo = todo,
                        isEditing = editingId == todo.id,
                        editedTitle = editedTitle,
                        onEditedTitleChange = { editedTitle = it },
                        onSetDone = { isDone -> todoViewModel.setTodoDone(todo.id, isDone) },
                        onStartEdit = {
                            editingId = todo.id
                            editedTitle = todo.title
                        },
                        onCancelEdit = { editingId = null },
                        onSaveEdit = { todoViewModel.editTodo(todo.id, editedTitle) },
                        onDelete = { todoViewModel.deleteTodo(todo.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    isEditing: Boolean,
    editedTitle: String,
    onEditedTitleChange: (String) -> Unit,
    onSetDone: (Boolean) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(
            checked = todo.isDone,
            onCheckedChange = onSetDone,
            modifier = Modifier.semantics {
                contentDescription = "Change completion: ${todo.title}"
            },
            colors = CheckboxDefaults.colors(
                checkedColor = AppRed,
                uncheckedColor = AppRed
            )
        )

        if (isEditing) {
            OutlinedTextField(
                value = editedTitle,
                onValueChange = onEditedTitleChange,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        } else {
            Text(
                text = todo.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
                )
            )
        }

        IconButton(
            onClick = if (isEditing) onSaveEdit else onStartEdit,
            enabled = !isEditing || editedTitle.isNotBlank()
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (isEditing) "Save task" else "Edit task",
                tint = AppRed
            )
        }

        IconButton(onClick = if (isEditing) onCancelEdit else onDelete) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Delete,
                contentDescription = if (isEditing) "Cancel editing" else "Delete task",
                tint = AppRed
            )
        }
    }
}

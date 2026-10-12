package com.example.cleantodoapp3

import com.example.cleantodoapp3.data.TodoDao
import com.example.cleantodoapp3.data.TodoDataRepository
import com.example.cleantodoapp3.data.TodoEntity
import com.example.cleantodoapp3.domain.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TodoDataRepositoryTest {
    private val dao = FakeDao()
    private val repository = TodoDataRepository(dao)

    @Test
    fun readsEntityIdsTitlesAndCompletion() = runTest {
        dao.entities.value = listOf(TodoEntity(7, "Saved", true))
        assertEquals(listOf(Todo(7, "Saved", true)), repository.getTodos().first())
    }

    @Test
    fun newTaskUsesDatabaseGeneratedIdentifier() = runTest {
        repository.addTodo(Todo(99, "New", true))
        assertEquals(TodoEntity(title = "New", isDone = true), dao.inserted)
    }

    @Test
    fun renameNormalizesWhitespace() = runTest {
        repository.renameTodo(7, "  Updated title  ")
        assertEquals(7 to "Updated title", dao.renamed)
    }

    @Test
    fun rejectsBlankRenameBeforeTouchingDatabase() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.renameTodo(7, " \t ") }
        }
        assertEquals(null, dao.renamed)
    }

    @Test
    fun reportsMissingTasksForEachMutation() {
        dao.affectedRows = 0
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.renameTodo(7, "Missing") }
        }
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.setTodoDone(7, true) }
        }
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.deleteTodo(7) }
        }
    }

    private class FakeDao : TodoDao {
        val entities = MutableStateFlow<List<TodoEntity>>(emptyList())
        var inserted: TodoEntity? = null
        var renamed: Pair<Int, String>? = null
        var affectedRows = 1

        override fun getAllTodos() = entities

        override suspend fun insert(todo: TodoEntity) {
            inserted = todo
        }

        override suspend fun setDone(id: Int, isDone: Boolean) = affectedRows

        override suspend fun rename(id: Int, title: String): Int {
            renamed = id to title
            return affectedRows
        }

        override suspend fun delete(id: Int) = affectedRows
    }
}

package com.example.cleantodoapp3

import com.example.cleantodoapp3.domain.AddTodoUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTodoUseCaseTest {
    @Test
    fun trimsTitleAndCreatesIncompleteTask() = runTest {
        val repository = FakeTodoRepository()
        AddTodoUseCase(repository).execute("  Review project  ")

        assertEquals("Review project", repository.todos.value.single().title)
        assertFalse(repository.todos.value.single().isDone)
    }

    @Test
    fun refusesBlankTitlesBeforeWriting() {
        val repository = FakeTodoRepository()
        for (title in listOf("", "   ", "\n\t")) {
            assertThrows(IllegalArgumentException::class.java) {
                runTest { AddTodoUseCase(repository).execute(title) }
            }
        }
        assertEquals(0, repository.addAttempts)
        assertTrue(repository.todos.value.isEmpty())
    }

    @Test
    fun preservesUnicodeAndInternalWhitespace() = runTest {
        val repository = FakeTodoRepository()
        AddTodoUseCase(repository).execute("  Купить  молоко ☕  ")

        assertEquals("Купить  молоко ☕", repository.todos.value.single().title)
    }
}

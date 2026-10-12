package com.example.cleantodoapp3

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cleantodoapp3.data.TodoDatabase
import com.example.cleantodoapp3.data.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TodoDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: TodoDatabase
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "taskflow-test-${UUID.randomUUID()}"
        database = Room.databaseBuilder(context, TodoDatabase::class.java, databaseName).build()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun independentFieldUpdatesPreserveLatestValues() = runBlocking {
        val dao = database.todoDao()
        dao.insert(TodoEntity(title = "Original"))
        val id = dao.getAllTodos().first().single().id

        assertEquals(1, dao.setDone(id, true))
        assertEquals(1, dao.rename(id, "Renamed"))
        assertEquals(TodoEntity(id, "Renamed", true), dao.getAllTodos().first().single())

        assertEquals(1, dao.setDone(id, false))
        assertEquals(TodoEntity(id, "Renamed", false), dao.getAllTodos().first().single())
    }

    @Test
    fun insertionOrderIdsAndMissingRowsAreHandled() = runBlocking {
        val dao = database.todoDao()
        dao.insert(TodoEntity(title = "First"))
        dao.insert(TodoEntity(title = "Second"))
        val tasks = dao.getAllTodos().first()
        assertEquals(listOf("First", "Second"), tasks.map(TodoEntity::title))
        assertTrue(tasks[0].id < tasks[1].id)
        assertEquals(0, dao.rename(-1, "Missing"))
        assertEquals(0, dao.setDone(-1, true))
        assertEquals(0, dao.delete(-1))

        assertEquals(1, dao.delete(tasks[0].id))
        assertEquals("Second", dao.getAllTodos().first().single().title)
    }

    @Test
    fun tasksSurviveClosingAndReopeningDatabase() = runBlocking {
        database.todoDao().insert(TodoEntity(title = "Saved task", isDone = true))
        database.close()
        database = Room.databaseBuilder(context, TodoDatabase::class.java, databaseName).build()

        val restored = database.todoDao().getAllTodos().first().single()
        assertEquals("Saved task", restored.title)
        assertTrue(restored.isDone)
    }
}

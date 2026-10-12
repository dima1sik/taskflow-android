package com.example.cleantodoapp3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM my_todos ORDER BY id ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(todo: TodoEntity)

    @Query("UPDATE my_todos SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(id: Int, isDone: Boolean): Int

    @Query("UPDATE my_todos SET title = :title WHERE id = :id")
    suspend fun rename(id: Int, title: String): Int

    @Query("DELETE FROM my_todos WHERE id = :id")
    suspend fun delete(id: Int): Int
}

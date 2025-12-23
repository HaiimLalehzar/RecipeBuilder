package com.example.recipebuilder.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val audioPath: String? = null,
    val tiktokUrl: String? = null,
    val transcription: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)



package com.example.recipebuilder.ResponseModel

data class AnalyzedInstruction(
    val name: String,
    val steps: List<Step>
)
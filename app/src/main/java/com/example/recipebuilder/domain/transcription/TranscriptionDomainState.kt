package com.example.recipebuilder.domain.transcription

import com.example.recipebuilder.AudioTranscriptionPipeline.Instruction
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineResult
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineStage

data class TranscriptionDomainState(
    val stage: PipelineStage = PipelineStage.IDLE,
    val rawText: String = "",
    val sentences: List<String> = emptyList(),
    val pipelineResults: List<PipelineResult> = emptyList(),
    val instructions: List<Instruction> = emptyList(),
    val error: String? = null
)

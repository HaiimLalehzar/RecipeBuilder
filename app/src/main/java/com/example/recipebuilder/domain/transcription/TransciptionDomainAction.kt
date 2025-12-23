package com.example.recipebuilder.domain.transcription

import com.example.recipebuilder.AudioTranscriptionPipeline.Instruction
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineResult
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineStage
import java.io.File

sealed interface TranscriptionDomainAction {

    data class Start(val file: File) : TranscriptionDomainAction

    data class SetStage(val stage: PipelineStage) : TranscriptionDomainAction

    // streaming from Whisper / assembler
    data class WhisperText(val text: String) : TranscriptionDomainAction
    data class Sentence(val text: String) : TranscriptionDomainAction

    // models + parser
    data class ModelResults(val results: List<PipelineResult>) : TranscriptionDomainAction
    data class Parsed(val instructions: List<Instruction>) : TranscriptionDomainAction

    data class Error(val message: String) : TranscriptionDomainAction
}

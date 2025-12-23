package com.example.recipebuilder.domain.transcription

import com.example.recipebuilder.AudioTranscriptionPipeline.*
import com.example.recipebuilder.AudioTranscriptionPipeline.Whisper.WhisperEngine
import com.example.recipebuilder.AudioTranscriptionPipeline.Whisper.WhisperSentenceAssembler
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@ViewModelScoped
class TranscriptionDomainFlow @Inject constructor(
    private val whisperEngine: WhisperEngine,
    private val assembler: WhisperSentenceAssembler,
    private val runner: TranscriptionModelRunner
) {

    private val _state = MutableStateFlow(TranscriptionDomainState())
    val state: StateFlow<TranscriptionDomainState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun dispatch(action: TranscriptionDomainAction) {
        _state.update { transcriptionReducer(it, action) }

        when (action) {
            is TranscriptionDomainAction.Start -> {
                runFullPipeline(action.file)
            }
            else -> Unit
        }
    }

    fun start(file: File) {
        dispatch(TranscriptionDomainAction.Start(file))
    }

    private fun runFullPipeline(file: File) = scope.launch {
        try {
            // ----------------------
            // Stage 1 — Whisper + Assembler (TRANSCRIBING)
            // ----------------------
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.TRANSCRIBING))

            // clear assembler internal state if you have a reset() method
            // assembler.reset()  // only if you implemented it

            coroutineScope {
                // stream raw Whisper output
                val rawJob = launch {
                    whisperEngine.rawWhisperFlow.collect { raw ->
                        when (raw) {
                            "[CHUNK_START]" -> {
                                // just log if you want
                            }
                            "[CHUNK_END]" -> {
                                assembler.flushPartial()
                            }
                            "[AUDIO_DONE]" -> {
                                assembler.flushAllRemaining()
                            }
                            else -> {
                                dispatch(TranscriptionDomainAction.WhisperText(raw))
                                assembler.processRawChunk(raw)
                            }
                        }
                    }
                }

                // stream assembled sentences
                val sentenceJob = launch {
                    assembler.sentences.collect { chunk ->
                        dispatch(TranscriptionDomainAction.Sentence(chunk.text))
                    }
                }

                // Kicks off Whisper (like old transcribeVideoAudio)
                whisperEngine.transcribeFile(file)

                // Wait for assembler completion (same as before)
                assembler.isCompleted.first()

                // Done with streaming
                rawJob.cancel()
                sentenceJob.cancel()
            }

            val sentences = state.value.sentences

            // ----------------------
            // Stage 2/3 — Models
            // ----------------------
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.MODEL_1))

            val pipelineResults = runner.run(sentences)  // wraps your withModel1/withModel2 logic

            dispatch(TranscriptionDomainAction.ModelResults(pipelineResults))
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.MODEL_2))

            // ----------------------
            // Stage 4 — Parser
            // ----------------------
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.PARSING))

            val instructions = buildInstructionsFromPipelineResults(pipelineResults)

            dispatch(TranscriptionDomainAction.Parsed(instructions))
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.DONE))

        } catch (e: Exception) {
            e.printStackTrace()
            dispatch(TranscriptionDomainAction.Error(e.message ?: "Unknown error"))
            dispatch(TranscriptionDomainAction.SetStage(PipelineStage.ERROR))
        }
    }
}

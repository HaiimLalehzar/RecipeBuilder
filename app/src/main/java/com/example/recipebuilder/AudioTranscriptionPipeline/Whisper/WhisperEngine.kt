package com.example.recipebuilder.AudioTranscriptionPipeline.Whisper

import android.content.Context
import android.util.Log
import com.argmaxinc.whisperkit.ExperimentalWhisperKit
import com.argmaxinc.whisperkit.WhisperKit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

@OptIn(ExperimentalWhisperKit::class)
class WhisperEngine(private val context: Context) {

    // Stream: emits [CHUNK_START], raw text lines, [CHUNK_END]
    val rawWhisperFlow: SharedFlow<String> get() = _rawFlow
    private val _rawFlow = MutableSharedFlow<String>(extraBufferCapacity = 128)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHUNK_SECONDS = 30
        private const val BYTES_PER_SAMPLE = 2
        private const val CHUNK_SIZE_BYTES =
            SAMPLE_RATE * CHUNK_SECONDS * BYTES_PER_SAMPLE
    }

    private lateinit var whisperKit: WhisperKit
    private val modelReady = CompletableDeferred<Unit>()

    init {
        scope.launch {
            try {
                whisperKit = WhisperKit.Builder()
                    .setApplicationContext(context.applicationContext)
                    .setModel(WhisperKit.Builder.OPENAI_TINY_EN)
                    .setCallback { msg, result ->
                        if (msg == WhisperKit.TextOutputCallback.MSG_TEXT_OUT) {
                            val t = result.text
                            if (t.isNotBlank()) {
                                Log.d("WhisperEngine", "RAW: $t")
                                _rawFlow.tryEmit(t)
                            }
                        }
                    }
                    .build()

                // Model load is expensive — do it ONE TIME.
                whisperKit.loadModel().collect { }
                modelReady.complete(Unit)
                Log.d("WhisperEngine", "Whisper model fully loaded.")

            } catch (e: Exception) {
                Log.e("WhisperEngine", "Model load failed", e)
                modelReady.completeExceptionally(e)
            }
        }
    }

    /**
     * Decode → split → for each chunk:
     *   [CHUNK_START]
     *   whisperKit.init()
     *   whisperKit.transcribe(chunk)
     *   [CHUNK_END]
     *   whisperKit.deinitialize()
     */
    suspend fun transcribeFile(file: File) {
        modelReady.await()

        val pcm = AndroidAudioDecoder.decodeToPCM16(file)
        if (pcm.isEmpty()) {
            Log.e("WhisperEngine", "PCM decode empty")
            return
        }

        var offset = 0
        var chunkNum = 0

        while (offset < pcm.size) {

            var end = offset + CHUNK_SIZE_BYTES
            if (end > pcm.size) end = pcm.size

            // ensure 16-bit alignment
            if (((end - offset) % 2) != 0) end -= 1
            val chunk = pcm.copyOfRange(offset, end)

            Log.d("WhisperEngine", "---- CHUNK $chunkNum start (bytes=${chunk.size}) ----")

            _rawFlow.emit("[CHUNK_START]")

            runChunk(chunk)

            _rawFlow.emit("[CHUNK_END]")

            offset = end
            chunkNum++
        }

        Log.d("WhisperEngine", "All audio chunks processed.")
    }

    /**
     * Run one whisper session for a single chunk
     */
    private suspend fun runChunk(chunk: ByteArray) {
        try {
            whisperKit.init(
                SAMPLE_RATE,
                1,  // mono
                0   // no vad
            )
            var chunked = chunk
            if (chunk.size < CHUNK_SIZE_BYTES) {
                // pad trailing PCM with zeros
                val padded = ByteArray(CHUNK_SIZE_BYTES)
                System.arraycopy(chunk, 0, padded, 0, chunk.size)
                chunked = padded
            }

            whisperKit.transcribe(chunked)

            // WhisperKit internally processes async; give a brief window

        } catch (e: Exception) {
            Log.e("WhisperEngine", "Chunk processing error", e)
        }
        finally {
            whisperKit.deinitialize()
        }

    }

    fun close() {
        try { whisperKit.deinitialize() } catch (_: Exception) {}
        scope.cancel()
    }

}

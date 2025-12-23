package com.example.recipebuilder.AudioTranscriptionPipeline.Whisper

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object AndroidAudioDecoder {

    private const val TAG = "AudioDecoder"
    private const val TARGET_RATE = 16_000
    private const val TIMEOUT_US = 10_000L   // per-buffer poll timeout only

    /**
     * Decode ANY Android-supported audio into 16kHz mono, 16-bit PCM (little endian).
     * Returns an empty ByteArray on failure.
     */
    fun decodeToPCM16(inputFile: File): ByteArray {
        val extractor = MediaExtractor()
        val outStream = ByteArrayOutputStream()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(inputFile.absolutePath)

            // 1. Find first audio track
            val (trackIndex, inputFormat) = findAudioTrack(extractor)
                ?: return emptyByte("No audio track found in ${inputFile.absolutePath}")

            extractor.selectTrack(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return emptyByte("MIME type missing in audio track")

            // Ask for PCM16 output if possible (hint only)
            try {
                inputFormat.setInteger(
                    MediaFormat.KEY_PCM_ENCODING,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            } catch (_: Exception) { /* best effort only */ }

            // 2. Configure decoder
            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            var sawInputEOS = false
            var sawOutputEOS = false

            Log.d(TAG, "Input format: $sampleRate Hz, $channelCount ch, mime=$mime")

            // 3. Decode loop — runs until decoder flags END_OF_STREAM
            while (!sawOutputEOS) {

                // Feed compressed data into decoder
                if (!sawInputEOS) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        decoder.getInputBuffer(inIndex)?.let { buf ->
                            val sampleSize = extractor.readSampleData(buf, 0)
                            if (sampleSize < 0) {
                                // no more input data
                                decoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEOS = true
                                Log.d(TAG, "Queued input EOS")
                            } else {
                                decoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                // Read decoded PCM from decoder
                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

                when {
                    // We have decoded output
                    outIndex >= 0 -> {
                        val outputBuffer = decoder.getOutputBuffer(outIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {

                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            val pcmBytes = when (pcmEncoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> {
                                    val floatCount = bufferInfo.size / 4
                                    floatToPcm16(outputBuffer, floatCount)
                                }
                                else -> {
                                    ByteArray(bufferInfo.size).also { outputBuffer.get(it) }
                                }
                            }

                            if (pcmBytes.isNotEmpty()) {
                                outStream.write(pcmBytes)
                            }
                        }

                        decoder.releaseOutputBuffer(outIndex, false)

                        // Decoder signals final output buffer
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            Log.d(TAG, "Decoder signaled OUTPUT EOS")
                            sawOutputEOS = true
                        }
                    }

                    // Output format changed (usually once, after first decode)
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val fmt = decoder.outputFormat
                        sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmEncoding =
                            if (fmt.containsKey(MediaFormat.KEY_PCM_ENCODING))
                                fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            else
                                AudioFormat.ENCODING_PCM_16BIT

                        Log.d(
                            TAG,
                            "Output format: $sampleRate Hz, $channelCount ch, enc=$pcmEncoding"
                        )
                    }

                    // No output right now — loop continues
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // Do nothing; this just means "no buffer ready yet".
                        // We keep looping until decoder eventually outputs EOS.
                    }
                }
            }

            // 4. Done decoding; shutdown decoder
            decoder.stop()
            decoder.release()
            decoder = null

            val pcm = outStream.toByteArray()
            if (pcm.isEmpty()) return emptyByte("Decoded PCM is empty")

            // If already 16k mono PCM16, we're done
            val alreadyOk =
                sampleRate == TARGET_RATE &&
                        channelCount == 1 &&
                        pcmEncoding == AudioFormat.ENCODING_PCM_16BIT

            return if (alreadyOk) {
                pcm
            } else {
                Log.d(TAG, "Resampling $sampleRate Hz, $channelCount ch → $TARGET_RATE Hz mono")
                resampleAndMixDown(pcm, sampleRate, channelCount)
            }

        } catch (t: Throwable) {
            Log.e(TAG, "Decoder error", t)
            return ByteArray(0)
        } finally {
            runCatching { extractor.release() }
            runCatching { decoder?.release() }
        }
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun emptyByte(reason: String): ByteArray {
        Log.e(TAG, reason)
        return ByteArray(0)
    }

    private fun findAudioTrack(extractor: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                return i to fmt
            }
        }
        return null
    }

    /**
     * Convert float PCM [-1, 1] to 16-bit PCM LE bytes.
     */
    private fun floatToPcm16(src: ByteBuffer, floatCount: Int): ByteArray {
        src.order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(floatCount)
        src.asFloatBuffer().get(floats)

        val shorts = ShortArray(floatCount)
        for (i in floats.indices) {
            val f = floats[i].coerceIn(-1f, 1f)
            shorts[i] = (f * Short.MAX_VALUE).roundToInt().toShort()
        }

        return ByteArray(shorts.size * 2).also { out ->
            ByteBuffer.wrap(out)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .put(shorts)
        }
    }

    /**
     * Mix to mono + linear resample to 16kHz. Expects PCM16 LE input.
     */
    private fun resampleAndMixDown(
        pcm16: ByteArray,
        sourceRate: Int,
        channelCount: Int
    ): ByteArray {
        if (pcm16.isEmpty() || sourceRate <= 0) return ByteArray(0)

        // Bytes → shorts
        val samples = ShortArray(pcm16.size / 2)
        ByteBuffer.wrap(pcm16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(samples)

        // Mix to mono (interleaved samples)
        val mono: ShortArray = if (channelCount <= 1) {
            samples
        } else {
            val perChannel = samples.size / channelCount
            ShortArray(perChannel) { i ->
                var sum = 0
                repeat(channelCount) { ch ->
                    sum += samples[i * channelCount + ch].toInt()
                }
                (sum / channelCount).toShort()
            }
        }

        // If already the right sample rate, just pack and return
        if (sourceRate == TARGET_RATE) {
            return ByteArray(mono.size * 2).also { out ->
                ByteBuffer.wrap(out)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .put(mono)
            }
        }

        // Linear resample mono[] from sourceRate → TARGET_RATE
        val ratio = sourceRate.toDouble() / TARGET_RATE.toDouble()
        val outLen = (mono.size / ratio).roundToInt().coerceAtLeast(1)
        val out = ShortArray(outLen)

        for (i in 0 until outLen) {
            val pos = i * ratio
            val idx = pos.toInt()
            val frac = pos - idx

            val s1 = mono.getOrElse(idx) { 0 }.toDouble()
            val s2 = mono.getOrElse(idx + 1) { mono.getOrElse(idx) { 0 } }.toDouble()

            val value = (s1 * (1.0 - frac) + s2 * frac)
                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())

            out[i] = value.roundToInt().toShort()
        }

        return ByteArray(out.size * 2).also { outBytes ->
            ByteBuffer.wrap(outBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .put(out)
        }
    }
}

package com.example.recipebuilder.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebuilder.AudioTranscriptionPipeline.*
import com.example.recipebuilder.AudioTranscriptionPipeline.Whisper.WhisperEngine
import com.example.recipebuilder.AudioTranscriptionPipeline.Whisper.WhisperSentenceAssembler
import com.example.recipebuilder.db.RecipeDatabase
import com.example.recipebuilder.model.Recipe
import com.example.recipebuilder.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle.MobileBertTokenizerFactory
import com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle.AndroidTokenizer


class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    // --------------------------------------------------------------------
    //  WHISPER + ASSEMBLER
    // --------------------------------------------------------------------
    private val whisperEngine = WhisperEngine(getApplication())
    private val assembler = WhisperSentenceAssembler()

    // --------------------------------------------------------------------
    //  PIPELINE STAGE (for loading screen)
    // --------------------------------------------------------------------
    private val _pipelineStage = MutableStateFlow(PipelineStage.IDLE)
    val pipelineStage: StateFlow<PipelineStage> = _pipelineStage.asStateFlow()

    // --------------------------------------------------------------------
    //  LOADERS FOR ASSETS (VOCAB + LABEL MAPS + MODELS)
    // --------------------------------------------------------------------

    private fun loadAssetString(name: String): String {
        val ctx = getApplication<Application>()
        return ctx.assets.open(name).use { input ->
            input.readBytes().toString(Charset.forName("UTF-8"))
        }
    }

    private fun loadVocab(assetName: String): Map<String, Int> {
        val text = loadAssetString(assetName)
        val map = mutableMapOf<String, Int>()
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEachIndexed { index, token ->
                map[token] = index
            }
        return map
    }
    private fun copyAssetToFile(assetName: String, outputFile: File) {
        val ctx = getApplication<Application>()

        // Already exists → skip for speed
        if (outputFile.exists()) return

        ctx.assets.open(assetName).use { input ->
            outputFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun prepareOnnxModel(folder: String, onnxAsset: String, dataAsset: String?): File {
        val ctx = getApplication<Application>()
        val modelDir = File(ctx.cacheDir, folder)
        if (!modelDir.exists()) modelDir.mkdirs()

        val onnxFile = File(modelDir, onnxAsset)
        val dataFile = dataAsset?.let { File(modelDir, it) }

        // Copy .onnx
        copyAssetToFile(onnxAsset, onnxFile)

        // Copy .data (only if present)
        if (dataAsset != null) {
            copyAssetToFile(dataAsset, dataFile!!)
        }

        return onnxFile   // ONNX Runtime auto-loads the .data if exists
    }


    /** JSON: { "0": "O", "1": "B-INGREDIENT", ... } */
    private fun loadId2LabelMap(assetName: String): Map<Int, String> {
        val jsonStr = loadAssetString(assetName)
        val obj = JSONObject(jsonStr)
        val map = mutableMapOf<Int, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val id = k.toIntOrNull() ?: continue
            val label = obj.getString(k)
            map[id] = label
        }
        return map
    }

    /** JSON: { "O": 0, "B-INGREDIENT": 1, ... } */
    private fun loadLabelToIdMap(assetName: String): Map<String, Int> {
        val jsonStr = loadAssetString(assetName)
        val obj = JSONObject(jsonStr)
        val map = mutableMapOf<String, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val id = obj.getInt(k)
            map[k] = id
        }
        return map
    }

    // --------------------------------------------------------------------
    //  TEMP MODEL FILE HELPERS (ONE MODEL AT A TIME)
    // --------------------------------------------------------------------

    private fun prepareModel1File(): File {
        return prepareOnnxModel(
            folder = "onnx_m1",
            onnxAsset = "model1_dynamic.onnx",
            dataAsset = "model1_dynamic.onnx.data"
        )
    }


    private fun deleteModel1File() {
        val ctx = getApplication<Application>()
        val modelDir = File(ctx.cacheDir, "onnx_m1")
        if (modelDir.exists()) {
            modelDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun prepareModel2File(): File {
        return prepareOnnxModel(
            folder = "onnx_m2",
            onnxAsset = "model2_dynamic.onnx",   // ← quantized OR not, your choice
            dataAsset = "model2_dynamic.onnx.data"
        )
    }


    private fun deleteModel2File() {
        val ctx = getApplication<Application>()
        val modelDir = File(ctx.cacheDir, "onnx_m2")
        if (modelDir.exists()) {
            modelDir.listFiles()?.forEach { it.delete() }
        }
    }


    // --------------------------------------------------------------------
    //  TOKENIZERS
    // --------------------------------------------------------------------
    private val tokenizerM1: AndroidTokenizer by lazy {
        MobileBertTokenizerFactory(getApplication(), "vocab_m1.txt")
            .loadTokenizer()
    }

    private val tokenizerM2: AndroidTokenizer by lazy {
        MobileBertTokenizerFactory(getApplication(), "vocabm2.txt")
            .loadTokenizer()
    }


    // --------------------------------------------------------------------
    //  FLOWS EXPOSED TO THE UI
    // --------------------------------------------------------------------

    // 1) RAW WHISPER TEXT (debug / streaming view)
    private val _rawTranscriptionText = MutableStateFlow("")
    val rawTranscriptionText: StateFlow<String> = _rawTranscriptionText.asStateFlow()

    // 2) COMPLETED SENTENCES (just the cleaned strings)
    private val _completedSentences = MutableStateFlow<List<String>>(emptyList())
    val completedSentences: StateFlow<List<String>> = _completedSentences.asStateFlow()

    // 3) FINAL INSTRUCTIONS FROM THE FULL PIPELINE
    private val _finalInstructions = MutableStateFlow<List<Instruction>>(emptyList())
    val finalInstructions: StateFlow<List<Instruction>> = _finalInstructions.asStateFlow()

    // --------------------------------------------------------------------
    //  RECIPE DATABASE
    // --------------------------------------------------------------------
    private val repo = RecipeRepository(
        dao = RecipeDatabase.getDatabase(application).recipeDao()
    )

    private val _uploadStatus = MutableStateFlow("")
    val uploadStatus: StateFlow<String> = _uploadStatus.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    private val _selectedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val selectedRecipes: StateFlow<List<Recipe>> = _selectedRecipes.asStateFlow()


    fun setSelectedRecipe(recipe: Recipe?) {
        _selectedRecipe.value = recipe
    }


    // --------------------------------------------------------------------
    //  INIT — wire Whisper → Assembler → UI (no ONNX here)
    // --------------------------------------------------------------------
    init {
        loadRecipes()

        viewModelScope.launch(Dispatchers.IO) {
            whisperEngine.rawWhisperFlow.collect { raw ->

                // Debug UI
                _rawTranscriptionText.value =
                    if (_rawTranscriptionText.value.isEmpty()) raw
                    else "${_rawTranscriptionText.value}\n$raw"

                when (raw) {

                    "[CHUNK_START]" -> {
                        Log.d("VM", "Whisper chunk started")
                    }

                    "[CHUNK_END]" -> {
                        Log.d("VM", "Whisper chunk ended — flushing partial")
                        assembler.flushPartial()
                    }

                    "[AUDIO_DONE]" -> {
                        Log.d("VM", "Whisper ALL DONE → final flush")
                        assembler.flushAllRemaining()
                    }

                    else -> {
                        assembler.processRawChunk(raw)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            assembler.sentences.collect { chunk ->
                _completedSentences.value += chunk.text
            }
        }
    }


    // --------------------------------------------------------------------
    //  DATABASE METHODS
    // --------------------------------------------------------------------
    private fun loadRecipes() {
        viewModelScope.launch {
            _recipes.value = repo.getAllRecipes()
        }
    }

    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            _uploadStatus.value = "...uploading"
            repo.addRecipe(recipe)
            loadRecipes()
            _uploadStatus.value = "Successfully Saved!"
        }
    }

    fun updateRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repo.updateRecipe(recipe)
            loadRecipes()
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repo.deleteRecipe(recipe)
            loadRecipes()
        }
    }

    fun getRecipeById(id: UUID): Recipe? {
        return recipes.value.find { it.id == id }
    }

    // --------------------------------------------------------------------
    //  MODEL HELPERS — load, run, unload
    // --------------------------------------------------------------------

    private suspend fun <T> withModel1(block: suspend (OnnxModel1) -> T): T {
        val modelFile = prepareModel1File()
        val id2label = loadId2LabelMap("m1_id2label.json")

        val model = OnnxModel1(
            modelPath = modelFile.absolutePath,
            tokenizer = tokenizerM1,
            id2label = id2label
        )

        return try {
            block(model)
        } finally {
            model.close()
            deleteModel1File()
        }
    }

    private suspend fun <T> withModel2(block: suspend (OnnxModel2) -> T): T {
        val modelFile = prepareModel2File()
        val id2labelM2 = loadId2LabelMap("m2_id2label.json")
        val m1LabelToId = loadLabelToIdMap("m1_label_to_id.json")

        val model = OnnxModel2(
            modelPath = modelFile.absolutePath,
            tokenizer = tokenizerM2,
            id2labelM2 = id2labelM2,
            m1LabelToId = m1LabelToId
        )

        return try {
            block(model)
        } finally {
            model.close()
            deleteModel2File()
        }
    }

    // Small struct used just inside the VM
    private data class SentenceM1Labels(
        val words: List<String>,
        val m1Labels: List<String>
    )

    // --------------------------------------------------------------------
    //  RUN FULL END-TO-END PIPELINE (ONE MODEL AT A TIME)
    // --------------------------------------------------------------------
    fun transcribeVideoAudio(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _pipelineStage.value = PipelineStage.TRANSCRIBING
                _rawTranscriptionText.value = ""
                _completedSentences.value = emptyList()
                _finalInstructions.value = emptyList()

                Log.d("VM", "### STAGE 1 — START WHISPER ###")

                // 1) Run Whisper on all chunks
                whisperEngine.transcribeFile(file)
                Log.d("VM", "### STAGE 1 — WHISPER DONE ###")


                // 2) Wait for completion signal
                assembler.isCompleted.first()

                // 2) WAIT FOR ASSEMBLER TO FINISH
                Log.d("VM", "Waiting for assembler to complete...")
                assembler.isCompleted.first()   // suspends until final signal
                Log.d("VM", "### ASSEMBLER FINISHED ###")

                val sentences = _completedSentences.value
                Log.d("VM", "Assembler produced ${sentences.size} sentences")

                // 3) RUN MODEL-1
                _pipelineStage.value = PipelineStage.MODEL_1
                Log.d("VM", "### STAGE 2 — MODEL-1 NER ###")

                val m1Results = withModel1 { model1 ->
                    sentences.mapIndexed { index, sent ->
                        val words = tokenizerM1.splitWords(sent)
                        Log.d("VM", "[M1] Sentence $index words=$words")

                        val m1 = model1.predict(words)
                        Log.d("VM", "[M1] Sentence $index labels=$m1")

                        SentenceM1Labels(words, m1)
                    }
                }

                // 4) RUN MODEL-2
                _pipelineStage.value = PipelineStage.MODEL_2
                Log.d("VM", "### STAGE 3 — MODEL-2 GROUPING ###")

                val pipelineResults = withModel2 { model2 ->
                    m1Results.mapIndexed { index, item ->
                        val m2 = model2.predict(item.words, item.m1Labels)
                        Log.d("VM", "[M2] Sentence $index output=$m2")

                        PipelineResult(
                            words = item.words,
                            m1Labels = item.m1Labels,
                            m2Labels = m2,
                            sentenceIndex = index
                        )
                    }
                }

                // 5) PARSER
                _pipelineStage.value = PipelineStage.PARSING
                Log.d("VM", "### STAGE 4 — PARSE ###")

                val instructions = buildInstructionsFromPipelineResults(pipelineResults)
                _finalInstructions.value = instructions

                _pipelineStage.value = PipelineStage.DONE

                Log.d("VM", "### PIPELINE COMPLETE ###")

            } catch (e: Exception) {
                e.printStackTrace()
                _rawTranscriptionText.value = "Error: ${e.message}"
                _pipelineStage.value = PipelineStage.ERROR
            }
        }
    }


    // --------------------------------------------------------------------
    // CLEANUP
    // --------------------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        try {
            whisperEngine.close()
        } catch (_: Exception) { }
    }
}
# RecipeBuilder

RecipeBuilder is an Android app that converts cooking audio/video into a structured recipe (ingredients + steps + prep + conditions/tips). It runs a multi-stage on-device pipeline: speech-to-text transcription, token labeling, clause grouping, and a final rule-based parser that produces clean recipe structure.

## Key Features
- On-device transcription (chunked audio → sentences)
- Multi-stage NLP pipeline (Model-1 token labels → Model-2 clause groups → parser)
- Offline-first workflow (no required backend)
- Saves structured recipes locally

## Download (APK)
- [Download APK](./apk/RecipeBuilder-debug.apk)

## Documentation
- [System & Technical Design](./docs/SYSTEM_DESIGN.md)

## Tech Stack (high level)
- Android (Kotlin, Jetpack Compose)
- Coroutines + Flow
- ONNX Runtime (on-device inference)
- Local persistence (Room)

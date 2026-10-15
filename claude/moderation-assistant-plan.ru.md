# Помощник модератора на ИИ

Этап 3 общего плана AI-функций (`~/Personal/OurMemory-ios/claude/ai-features-plan.ru.md`). Промпт и схема ответа совпадают с iOS дословно.

## 1. Анализ заявки через Gemini

### Почему

Модератор вручную сверяет присланный текст с карточкой, перепечатывает документы с фото и пишет ответ автору. Gemini делает черновик всего этого за один запрос; решение одобрить или отклонить остаётся за человеком, в базу ничего не пишется.

### Код

```kotlin
private val model = firebaseAi.generativeModel(
    modelName = modelName,
    generationConfig = generationConfig {
        temperature = TEMPERATURE
        responseMimeType = JSON_MIME_TYPE
        responseSchema = Schema.obj(
            mapOf(
                "summary" to Schema.string(),
                "suggested_text" to Schema.string(),
                "conflicts" to Schema.array(Schema.string()),
                "duplicate_of" to Schema.string(),
                "document_transcripts" to Schema.array(Schema.string()),
                "suggested_reply" to Schema.string()
            )
        )
    },
    systemInstruction = content { text(SYSTEM_INSTRUCTION) }
)
```

Фото заявки (не больше 4) скачиваются и уменьшаются до ~1600 px (`ReviewPhotoLoader`), ответ разбирается Gson в `SubmissionAnalysisDto` → `SubmissionAnalysis`.

## 2. Экран проверки заявки

### Почему

Модератору нужна кнопка запуска, индикатор, результат и возможность одним нажатием подставить исправленный текст и ответ автору.

### Код

```kotlin
sealed interface SubmissionReviewUiIntent {
    data class OnAnalyzeClick(val rewardNames: Map<Int, String>) : SubmissionReviewUiIntent
    data object OnUseSuggestedTextClick : SubmissionReviewUiIntent
    data object OnUseSuggestedReplyClick : SubmissionReviewUiIntent
}
```

Названия наград передаются из UI (как подпись фото при одобрении), потому что строковые ресурсы есть только в UI-слое. Блок `SubmissionAnalysisSection`: подсказка и кнопка `analyze_with_ai` → `analyzing` → резюме, `possible_duplicate_msg`, `possible_conflicts`, `document_transcripts`, `suggested_text` / `use_suggested_text`, `suggested_reply` / `use_suggested_reply`, `ai_can_make_mistakes_msg`; при ошибке — `failed_to_analyze_msg`.

## Проверка

- `./gradlew assembleDebug` и `./gradlew detektAll`.
- Под админом открыть заявку с текстом и фото документа: анализ возвращает резюме и расшифровку; «Подставить текст» и «Подставить ответ» меняют поля; одобрение работает как раньше.
- Без интернета показывается `failed_to_analyze_msg`, повторное нажатие запускает анализ снова.

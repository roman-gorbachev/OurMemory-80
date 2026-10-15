package com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote

import android.graphics.Bitmap
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

class SubmissionAnalysisRemoteDataSourceImpl(
    firebaseAi: FirebaseAI,
    modelName: String
) : SubmissionAnalysisRemoteDataSource {

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

    override suspend fun analyze(prompt: String, photos: List<Bitmap>): String {
        val response = model.generateContent(
            content {
                photos.forEach { image(it) }
                text(prompt)
            }
        )
        return response.text?.takeIf { it.isNotBlank() } ?: error("Empty analysis response")
    }

    companion object {
        private const val TEMPERATURE = 0.2f
        private const val JSON_MIME_TYPE = "application/json"
        private val SYSTEM_INSTRUCTION = """
            Ты помогаешь модератору мемориального приложения о ветеранах, похороненных на военном кладбище в Минске. Посетители присылают воспоминания и фотографии для карточки ветерана. Проанализируй заявку и верни JSON:
            summary — одно-два предложения по-русски о том, что содержит заявка;
            suggested_text — текст заявки с исправленными орфографией, пунктуацией и стилем на языке оригинала, без изменения, удаления и добавления фактов;
            conflicts — расхождения заявки с карточкой (даты, награды, места, имена) по-русски, пустой список, если их нет;
            duplicate_of — дословный фрагмент материалов карточки, который заявка повторяет, или пустая строка;
            document_transcripts — дословная расшифровка текста документов, писем и наградных листов на фотографиях, по одному элементу на документ, пустой список, если документов нет;
            suggested_reply — короткий вежливый ответ автору на языке заявки, без обещаний одобрения.
            Никогда не придумывай факты, которых нет в заявке, карточке или на фотографиях.
        """.trimIndent()
    }
}

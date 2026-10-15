package com.gorman.ourmemoryapp.data.inscription.datasource.remote

import android.graphics.BitmapFactory
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content

class InscriptionRemoteDataSourceImpl(
    private val model: GenerativeModel
) : InscriptionRemoteDataSource {

    override suspend fun recognize(jpeg: ByteArray): String {
        val photo = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            ?: throw IllegalArgumentException("Photo cannot be decoded")
        val response = model.generateContent(
            content {
                image(photo)
                text(PROMPT)
            }
        )
        return response.text.orEmpty()
    }

    companion object {
        private const val PROMPT = "На фото надгробие или мемориальная плита. " +
            "Перепиши дословно все фамилии, имена, отчества и годы, которые видны на надписи, " +
            "по одному человеку на строку. Ничего не добавляй и не объясняй. " +
            "Если надписи с именами нет, верни пустой ответ."
    }
}

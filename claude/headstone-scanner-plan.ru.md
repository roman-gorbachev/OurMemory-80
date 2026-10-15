# Свой сканер: QR-код и имя на плите

Часть общего плана AI-функций (`~/Personal/OurMemory-ios/claude/ai-features-plan.ru.md`, этап 1). iOS-версия уже сделана: один экран камеры ловит QR и текст, имя сопоставляется с ветеранами и показываются кандидаты.

## 1. Свой экран камеры вместо сканера Google

### Почему

`rememberQrScanAction` открывает системный экран `GmsBarcodeScanning`: к кадру доступа нет, поэтому нельзя ни распознать имя на плите, ни показать кандидатов поверх камеры. Нужен свой экран на CameraX, где QR распознаётся вживую (ML Kit `barcode-scanning`, без Google Play-модуля), а плиту можно сфотографировать кнопкой.

### Код

```kotlin
val controller = remember {
    LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        setImageAnalysisAnalyzer(executor, QrCodeAnalyzer(onQrScanned))
    }
}
AndroidView(factory = { PreviewView(it).apply { this.controller = controller } })
```

```kotlin
class QrCodeAnalyzer(private val onQrScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onQrScanned) }
            .addOnCompleteListener { imageProxy.close() }
    }
}
```

Маршрут `Screen.ScannerScreen` («scanner»), открывается из шапки списка ветеранов и из «Ещё». `QrScanAction.kt` и `play-services-code-scanner` удаляются; в манифест добавляется `CAMERA` и `uses-feature android.hardware.camera` с `required="false"`.

## 2. Распознавание надписи через Gemini

### Почему

ML Kit на устройстве не понимает кириллицу. Надпись на плите распознаёт Gemini через Firebase AI Logic (провайдер Agent Platform): по нажатию «Сфотографировать плиту» кадр уменьшается до 1600 px, сжимается в JPEG и отправляется в модель. Стоимость ~$0,001 за фото, нужен интернет.

### Код

```kotlin
class InscriptionRemoteDataSourceImpl @Inject constructor(
    private val model: GenerativeModel
) : InscriptionRemoteDataSource {
    override suspend fun recognize(jpeg: ByteArray): String {
        val photo = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        return response.text.orEmpty()
    }
}
```

```kotlin
@Provides
@Singleton
fun provideGenerativeModel(): GenerativeModel = Firebase.ai(backend = GenerativeBackend.agentPlatform())
    .generativeModel(MODEL_NAME, generationConfig { temperature = TEMPERATURE })
```

## 3. Сопоставление имени с ветеранами

### Почему

Gemini возвращает текст надписи, а не id ветерана. Нужен тот же алгоритм, что на iOS (`VeteranNameMatcher.swift`), чтобы обе платформы находили одинаковых кандидатов: нормализация (регистр, `ё→е`, латинские двойники кириллицы), совпадение фамилии (3 балла), имени и отчества (по 2) с допуском опечаток по расстоянию Левенштейна, плюс 1 балл за каждый совпавший год; кандидат — от 3 баллов, не больше трёх.

### Код

```kotlin
object VeteranNameMatcher {
    fun candidates(recognizedText: String, veterans: List<Veteran>): List<Veteran> {
        val words = normalizedWords(recognizedText)
        val years = words.filter(::isYear).toSet()
        val names = words.filter { !isYear(it) && it.length >= MIN_WORD_LENGTH }
        if (names.isEmpty()) return emptyList()
        return veterans
            .map { it to nameScore(it, names) + yearScore(it, years) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_CANDIDATES)
            .map { (veteran, _) -> veteran }
    }
}
```

## 4. Экран

### Почему

Посетитель должен видеть подсказку, ход распознавания и кандидатов прямо поверх камеры, а QR — по-прежнему сразу открывать карточку.

### Код

```kotlin
data class ScannerUiState(
    val candidates: ImmutableList<Veteran> = persistentListOf(),
    val status: InscriptionStatus = InscriptionStatus.IDLE
)
```

```kotlin
sealed interface ScannerUiIntent {
    class OnPhotoTaken(val jpeg: ByteArray) : ScannerUiIntent
}
```

Нижняя панель: подсказка `point_camera_at_qr_or_name_msg` → `recognizing_name` с индикатором → список `matching_veterans` (общий `VeteranItem`, вынесенный из `HomeScreen` в `ui/common/ui`) или `no_matching_veterans_msg` / `failed_to_recognize_name_msg`; под ней кнопка `photograph_headstone`. Без разрешения камеры — `camera_permission_needed_msg` и кнопка `allow_camera_access`.

## Проверка

- `./gradlew assembleDebug` и `./gradlew detektAll`.
- На устройстве: QR нашей карточки открывает ветерана; чужой QR показывает `not_our_qr_code_msg`; отказ в разрешении камеры показывает просьбу и кнопку; фото плиты (или экрана с именем) даёт кандидатов, нажатие открывает карточку; без интернета — `failed_to_recognize_name_msg`.
- Пока App Check не включён (этап 2 общего плана), запросы к модели не защищены — до релиза обязательно включить.

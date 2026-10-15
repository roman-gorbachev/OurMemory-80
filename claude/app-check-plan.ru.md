# App Check для Firebase AI Logic

Этап 2 общего плана AI-функций (`~/Personal/OurMemory-ios/claude/ai-features-plan.ru.md`).

## 1. Провайдер App Check по типу сборки

### Почему

Без App Check любой, у кого есть `google-services.json`, может слать запросы к Gemini за наш счёт. В консоли App Check зарегистрирован (Play Integrity); приложение должно прикладывать токен к каждому запросу. Play Integrity не работает в отладочных сборках и эмуляторе, поэтому debug-сборка использует debug-провайдер, а его библиотека не должна попадать в release — отсюда разные source set'ы.

### Код

```kotlin
object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
```

(`src/release/`; в `src/debug/` — `DebugAppCheckProviderFactory.getInstance()`, зависимость `debugImplementation(libs.firebase.appcheck.debug)`.)

```kotlin
override fun onCreate() {
    super.onCreate()
    Firebase.appCheck.installAppCheckProviderFactory(AppCheckProviders.factory())
}
```

## 2. Одноразовые токены для AI Logic

### Почему

Ограниченные по использованию токены нельзя переиспользовать при перехвате запроса — Firebase рекомендует их для AI Logic.

### Код

```kotlin
Firebase.ai(
    backend = GenerativeBackend.agentPlatform(),
    useLimitedUseAppCheckTokens = true
)
```

## Проверка

- `./gradlew assembleDebug assembleRelease` и `./gradlew detektAll`.
- Debug-сборка печатает в logcat debug-секрет App Check; добавить его в консоли (App Check → приложение → Manage debug tokens), затем фото плиты в сканере распознаётся.
- После включения enforcement для AI Logic: release-сборка из Play (или internal testing) работает, debug-сборка без зарегистрированного токена получает ошибку.

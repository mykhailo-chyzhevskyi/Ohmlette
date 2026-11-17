# Ohmlette

Актуальний опис реалізації, API, AI та запуску: [Knowledge Base](docs/kb/README.md). Цільова архітектура й майбутні функції описані окремо; їх не слід вважати вже реалізованими.

Матеріали курсу: [звіт №1](course/lab1/main.tex), [звіт №2](course/lab2/main.tex), [план робіт](course/DEVELOPMENT_PLAN.md), [чекліст функцій](course/FEATURE_CHECKLIST.md). Backend та сценарій №2 описані в [backend/README.md](backend/README.md).

Офлайн-інструменти електронного інженера для Android. Початкова версія: перерахунок інженерних префіксів і підбір найближчих номіналів E12/E24/E96.

## Запуск

Відкрийте цю папку в Android Studio та дочекайтеся Gradle Sync. Оберіть емулятор або смартфон з Android 8.0 чи новішим і натисніть Run.

Проєкт використовує AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose compiler 2.3.10, compileSdk/targetSdk 37, minSdk 26. Потрібні SDK Platform 37 та Build Tools 36.0.0. JDK — вбудований у Android Studio.

Збирання: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` на Windows, `./gradlew` на інших системах. Локальний шлях SDK задається у local.properties і не додається до Git.

## Структура

- `Engineering.kt` — незалежні від Android розрахунки та таблиці номіналів.
- `MainActivity.kt` — головний екран і два інструменти Compose. Стан введення зберігається при повороті екрана.
- `EngineeringTest.kt` — перевірки префіксів, меж декад, номіналів і некоректного введення.

Арифметичний парсер, резисторні мережі, допуски, RC-фільтри й постійна історія ще не реалізовані. Список майбутніх модулів — у FEATURE_CHECKLIST.md у батьківській папці. Версія для ПК запланована пізніше.

## Перевірка першої версії

Debug APK успішно зібрано. Шість JVM-тестів розрахунків пройдено. Запуск і вигляд на емуляторі/смартфоні ще не перевірені.

На Windows JVM-тести можуть не знаходити класи, якщо шлях проєкту або кешу містить кирилицю. Для такого випадку можна задати `GRADLE_USER_HOME` і `OHMLETTE_BUILD_DIR` як абсолютні шляхи без кирилиці та запускати тести з `--no-daemon -Pkotlin.compiler.execution.strategy=in-process`. `OHMLETTE_BUILD_DIR` змінює каталог результатів модуля app, а не вихідний код.

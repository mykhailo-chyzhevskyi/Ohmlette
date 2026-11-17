# Реальна структура MVP

```text
Ohmlette/
├── app/src/main/java/ua/chyzhevskyi/ohmlette/
│   ├── MainActivity.kt
│   ├── Engineering.kt
│   └── datasheets/DatasheetScreen.kt
├── app/src/main/AndroidManifest.xml
├── app/src/debug/AndroidManifest.xml
├── app/src/test/java/ua/chyzhevskyi/ohmlette/EngineeringTest.kt
├── backend/
│   ├── server.py
│   ├── classifier.py
│   ├── requirements.txt
│   ├── test_backend.py
│   └── check_sample.py
└── calculations/               поки тільки папки з README
```

## Android

[MainActivity.kt](../../app/src/main/java/ua/chyzhevskyi/ohmlette/MainActivity.kt) запускає Compose. `Ohmlette()` перемикає екрани рядком home/units/resistors/datasheets. Екрани одиниць і номіналів та спільні компоненти залишаються в цьому ж файлі.

[Engineering.kt](../../app/src/main/java/ua/chyzhevskyi/ohmlette/Engineering.kt) — об'єкт без Android-залежностей: parse, convert, format, nearest та таблиці номіналів. Номінали шукаються в сусідніх декадах, повертаються три найближчі за абсолютною різницею опору.

[DatasheetScreen.kt](../../app/src/main/java/ua/chyzhevskyi/ohmlette/datasheets/DatasheetScreen.kt) зараз містить і UI, і HTTP-клієнт `createCard()`. Це ще не завершена MVVM-архітектура. Окремого ViewModel, репозиторію API, DI-контейнера чи бібліотеки навігації немає.

Запит виконується через HttpURLConnection у single-thread executor. Результат передається в UI через Handler головного потоку. Тайм-аут підключення — 10 с, читання — 90 с. Запит не скасовується при закритті екрана; прапорець alive лише не дозволяє оновити закритий UI.

Адреса, артикул і URL використовують rememberSaveable. Файл, busy, error та card — remember, тому не мають гарантованого відновлення при пересозданні екрана. Адреса додатково записується в SharedPreferences при відправленні. Збереження картки записує JSON у `filesDir/cards/`; екрана перегляду цих файлів немає.

HTTP дозволено в debug manifest. Release-версія не має цього винятку для незашифрованого HTTP. INTERNET permission є в основному manifest.

## Потік даташита

```text
DatasheetScreen
  → createCard(): артикул + URL або PDF у base64
  → POST /v1/cards
  → server.py: валідація, завантаження, pypdf
  → classifier.py: класифікація першої сторінки
  → server.py: пошук артикулу по сторінках, збереження PDF
  → JSON-картка
  → Compose: тип, оригінальні фрагменти, сторінки, обмеження
```

Посилання «Відкрити PDF» створює зовнішній ACTION_VIEW на адресу документа з `#page=N`. Перехід на конкретну сторінку залежить від програми перегляду. Картка не включає PDF-байти.

## Backend і зберігання

[server.py](../../backend/server.py) використовує ThreadingHTTPServer зі стандартної бібліотеки. Це не FastAPI/Flask. PdfReader читає текст, а Classifier визначає категорію. PDF зберігається на ПК у `backend/storage/{sha256}.pdf`. Бази даних та облікових записів немає.

Core/calculations і групи calculations ще не підключені як Gradle-модулі. Не вважати правила майбутніх залежностей уже забезпеченими збирачем.

# Запуск і перевірки

Команди нижче виконуються в PowerShell. Корінь проєкту — папка Ohmlette, не app. Працюють також із термінала VS Code.

## Android

За конфігурацією коду: AGP 9.2.1; Gradle Wrapper 9.4.1; Compose compiler 2.3.10; Compose BOM 2026.09.00; activity-compose 1.12.0. compileSdk/targetSdk 37, minSdk 26, Java target 17. Шлях SDK задається локально в local.properties.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK за типовим шляхом: `app/build/outputs/apk/debug/app-debug.apk`. Якщо задано OHMLETTE_BUILD_DIR, результати app лежать у цьому каталозі.

У Windows раніше виникали проблеми JVM test worker через кирилицю. Перевірений обхід: GRADLE_USER_HOME і OHMLETTE_BUILD_DIR з абсолютними шляхами без кирилиці, а для запуску `--no-daemon "-Pkotlin.compiler.execution.strategy=in-process"`. У проєкті вже є android.overridePathCheck=true. Не видаляти чужі кеші або змінювати системні налаштування як перший крок діагностики.

Шість EngineeringTest перевіряють: m/M і кому, mega→pico, перехід декади, номінал E96, від'ємний опір та переповнення. Вони не перевіряють мережу або Compose UI.

## Backend

З папки backend, Python 3.11+:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m unittest -v
.\.venv\Scripts\python.exe server.py --host 127.0.0.1 --port 8000
```

Чотири unit-тести перевіряють класифікацію контрольних фраз, невідомий словник, некоректний PDF і локальний URL. Це невелика перевірка поведінки, не оцінка точності на реальному корпусі даташитів.

Окремий `check_sample.py` потребує `sample.pdf` у поточній папці. Він запускає тимчасовий HTTP-сервер на вільному порту, перевіряє health, base64-upload, тип «Діод», доказ на сторінці 3, отримання PDF, відсутній артикул і HTTP 400. Він не перевіряє завантаження PDF за URL. Пише sample-card.json та PDF у storage.

Контрольний PDF: https://www.onsemi.com/pdf/datasheet/bzx84c2v4lt1-d.pdf. Артикул: BZX84C2V4LT1G. Завантажувати документ слід окремо, не змінюючи його вміст для проходження тесту.

## З'єднання емулятора

Основний варіант: `http://10.0.2.2:8000`. У перевіреному середовищі він давав тайм-аут, тому використано:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```

Після цього **в застосунку** вводиться `http://127.0.0.1:8000`. Backend залишається працювати. Для фізичного телефона через USB можна використати такий самий reverse, вибравши потрібний пристрій, якщо їх кілька. Інший варіант — LAN-адреса ПК з backend --host 0.0.0.0 у довіреній мережі.

## Регресія сценарію №2

1. Відкрити «Даташити», перевірити адресу backend.
2. Ввести контрольні URL та артикул, створити картку.
3. Переконатися, що є «Діод», «неперевірено», доказ і сторінка 3.
4. Змінити порт на 8001, повторити, отримати видиму помилку.
5. Повернути 8000 та повторно отримати картку.
6. Окремо перевіряти файл, PDF-перегляд і збереження, якщо зміни торкаються цих дій.

Кроки 1–5 уже виконано на емуляторі під час №2; це історичний результат, не новий прогін у таску створення KB. Попередні Android/HTTP-тести теж не слід називати щойно виконаними.

## Git і наступна робота

Під час початкового аудиту Git checkout не було; згодом папку підключено до origin/main репозиторію Ohmlette. Перед наступною функцією перевірити git status, отримати актуальний базовий стан і переглядати git diff. Не запускати git init у домашній папці та не робити force-push.

Ціль наступної функції №3 — кнопка перевірки backend через наявний GET /health. Її ще немає в мобільному інтерфейсі. Експеримент без KB/з KB також ще не проведено.

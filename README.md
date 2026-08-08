# ReHoster

**ReHoster** — Java-фреймворк для миграции legacy-приложений в контейнеры на основе анализа поведения программ во время выполнения.

## Возможности

- Запуск legacy-приложения как внешнего процесса
- Сбор телеметрии о запуске и окружении во время выполнения
- **Парсинг `.env` файлов проекта** (поддержка PHP, Laravel, Node.js проектов)
- **Автоматическое определение зависимостей** (MySQL, PostgreSQL, Redis, MongoDB)
- **Многораундовый анализ** с разными конфигурациями
- Построение модели зависимостей и требований к окружению
- Генерация артефактов контейнеризации: `Dockerfile`, `docker-compose.yml` и рекомендации
- Опциональный **AI-режим** — интерактивное уточнение артефактов через локальную нейросеть (LM Studio)

## Требования

- **Java 8**
- **Maven 3.6**
- **Docker**
- **LM Studio** с запущенным локальным сервером на `localhost:1234` — только для AI-режима

## Структура проекта

```bash
src/main/java/com/rehoster/
├── Main.java                    # Точка входа
├── cli/                         # Парсинг аргументов командной строки
├── orchestrator/                # Координация pipeline
├── launcher/                    # Запуск legacy-процессов
├── collector/                   # Сборщики данных
│   ├── process/                 # Сбор информации о процессах
│   ├── env/                     # Сбор переменных окружения
│   └── args/                    # Сбор аргументов запуска
├── storage/                     # Сохранение данных в JSON
├── analysis/                    # Анализ зависимостей
├── generation/                  # Генерация Dockerfile/Compose
├── model/                       # Модели данных
│   ├── run/                     # RunConfig, RunContext, LaunchResult
│   ├── snapshot/                # RuntimeSnapshot, ProcessInfo, EnvVar
│   ├── analysis/                # AppDependencyModel, EnvVarSpec
│   └── generation/              # DockerfileSpec, ComposeSpec, RunReport
└── ai/                          # AI-подсистема
    ├── client/                  # LmStudioAiClient, AiClient interface
    ├── interactive/             # UserRequirementCollector
    ├── config/                  # AiConfig, AiMode, AiProvider
    ├── prompt/                  # AiPromptBuilder
    ├── service/                 # AiArtifactRefiner, парсер, валидатор
    └── model/                   # AiRefinementRequest/Result/Outcome
```

## Сборка проекта

```bash
mvn clean package
```

После сборки JAR-файл будет находиться в `target/rehoster-1.0.0.jar`

## GitHub и CI/CD

В проект добавлена конфигурация GitHub Actions:

- **CI** — workflow `.github/workflows/ci.yml` запускается при каждом `push` в `main`/`master` и при каждом `pull_request`, собирает проект через Maven и сохраняет JAR как build artifact.
- **Release** — workflow `.github/workflows/release.yml` запускается при публикации Git-тега вида `v*`, собирает актуальный JAR, создаёт GitHub Release и прикрепляет файл `target/rehoster-<version>.jar`.

Чтобы выпустить новую версию:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Запуск

### Базовый синтаксис

```bash
java -jar target/rehoster-1.0.0.jar run [опции] -- <команда legacy-приложения>
```

### Опции

| Опция | Описание | По умолчанию |
|-------|----------|--------------|
| `-t, --timeout <секунды>` | Таймаут выполнения процесса | 60 |
| `-d, --dir <путь>` | Рабочая директория | текущая |
| `-o, --output <путь>` | Директория для результатов | `rehoster-output` |
| `-e, --env <KEY=VALUE>` | Переопределение переменной окружения | — |
| `--ai` | Включить AI-режим (требует LM Studio) | выключено |
| `--ai-mode <off\|advisory\|auto>` | Режим AI (`advisory` — не применяет изменения автоматически) | `off` |
| `--ai-model <model>` | Переопределить имя модели в LM Studio | `qwen/qwen3.5-9b` |
| `--ai-timeout <секунды>` | Таймаут AI-запроса | 120 |
| `-h, --help` | Показать справку | — |

### Примеры использования

#### 1. Анализ Java-приложения

```bash
java -jar target/rehoster-1.0.0.jar run -- java -jar myapp.jar
```

#### 2. Анализ с таймаутом и переменными окружения

```bash
java -jar target/rehoster-1.0.0.jar run -t 120 -e DB_HOST=localhost -- java -jar myapp.jar
```

#### 3. Анализ Python-скрипта

```bash
java -jar target/rehoster-1.0.0.jar run -- python app.py --port 8080
```

#### 4. Запуск с AI-режимом

```bash
java -jar target/rehoster-1.0.0.jar run --ai -- java -jar myapp.jar
```

#### 5. AI в advisory-режиме (артефакты не применяются автоматически)

```bash
java -jar target/rehoster-1.0.0.jar run --ai --ai-mode advisory -- java -jar myapp.jar
```

## AI-режим

Для работы AI-режима необходимо:

1. Установить и запустить **LM Studio**
2. Загрузить модель (рекомендуется **Qwen3-9B Q4_K_M**)
3. Запустить локальный сервер в LM Studio (порт `1234`)
4. Передать флаг `--ai` при запуске ReHoster

Если `--ai` указан, ReHoster:

1. Проверяет доступность LM Studio (`GET localhost:1234/v1/models`)
2. Если сервер недоступен — **возвращает baseline без ошибки**
3. Если доступен — предлагает пользователю ввести требования в консоли
4. Собирает контекст проекта и маскирует секреты
5. Отправляет промпт с требованием в LM Studio (`POST /v1/chat/completions`)
6. Валидирует ответ модели
7. Применяет изменения если ответ прошёл валидацию и `confidence >= 0.65`

Если AI вернул невалидный ответ или недоступен — ReHoster **автоматически откатывается к baseline**.

Все данные обрабатываются **локально**, ничего не покидает машину.

## Выходные файлы

После выполнения в директории `rehoster-output/<run-id>/` будут созданы:

| Файл | Описание |
|------|----------|
| `runtime-snapshot.json` | Сырые данные о запуске приложения |
| `analysis-result.json` | Результаты анализа зависимостей |
| `Dockerfile` | Итоговый Dockerfile |
| `docker-compose.yml` | Итоговый docker-compose |
| `.dockerignore` | Исключения для Docker build |
| `Dockerfile.ai` | Dockerfile от AI (если AI применился) |
| `docker-compose.ai.yml` | Compose от AI (если AI применился) |
| `ai-refinement.json` | Отчёт о работе AI-этапа |
| `recommendations.md` | Рекомендации по контейнеризации |
| `report.json` | Общий отчёт о выполнении |

## Пример вывода

```
=== ReHoster ===
Run ID: run-20240215-143025-a1b2c3d4
Command: php artisan serve

[1/9] Launching legacy process...
      PID: 12345
      Exit code: 0
[2/9] Collecting runtime data...
      ProcessCollector - done
      EnvironmentCollector - done
      EnvFileCollector - done
      ArgsCollector - done
[3/9] Saving runtime snapshot...
[4/9] Analyzing dependencies...
      Detected type: php
      Environment vars: 15
      Exposed ports: [8080]
[5/9] Detecting service dependencies (databases, caches)...
      Found 2 service dependency(ies):
        - mysql (mysql:8.0)
        - redis (redis:7-alpine)
[6/9] Running multi-round analysis...
[7/9] Generating container artifacts...
      .dockerignore - done
[8/9] Running AI refinement (LM Studio)...
      Connecting to LM Studio at http://localhost:1234...
      Connected. Model: qwen/qwen3.5-9b

      Enter your requirements for Dockerfile/docker-compose refinement.
      Examples: "use alpine image", "remove database services"
      > убери сервисы MySQL и Redis

      Sending to AI...
      AI refinement applied (confidence: 0.88)
      Dockerfile.ai - done
      docker-compose.ai.yml - done
      Dockerfile - done
      docker-compose.yml - done
      recommendations.md - done
[9/9] Saving report...

=== Complete ===
Output directory: rehoster-output\run-20240215-143025-a1b2c3d4
```

## Архитектура

```
CLI → Orchestrator → Launcher → Collectors → Analyzers → Generators → AI Refiner → Output
                                                               ↓
                                                   [DependencyAnalyzer]
                                                   [ServiceDependencyDetector]
                                                   [MultiRoundAnalyzer]
                                                               ↓
                                                   baseline Dockerfile + Compose
                                                               ↓
                                                   [LmStudioAiClient] ping
                                                   [UserRequirementCollector]
                                                   [AiArtifactRefiner]
                                                   POST localhost:1234
                                                               ↓
                                                   final Dockerfile + Compose
```

1. **CLI** — парсит аргументы, формирует `RunConfig`
2. **Orchestrator** — управляет 9 этапами выполнения
3. **Launcher** — запускает legacy-процесс через `ProcessBuilder`
4. **Collectors** — собирают данные (процессы, env, args, .env файлы)
5. **Analyzers** — строят модель зависимостей, определяют сервисы, многораундовый анализ
6. **Generators** — генерируют baseline Dockerfile, Compose, рекомендации
7. **AI Refiner** — при включённом `--ai` подключается к LM Studio, принимает требование пользователя, дорабатывает baseline и при ошибках автоматически откатывается

## Troubleshooting

### Сгенерирован `FROM ubuntu:22.04` вместо нужного образа

Приложение классифицировано как `generic`. Возможные причины:

- команда запуска обёрнута в оболочку (`cmd.exe /c ...`)
- `--dir` указывает не на корень проекта, где лежит `pom.xml` / `package.json`

Решение: передать корень проекта через `-d/--dir`.

### LM Studio недоступен

ReHoster возвращает baseline без ошибки. Убедитесь что:

- LM Studio запущен и модель загружена
- Локальный сервер включён (кнопка **Start Server** в LM Studio)
- Порт `1234` не занят другим процессом

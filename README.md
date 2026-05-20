# ReHoster

**ReHoster** — Java-фреймворк для миграции legacy-приложений в контейнеры на основе анализа поведения программ во время выполнения.

## Возможности

- Запуск legacy-приложения как внешнего процесса
- Сбор телеметрии о запуске и окружении во время выполнения
- **Парсинг `.env` файлов проекта** (поддержка PHP, Laravel, Node.js проектов)
- **Автоматическое определение зависимостей** (MySQL, PostgreSQL, Redis, MongoDB)
- **Многораундовый анализ** с разными конфигурациями
- Построение модели зависимостей/требований к окружению
- Генерация артефактов контейнеризации: `Dockerfile`, `docker-compose.yml` и рекомендации
- Опциональный **AI-этап refinement** для улучшения baseline `Dockerfile` и `docker-compose.yml` через OpenRouter API
- Генерация `docker-compose.yml` с **явными значениями по умолчанию** для популярных переменных (без `${VAR:-...}`); по умолчанию `DB_HOST` выставляется в `host.docker.internal` для подключения к локальной БД

## Требования

- **Java 8**
- **Maven 3.6**
- **Docker** 
- **OpenRouter API key** в переменной окружения `OPENROUTER_API_KEY` для проверки AI-режима

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
└── util/                        # Утилиты
```

## Сборка проекта

```bash
# Перейти в директорию проекта
cd ReHoster

# Собрать проект с зависимостями
mvn clean package
```

После сборки JAR-файл будет находиться в `target/rehoster-1.0.0.jar`

## GitHub и CI/CD

В проект добавлена конфигурация GitHub Actions:

- **CI** — workflow `.github/workflows/ci.yml` запускается при каждом `push` в `main`/`master` и при каждом `pull_request`, собирает проект через Maven и сохраняет JAR как build artifact.
- **Release** — workflow `.github/workflows/release.yml` запускается при публикации Git-тега вида `v*`, собирает актуальный JAR, создаёт GitHub Release и прикрепляет файл `target/rehoster-<version>.jar`.
- **Автообновление ссылки скачивания** — при каждом релизе workflow обновляет файл `DOWNLOAD.md`, записывая актуальную версию и ссылку на скачивание JAR из GitHub Releases.

Чтобы выпустить новую версию фреймворка:

```bash
git tag v1.0.0
git push origin v1.0.0
```

После выполнения release workflow актуальная ссылка будет доступна в `DOWNLOAD.md` и на странице GitHub Releases.

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
| `-e, --env <KEY=VALUE>` | Переопределение переменной окружения | - |
| `--ai` | Включить AI refinement артефактов | выключено |
| `--ai-mode <off\|advisory\|auto>` | Режим работы AI (`advisory` не применяет изменения автоматически) | `off` |
| `--ai-model <model>` | Переопределить primary AI model | `qwen/qwen3-coder:free` |
| `--ai-timeout <секунды>` | Таймаут AI-запроса | 30 |
| `--no-ai-fallback` | Отключить fallback model | fallback включён |
| `-h, --help` | Показать справку | - |

### Примеры использования

#### 1. Анализ Java-приложения

```bash
java -jar target/rehoster-1.0.0.jar run -- java -jar myapp.jar
```

#### 2. Анализ с таймаутом и переменными окружения

```bash
java -jar target/rehoster-1.0.0.jar run -t 120 -e DB_HOST=localhost -e DB_PORT=5432 -- java -jar myapp.jar
```

#### 3. Анализ Python-скрипта

```bash
java -jar target/rehoster-1.0.0.jar run -- python app.py --port 8080
```

#### 4. Анализ нативного бинарника

```bash
java -jar target/rehoster-1.0.0.jar run -o ./output -- ./myapp --config config.yaml
```

#### 5. Указание рабочей директории

```bash
java -jar target/rehoster-1.0.0.jar run -d /path/to/app -- ./start.sh
```

#### 6. Запуск с AI refinement

```bash
java -jar target/rehoster-1.0.0.jar run --ai --ai-mode auto -- java -jar myapp.jar
```

#### 7. Запуск с AI в advisory-режиме

```bash
java -jar target/rehoster-1.0.0.jar run --ai --ai-mode advisory -- java -jar myapp.jar
```

## AI refinement

Если включён `--ai`, ReHoster:

- генерирует baseline `Dockerfile` и `docker-compose.yml`
- собирает безопасный контекст выполнения и проекта
- маскирует секреты перед отправкой во внешний API
- отправляет refinement-запрос в OpenRouter
- валидирует ответ модели
- применяет результат только если он прошёл policy/validation

Если AI недоступен, вернул невалидный JSON или предложил рискованные изменения, ReHoster **автоматически откатывается к baseline-артефактам**.

### Где указать API ключ

ReHoster читает ключ из переменной окружения:

```bash
OPENROUTER_API_KEY
```

Для **Windows PowerShell** перед запуском установите ключ в текущую сессию:

```powershell
$env:OPENROUTER_API_KEY="ваш_ключ_openrouter"
```

Для постоянного значения в Windows:

```powershell
setx OPENROUTER_API_KEY "ваш_ключ_openrouter"
```

После `setx` откройте **новый** терминал/IDE session.

## Выходные файлы

После выполнения в директории `rehoster-output/<run-id>/` будут созданы:

| Файл | Описание |
|------|----------|
| `runtime-snapshot.json` | Сырые данные о запуске приложения |
| `analysis-result.json` | Результаты анализа зависимостей |
| `Dockerfile` | Сгенерированный Dockerfile |
| `docker-compose.yml` | Сгенерированный docker-compose |
| `Dockerfile.ai` | Dockerfile, предложенный AI (если AI вернул результат) |
| `docker-compose.ai.yml` | Compose, предложенный AI (если AI вернул результат) |
| `ai-refinement.json` | Подробный отчёт о работе AI-этапа |
| `recommendations.md` | Рекомендации по контейнеризации |
| `report.json` | Отчёт о выполнении |

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
[7/9] Generating baseline container artifacts...
      Dockerfile.baseline - done
      docker-compose.baseline.yml - done
[8/9] Running AI refinement...
      Dockerfile - done
      docker-compose.yml - done
      recommendations.md - done
[9/9] Saving report...

=== Complete ===
Output directory: D:\project\rehoster-output\run-20240215-143025-a1b2c3d4

Generated files:
  - .dockerignore
  - runtime-snapshot.json
  - analysis-result.json
  - ai-refinement.json
  - Dockerfile
  - docker-compose.yml
  - recommendations.md
  - report.json
```

## Архитектура

```
CLI → Orchestrator → Launcher → Collectors → Storage → Analyzers → Generators → AI Refiner → Output
                                    ↓
                         [ProcessCollector]
                         [EnvironmentCollector]
                         [EnvFileCollector]
                         [ArgsCollector]
                                    ↓
                     [DependencyAnalyzer]
                     [ServiceDependencyDetector]
                     [MultiRoundAnalyzer]
                                    ↓
                          [AiArtifactRefiner]
```

1. **CLI** — парсит аргументы, формирует `RunConfig`
2. **Orchestrator** — управляет сценарием выполнения (9 этапов при включённом AI)
3. **Launcher** — запускает legacy-процесс через `ProcessBuilder`
4. **Collectors** — собирают данные (процессы, env, args, .env файлы)
5. **Storage** — сохраняет снимки и результаты в JSON
6. **Analyzers**:
   - `DependencyAnalyzer` — строит модель зависимостей приложения
   - `ServiceDependencyDetector` — определяет внешние сервисы (БД, кеш)
   - `MultiRoundAnalyzer` — запускает приложение с разными конфигурациями
7. **Generators** — генерируют baseline Dockerfile/Compose/рекомендации
8. **AI Refiner** — безопасно дорабатывает baseline-артефакты и при ошибках откатывается к baseline

## Ограничения и troubleshooting

### Почему может сгенерироваться Dockerfile с `FROM ubuntu:22.04`

Это означает, что приложение классифицировано как `generic`.
Частые причины:

- команда запуска обёрнута в оболочку (`cmd.exe /c ...`) и тип приложения не извлечён из команды
- рабочая директория анализа (`--dir`) указывает не на корень проекта и ReHoster не видит `pom.xml`

Решение:

- явно передавать корень проекта через `-d/--dir` так, чтобы там лежали `pom.xml` и папка `src/`
- для Maven/Spring использовать запуск через Maven wrapper (`mvnw.cmd`) или установленный `mvn`

### Docker build падает на `cmd.exe`

Если в Dockerfile встречается `cmd.exe`, это почти наверняка означает, что Dockerfile был сгенерирован как для Windows-сценария запуска процесса.
Для Linux-контейнеров это неверно. Правильный путь:

- генерировать multi-stage Dockerfile (Maven build -> JRE runtime)
- запускать `java -jar ...`, а не `mvn spring-boot:run`

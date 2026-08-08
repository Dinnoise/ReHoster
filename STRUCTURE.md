src/main/java/com/rehoster
├── analysis
│ ├── DependencyAnalyzer.java - анализирует зависимости приложения
│ ├── MultiRoundAnalyzer.java - запускает многоэтапный анализ
│ ├── ServiceDependencyDetector.java - определяет внешние сервисы
│
├── ai
│ ├── client
│ │ ├── AiClient.java - контракт AI-клиента
│ │ ├── AiClientResponse.java - результат вызова AI
│ │ ├── LmStudioAiClient.java - клиент LM Studio API
│ │ └── OpenRouterAiClient.java - клиент OpenRouter API
│ ├── config
│ │ ├── AiConfig.java - конфигурация AI-этапа
│ │ ├── AiMode.java - режим применения AI
│ │ └── AiProvider.java - тип AI-провайдера
│ ├── interactive
│ │ └── UserRequirementCollector.java - собирает требования пользователя
│ ├── model
│ │ ├── AiChange.java - описание одного AI-изменения
│ │ ├── AiExecutionReport.java - отчёт выполнения AI
│ │ ├── AiRefinementOutcome.java - итог AI-рефайнинга
│ │ ├── AiRefinementRequest.java - входной AI-контекст
│ │ └── AiRefinementResult.java - ответ AI после нормализации
│ ├── prompt
│ │ └── AiPromptBuilder.java - собирает system/user prompt
│ └── service
│   ├── AiArtifactRefiner.java - координирует AI-рефайнинг
│   ├── AiContextCollector.java - собирает контекст для AI
│   ├── AiResponseParser.java - парсит и нормализует JSON AI
│   ├── AiResponseValidator.java - валидирует AI-артефакты
│   ├── AiSecretSanitizer.java - маскирует секреты перед AI
│   └── ArtifactMergePolicy.java - решает, применять ли AI
│
├── cli
│ └── CliParser.java - парсит CLI-аргументы
│
├── collector
│ ├── args
│ │ └── ArgsCollector.java - собирает аргументы запуска
│ ├── env
│ │ ├── EnvFileCollector.java - читает `.env` и похожие файлы
│ │ └── EnvironmentCollector.java - собирает env из процесса
│ ├── process
│ │ └── ProcessCollector.java - снимает данные о процессе
│ └── Collector.java - базовый контракт коллектора
│
├── generation
│ ├── ComposeGenerator.java - генерирует `docker-compose.yml`
│ ├── DockerfileGenerator.java - генерирует `Dockerfile`
│ ├── DockerignoreGenerator.java - генерирует `.dockerignore`
│ └── RecommendationGenerator.java - формирует рекомендации
│
├── launcher
│ └── ProcessLauncher.java - запускает целевое приложение
│
├── model
│ ├── analysis
│ │ ├── AppDependencyModel.java - модель зависимостей приложения
│ │ ├── EnvVarSpec.java - спецификация env-переменной
│ │ └── VolumeSpec.java - спецификация volume
│ ├── generation
│ │ ├── ComposeService.java - модель сервиса compose
│ │ ├── ComposeSpec.java - модель compose-спецификации
│ │ ├── DockerfileSpec.java - модель Dockerfile-спецификации
│ │ └── RunReport.java - итоговый отчёт запуска
│ ├── run
│ │ ├── LaunchResult.java - результат запуска процесса
│ │ ├── RunConfig.java - конфигурация запуска
│ │ └── RunContext.java - контекст выполнения
│ ├── snapshot
│ │ ├── EnvVar.java - снимок env-переменной
│ │ ├── Observation.java - единичное наблюдение анализа
│ │ ├── ProcessInfo.java - информация о процессе
│ │ └── RuntimeSnapshot.java - снапшот runtime-состояния
│
├── orchestrator
│ └── Orchestrator.java - координирует весь pipeline
│
├── storage
│ └── JsonStorage.java - сохраняет JSON и артефакты
│
├── util
│ ├── InstantTypeAdapter.java - адаптер времени для Gson
│ └── PathTypeAdapter.java - адаптер Path для Gson
│
├── Main.java - точка входа приложения
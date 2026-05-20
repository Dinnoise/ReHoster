package com.rehoster.generation;

import java.time.Instant;

import com.rehoster.model.analysis.AppDependencyModel;
import com.rehoster.model.analysis.EnvVarSpec;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class RecommendationGenerator {

    public String generate(RuntimeSnapshot snapshot, AppDependencyModel model) {
        StringBuilder md = new StringBuilder();
        
        md.append("# Отчёт анализа ReHoster\n\n");
        md.append("Сгенерировано: ").append(Instant.now()).append("\n\n");
        
        md.append("## Обзор приложения\n\n");
        md.append("- **Обнаруженный тип:** ").append(model.getDetectedType()).append("\n");
        md.append("- **Точка входа:** `").append(String.join(" ", model.getEntrypoint())).append("`\n");
        if (snapshot.getLaunchResult() != null && snapshot.getLaunchResult().getExitCode() != null) {
            md.append("- **Код завершения:** ").append(snapshot.getLaunchResult().getExitCode()).append("\n");
        }
        md.append("\n");
        
        md.append("## Переменные окружения\n\n");
        if (model.getRequiredEnv().isEmpty()) {
            md.append("Специальные переменные окружения не обнаружены.\n\n");
        } else {
            md.append("| Переменная | Обязательная | Значение по умолчанию |\n");
            md.append("|----------|----------|---------------|\n");
            for (EnvVarSpec env : model.getRequiredEnv()) {
                md.append("| `").append(env.getKey()).append("` | ");
                md.append(env.isRequired() ? "Да" : "Нет").append(" | ");
                md.append(env.getDefaultValue() != null ? "`" + truncate(env.getDefaultValue(), 30) + "`" : "-");
                md.append(" |\n");
            }
            md.append("\n");
        }
        
        md.append("## Открытые порты\n\n");
        if (model.getExposedPorts().isEmpty()) {
            md.append("Порты не обнаружены. Будет использован порт 8080 по умолчанию.\n\n");
        } else {
            for (Integer port : model.getExposedPorts()) {
                md.append("- **Порт ").append(port).append("**\n");
            }
            md.append("\n");
        }
        
        md.append("## Рекомендации\n\n");
        generateRecommendations(md, model, snapshot);
        
        md.append("## Наблюдения\n\n");
        if (snapshot.getObservations().isEmpty()) {
            md.append("Дополнительных наблюдений нет.\n\n");
        } else {
            for (Observation obs : snapshot.getObservations()) {
                String icon = getObservationIcon(obs.getSeverity());
                md.append("- ").append(icon).append(" **").append(obs.getType()).append(":** ");
                md.append(obs.getMessage()).append("\n");
            }
            md.append("\n");
        }
        
        md.append("## Сгенерированные файлы\n\n");
        md.append("- `Dockerfile` — описание контейнерного образа\n");
        md.append("- `docker-compose.yml` — конфигурация Docker Compose\n");
        md.append("- `runtime-snapshot.json` — сырые данные выполнения\n");
        md.append("- `analysis-result.json` — результаты анализа\n");
        md.append("- `report.json` — отчёт о запуске\n\n");
        
        md.append("## Следующие шаги\n\n");
        md.append("1. Проверьте сгенерированный `Dockerfile` и при необходимости измените базовый образ\n");
        md.append("2. Обновите переменные окружения в `docker-compose.yml`\n");
        md.append("3. Скопируйте файлы приложения в контекст сборки\n");
        md.append("4. Соберите образ: `docker build -t myapp .`\n");
        md.append("5. Запустите через Compose: `docker-compose up`\n");
        
        return md.toString();
    }

    private void generateRecommendations(StringBuilder md, AppDependencyModel model, RuntimeSnapshot snapshot) {
        String appType = model.getDetectedType();
        
        switch (appType) {
            case "java":
                md.append("### Java-приложение\n\n");
                md.append("- Рекомендуется использовать многоэтапную сборку для уменьшения размера образа\n");
                md.append("- Настройте ограничения памяти JVM с помощью параметров `-Xmx` и `-Xms`\n");
                md.append("- Используйте `eclipse-temurin` или `amazoncorretto` в качестве базового образа\n");
                md.append("- Добавьте endpoint проверки здоровья для оркестраторов контейнеров\n\n");
                
                break;

            case "php":
                md.append("### PHP / Laravel\n\n");
                md.append("- Убедитесь, что в Dockerfile установлены нужные расширения PHP (например `pdo_mysql`)\n");
                md.append("- Для `php artisan serve` используйте привязку к `0.0.0.0`, иначе приложение будет недоступно снаружи контейнера\n");
                md.append("- Рассмотрите переход на `php-fpm` + nginx для production-сценариев\n");
                md.append("- HEALTHCHECK лучше добавлять вручную под конкретный проект (endpoints и зависимости отличаются)\n\n");
                break;
                
            case "python":
                md.append("### Python-приложение\n\n");
                md.append("- Подготовьте `requirements.txt` со всеми зависимостями\n");
                md.append("- Используйте slim- или alpine-образ для уменьшения размера\n");
                md.append("- Рассмотрите использование виртуального окружения внутри контейнера\n");
                md.append("- Установите `PYTHONUNBUFFERED=1` для корректного логирования\n\n");
                
                break;
                
            case "nodejs":
                md.append("### Node.js-приложение\n\n");
                md.append("- Убедитесь, что `package.json` и `package-lock.json` присутствуют в проекте\n");
                md.append("- Используйте `npm ci` вместо `npm install` для воспроизводимых сборок\n");
                md.append("- Рассмотрите использование alpine-образа Node.js\n");
                md.append("- Установите `NODE_ENV=production` для оптимизированного выполнения\n\n");
                
                break;
                
            default:
                md.append("### Общие рекомендации\n\n");
                md.append("- Убедитесь, что все зависимости доступны внутри контейнера\n");
                md.append("- Протестируйте приложение внутри контейнера перед развёртыванием\n");
                md.append("- Добавьте проверки работоспособности (HEALTHCHECK/ready endpoints) под конкретный проект\n");
                md.append("- Проверьте соблюдение лучших практик безопасности контейнеризации\n\n");
        }
        
        if (!model.getRequiredEnv().isEmpty()) {
            md.append("### Настройка окружения\n\n");
            md.append("- Используйте Docker secrets или внешние хранилища для чувствительных значений\n");
            md.append("- Рассмотрите применение файла `.env` для локальной разработки\n");
            md.append("- Задокументируйте все обязательные переменные окружения\n\n");
        }
    }

    private String getObservationIcon(Observation.Severity severity) {
        switch (severity) {
            case ERROR: return "[ERROR]";
            case WARNING: return "[WARN]";
            case INFO: 
            default: return "[INFO]";
        }
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength - 3) + "...";
    }
}

package com.rehoster.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rehoster.model.snapshot.EnvVar;
import com.rehoster.model.snapshot.Observation;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public class ServiceDependencyDetector {

    public static class ServiceDependency {
        private String serviceName;
        private String image;
        private int defaultPort;
        private Map<String, String> environment;
        private List<String> volumes;
        private String healthCheck;

        public ServiceDependency(String serviceName, String image, int defaultPort) {
            this.serviceName = serviceName;
            this.image = image;
            this.defaultPort = defaultPort;
            this.environment = new HashMap<>();
            this.volumes = new ArrayList<>();
        }

        public String getServiceName() { return serviceName; }
        public String getImage() { return image; }
        public int getDefaultPort() { return defaultPort; }
        public Map<String, String> getEnvironment() { return environment; }
        public List<String> getVolumes() { return volumes; }
        public String getHealthCheck() { return healthCheck; }
        public void setHealthCheck(String healthCheck) { this.healthCheck = healthCheck; }
    }

    private static final Map<String, String[]> DB_PATTERNS = new HashMap<>();
    
    static {
        DB_PATTERNS.put("mysql", new String[]{"mysql", "mariadb", "DB_CONNECTION=mysql", "MYSQL_", "DATABASE_URL=mysql"});
        DB_PATTERNS.put("postgres", new String[]{"postgres", "pgsql", "postgresql", "DB_CONNECTION=pgsql", "POSTGRES_", "DATABASE_URL=postgres"});
        DB_PATTERNS.put("mongodb", new String[]{"mongodb", "mongo", "MONGO_", "MONGODB_"});
        DB_PATTERNS.put("redis", new String[]{"redis", "REDIS_", "CACHE_DRIVER=redis", "SESSION_DRIVER=redis"});
        DB_PATTERNS.put("memcached", new String[]{"memcached", "CACHE_DRIVER=memcached", "MEMCACHED_"});
        DB_PATTERNS.put("elasticsearch", new String[]{"elasticsearch", "elastic", "ELASTICSEARCH_"});
        DB_PATTERNS.put("rabbitmq", new String[]{"rabbitmq", "amqp", "RABBITMQ_", "QUEUE_CONNECTION=rabbitmq"});
        DB_PATTERNS.put("kafka", new String[]{"kafka", "KAFKA_"});
    }

    public List<ServiceDependency> detectDependencies(RuntimeSnapshot snapshot, Path workDir) {
        List<ServiceDependency> dependencies = new ArrayList<>();
        
        detectFromEnvVars(snapshot.getEnvironment(), dependencies, snapshot);
        
        if (workDir != null) {
            detectFromProjectFiles(workDir, dependencies, snapshot);
        }

        detectKafkaIfPresent(workDir, snapshot, dependencies);
        
        return dependencies;
    }

    private void detectFromEnvVars(List<EnvVar> envVars, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        boolean mysqlDetected = false;
        boolean postgresDetected = false;
        boolean redisDetected = false;
        boolean mongoDetected = false;
        boolean kafkaDetected = false;

        Map<String, String> dbConfig = new HashMap<>();

        for (EnvVar env : envVars) {
            String key = env.getKey().toUpperCase();
            String value = env.getValue() != null ? env.getValue().toLowerCase() : "";

            if (key.contains("MYSQL") || value.contains("mysql") || 
                (key.equals("DB_CONNECTION") && value.equals("mysql"))) {
                mysqlDetected = true;
            }
            if (key.contains("POSTGRES") || key.contains("PGSQL") || value.contains("postgres") ||
                (key.equals("DB_CONNECTION") && (value.equals("pgsql") || value.equals("postgres")))) {
                postgresDetected = true;
            }
            if (key.contains("REDIS") || value.contains("redis") ||
                (key.equals("CACHE_DRIVER") && value.equals("redis")) ||
                (key.equals("SESSION_DRIVER") && value.equals("redis"))) {
                redisDetected = true;
            }
            if (key.contains("MONGO") || value.contains("mongodb")) {
                mongoDetected = true;
            }

            if (key.startsWith("KAFKA_") || key.contains("KAFKA") || value.contains("kafka")) {
                kafkaDetected = true;
            }

            if (key.equals("DB_HOST") || key.equals("DATABASE_HOST")) {
                dbConfig.put("host", env.getValue());
            }
            if (key.equals("DB_PORT") || key.equals("DATABASE_PORT")) {
                dbConfig.put("port", env.getValue());
            }
            if (key.equals("DB_DATABASE") || key.equals("DB_NAME") || key.equals("DATABASE_NAME")) {
                dbConfig.put("database", env.getValue());
            }
            if (key.equals("DB_USERNAME") || key.equals("DATABASE_USER")) {
                dbConfig.put("username", env.getValue());
            }
            if (key.equals("DB_PASSWORD") || key.equals("DATABASE_PASSWORD")) {
                dbConfig.put("password", env.getValue());
            }
        }

        if (mysqlDetected && !hasDependency(dependencies, "mysql")) {
            ServiceDependency mysql = createMySQLDependency(dbConfig);
            dependencies.add(mysql);
            snapshot.addObservation(new Observation(
                "dependency_detection",
                Observation.Severity.INFO,
                "Detected MySQL/MariaDB database dependency"
            ));
        }

        if (postgresDetected && !hasDependency(dependencies, "postgres")) {
            ServiceDependency postgres = createPostgresDependency(dbConfig);
            dependencies.add(postgres);
            snapshot.addObservation(new Observation(
                "dependency_detection",
                Observation.Severity.INFO,
                "Detected PostgreSQL database dependency"
            ));
        }

        if (redisDetected && !hasDependency(dependencies, "redis")) {
            ServiceDependency redis = createRedisDependency();
            dependencies.add(redis);
            snapshot.addObservation(new Observation(
                "dependency_detection",
                Observation.Severity.INFO,
                "Detected Redis cache/session dependency"
            ));
        }

        if (mongoDetected && !hasDependency(dependencies, "mongo")) {
            ServiceDependency mongo = createMongoDependency(dbConfig);
            dependencies.add(mongo);
            snapshot.addObservation(new Observation(
                "dependency_detection",
                Observation.Severity.INFO,
                "Detected MongoDB database dependency"
            ));
        }

        if (kafkaDetected) {
            addKafkaAndZookeeper(dependencies, snapshot, "Detected Kafka dependency from environment variables");
        }
    }

    private void detectFromProjectFiles(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        detectFromComposerJson(workDir, dependencies, snapshot);
        detectFromPackageJson(workDir, dependencies, snapshot);
        detectFromRequirementsTxt(workDir, dependencies, snapshot);
        detectFromPomXml(workDir, dependencies, snapshot);
        detectFromDockerCompose(workDir, dependencies, snapshot);
    }

    private void detectFromComposerJson(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        File composerFile = workDir.resolve("composer.json").toFile();
        if (!composerFile.exists()) return;

        try {
            String content = readFile(composerFile);
            
            if (content.contains("\"illuminate/database\"") || content.contains("\"doctrine/dbal\"") ||
                content.contains("\"laravel/framework\"")) {
                
                if (content.contains("mysql") || content.contains("pdo_mysql")) {
                    if (!hasDependency(dependencies, "mysql")) {
                        dependencies.add(createMySQLDependency(new HashMap<>()));
                        snapshot.addObservation(new Observation("composer", Observation.Severity.INFO, 
                            "Detected MySQL from composer.json"));
                    }
                }
                if (content.contains("pgsql") || content.contains("pdo_pgsql")) {
                    if (!hasDependency(dependencies, "postgres")) {
                        dependencies.add(createPostgresDependency(new HashMap<>()));
                        snapshot.addObservation(new Observation("composer", Observation.Severity.INFO, 
                            "Detected PostgreSQL from composer.json"));
                    }
                }
            }
            if (content.contains("predis") || content.contains("phpredis")) {
                if (!hasDependency(dependencies, "redis")) {
                    dependencies.add(createRedisDependency());
                    snapshot.addObservation(new Observation("composer", Observation.Severity.INFO, 
                        "Detected Redis from composer.json"));
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void detectFromPackageJson(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        File packageFile = workDir.resolve("package.json").toFile();
        if (!packageFile.exists()) return;

        try {
            String content = readFile(packageFile);
            
            if (content.contains("\"mysql\"") || content.contains("\"mysql2\"")) {
                if (!hasDependency(dependencies, "mysql")) {
                    dependencies.add(createMySQLDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("npm", Observation.Severity.INFO, 
                        "Detected MySQL from package.json"));
                }
            }
            if (content.contains("\"pg\"") || content.contains("\"postgres\"")) {
                if (!hasDependency(dependencies, "postgres")) {
                    dependencies.add(createPostgresDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("npm", Observation.Severity.INFO, 
                        "Detected PostgreSQL from package.json"));
                }
            }
            if (content.contains("\"redis\"") || content.contains("\"ioredis\"")) {
                if (!hasDependency(dependencies, "redis")) {
                    dependencies.add(createRedisDependency());
                    snapshot.addObservation(new Observation("npm", Observation.Severity.INFO, 
                        "Detected Redis from package.json"));
                }
            }
            if (content.contains("\"mongodb\"") || content.contains("\"mongoose\"")) {
                if (!hasDependency(dependencies, "mongo")) {
                    dependencies.add(createMongoDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("npm", Observation.Severity.INFO, 
                        "Detected MongoDB from package.json"));
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void detectFromRequirementsTxt(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        File reqFile = workDir.resolve("requirements.txt").toFile();
        if (!reqFile.exists()) return;

        try {
            String content = readFile(reqFile).toLowerCase();
            
            if (content.contains("mysqlclient") || content.contains("pymysql") || content.contains("mysql-connector")) {
                if (!hasDependency(dependencies, "mysql")) {
                    dependencies.add(createMySQLDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("pip", Observation.Severity.INFO, 
                        "Detected MySQL from requirements.txt"));
                }
            }
            if (content.contains("psycopg2") || content.contains("asyncpg")) {
                if (!hasDependency(dependencies, "postgres")) {
                    dependencies.add(createPostgresDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("pip", Observation.Severity.INFO, 
                        "Detected PostgreSQL from requirements.txt"));
                }
            }
            if (content.contains("redis")) {
                if (!hasDependency(dependencies, "redis")) {
                    dependencies.add(createRedisDependency());
                    snapshot.addObservation(new Observation("pip", Observation.Severity.INFO, 
                        "Detected Redis from requirements.txt"));
                }
            }
            if (content.contains("pymongo") || content.contains("motor")) {
                if (!hasDependency(dependencies, "mongo")) {
                    dependencies.add(createMongoDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("pip", Observation.Severity.INFO, 
                        "Detected MongoDB from requirements.txt"));
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void detectFromPomXml(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        File pomFile = workDir.resolve("pom.xml").toFile();
        if (!pomFile.exists()) return;

        try {
            String content = readFile(pomFile);
            
            if (content.contains("mysql-connector") || content.contains("<artifactId>mysql</artifactId>")) {
                if (!hasDependency(dependencies, "mysql")) {
                    dependencies.add(createMySQLDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("maven", Observation.Severity.INFO, 
                        "Detected MySQL from pom.xml"));
                }
            }
            if (content.contains("postgresql") || content.contains("<artifactId>postgres</artifactId>")) {
                if (!hasDependency(dependencies, "postgres")) {
                    dependencies.add(createPostgresDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("maven", Observation.Severity.INFO, 
                        "Detected PostgreSQL from pom.xml"));
                }
            }
            if (content.contains("jedis") || content.contains("lettuce")) {
                if (!hasDependency(dependencies, "redis")) {
                    dependencies.add(createRedisDependency());
                    snapshot.addObservation(new Observation("maven", Observation.Severity.INFO, 
                        "Detected Redis from pom.xml"));
                }
            }

            if (content.contains("spring-kafka") || content.contains("kafka-clients") || content.contains("<artifactId>kafka</artifactId>")) {
                addKafkaAndZookeeper(dependencies, snapshot, "Detected Kafka from pom.xml");
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void detectKafkaIfPresent(Path workDir, RuntimeSnapshot snapshot, List<ServiceDependency> dependencies) {
        if (workDir == null) {
            return;
        }

        if (hasDependency(dependencies, "kafka")) {
            if (!hasDependency(dependencies, "zookeeper")) {
                dependencies.add(createZookeeperDependency());
            }
            return;
        }

        boolean kafkaFound = false;

        if (detectKafkaFromSpringConfigs(workDir)) {
            kafkaFound = true;
        }
        if (!kafkaFound && detectKafkaFromSource(workDir)) {
            kafkaFound = true;
        }

        if (kafkaFound) {
            addKafkaAndZookeeper(dependencies, snapshot, "Detected Kafka usage from project configuration/source");
        }
    }

    private boolean detectKafkaFromSpringConfigs(Path workDir) {
        Path resources = workDir.resolve("src").resolve("main").resolve("resources");
        if (!resources.toFile().exists()) {
            return false;
        }

        try {
            return Files.walk(resources)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.equals("application.yml") || n.equals("application.yaml") || n.equals("application.properties") ||
                           n.endsWith(".yml") || n.endsWith(".yaml") || n.endsWith(".properties");
                })
                .anyMatch(p -> fileContainsKafkaHints(p));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean fileContainsKafkaHints(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            String c = content.toLowerCase();

            if (c.contains("spring.kafka")) {
                return true;
            }
            if (c.contains("bootstrap-servers") && c.contains("kafka")) {
                return true;
            }

            if (c.contains("app.kafka")) {
                if (c.contains("app.kafka.enabled") && c.contains("true")) {
                    return true;
                }
                if (c.contains("bootstrap") || c.contains("bootstrap-servers")) {
                    return true;
                }
            }

        } catch (IOException ignored) {
        }
        return false;
    }

    private boolean detectKafkaFromSource(Path workDir) {
        Path srcMain = workDir.resolve("src").resolve("main");
        if (!srcMain.toFile().exists()) {
            return false;
        }

        try {
            return Files.walk(srcMain)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".java") || n.endsWith(".kt");
                })
                .anyMatch(p -> fileContainsKafkaUsage(p));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean fileContainsKafkaUsage(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return content.contains("@KafkaListener") ||
                   content.contains("KafkaTemplate") ||
                   content.contains("KafkaConsumer") ||
                   content.contains("org.apache.kafka") ||
                   content.contains("springframework.kafka");
        } catch (IOException ignored) {
            return false;
        }
    }

    private void detectFromDockerCompose(Path workDir, List<ServiceDependency> dependencies, RuntimeSnapshot snapshot) {
        File composeFile = workDir.resolve("docker-compose.yml").toFile();
        if (!composeFile.exists()) {
            composeFile = workDir.resolve("docker-compose.yaml").toFile();
        }
        if (!composeFile.exists()) return;

        try {
            String content = readFile(composeFile).toLowerCase();
            
            if (content.contains("mysql:") || content.contains("mariadb:")) {
                if (!hasDependency(dependencies, "mysql")) {
                    dependencies.add(createMySQLDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("docker-compose", Observation.Severity.INFO, 
                        "Detected MySQL from existing docker-compose.yml"));
                }
            }
            if (content.contains("postgres:")) {
                if (!hasDependency(dependencies, "postgres")) {
                    dependencies.add(createPostgresDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("docker-compose", Observation.Severity.INFO, 
                        "Detected PostgreSQL from existing docker-compose.yml"));
                }
            }
            if (content.contains("redis:")) {
                if (!hasDependency(dependencies, "redis")) {
                    dependencies.add(createRedisDependency());
                    snapshot.addObservation(new Observation("docker-compose", Observation.Severity.INFO, 
                        "Detected Redis from existing docker-compose.yml"));
                }
            }
            if (content.contains("mongo:")) {
                if (!hasDependency(dependencies, "mongo")) {
                    dependencies.add(createMongoDependency(new HashMap<>()));
                    snapshot.addObservation(new Observation("docker-compose", Observation.Severity.INFO, 
                        "Detected MongoDB from existing docker-compose.yml"));
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private ServiceDependency createMySQLDependency(Map<String, String> config) {
        ServiceDependency mysql = new ServiceDependency("mysql", "mysql:8.0", 3306);
        mysql.getEnvironment().put("MYSQL_DATABASE", config.getOrDefault("database", "app"));
        mysql.getEnvironment().put("MYSQL_ALLOW_EMPTY_PASSWORD", "yes");
        mysql.getVolumes().add("mysql_data:/var/lib/mysql");
        mysql.setHealthCheck("mysqladmin ping -h localhost");
        return mysql;
    }

    private ServiceDependency createPostgresDependency(Map<String, String> config) {
        ServiceDependency postgres = new ServiceDependency("postgres", "postgres:15", 5432);
        postgres.getEnvironment().put("POSTGRES_DB", config.getOrDefault("database", "app"));
        postgres.getEnvironment().put("POSTGRES_USER", config.getOrDefault("username", "app"));
        postgres.getEnvironment().put("POSTGRES_PASSWORD", config.getOrDefault("password", "secret"));
        postgres.getVolumes().add("postgres_data:/var/lib/postgresql/data");
        postgres.setHealthCheck("pg_isready -U app");
        return postgres;
    }

    private ServiceDependency createRedisDependency() {
        ServiceDependency redis = new ServiceDependency("redis", "redis:7-alpine", 6379);
        redis.getVolumes().add("redis_data:/data");
        redis.setHealthCheck("redis-cli ping");
        return redis;
    }

    private ServiceDependency createMongoDependency(Map<String, String> config) {
        ServiceDependency mongo = new ServiceDependency("mongo", "mongo:6", 27017);
        mongo.getEnvironment().put("MONGO_INITDB_ROOT_USERNAME", config.getOrDefault("username", "root"));
        mongo.getEnvironment().put("MONGO_INITDB_ROOT_PASSWORD", config.getOrDefault("password", "secret"));
        mongo.getVolumes().add("mongo_data:/data/db");
        return mongo;
    }

    private ServiceDependency createZookeeperDependency() {
        ServiceDependency zk = new ServiceDependency("zookeeper", "confluentinc/cp-zookeeper:7.5.0", 2181);
        zk.getEnvironment().put("ZOOKEEPER_CLIENT_PORT", "2181");
        zk.getEnvironment().put("ZOOKEEPER_TICK_TIME", "2000");
        zk.getVolumes().add("zookeeper_data:/var/lib/zookeeper/data");
        return zk;
    }

    private ServiceDependency createKafkaDependency() {
        ServiceDependency kafka = new ServiceDependency("kafka", "confluentinc/cp-kafka:7.5.0", 9092);
        kafka.getEnvironment().put("KAFKA_BROKER_ID", "1");
        kafka.getEnvironment().put("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181");
        kafka.getEnvironment().put("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092");
        kafka.getEnvironment().put("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        kafka.getEnvironment().put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        kafka.getEnvironment().put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        kafka.getVolumes().add("kafka_data:/var/lib/kafka/data");
        return kafka;
    }

    private void addKafkaAndZookeeper(List<ServiceDependency> dependencies, RuntimeSnapshot snapshot, String message) {
        if (!hasDependency(dependencies, "zookeeper")) {
            dependencies.add(createZookeeperDependency());
        }
        if (!hasDependency(dependencies, "kafka")) {
            dependencies.add(createKafkaDependency());
        }

        if (snapshot != null && message != null) {
            snapshot.addObservation(new Observation(
                "dependency_detection",
                Observation.Severity.INFO,
                message
            ));
        }
    }

    private boolean hasDependency(List<ServiceDependency> dependencies, String name) {
        for (ServiceDependency dep : dependencies) {
            if (dep.getServiceName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}

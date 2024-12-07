# Используем базовый образ OpenJDK
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файл сборки
COPY target/aeris-dvoretsky.jar app.jar

# Устанавливаем точку входа
ENTRYPOINT ["java", "-jar", "app.jar"]
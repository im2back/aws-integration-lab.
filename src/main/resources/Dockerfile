
# STAGE 1 - BUILD
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copia o pom.xml para aproveitar o cache
COPY pom.xml .

# Baixa as dependencias do Maven
RUN mvn dependency:go-offline -B

# Copia o codigo-fonte
COPY src ./src

# Compila e empacota a aplicacao
RUN mvn clean package -DskipTests

# STAGE 2 - RUNTIME
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia o JAR produzido pelo primeiro stage
COPY --from=build /app/target/*.jar app.jar

# Porta utilizada pelo Spring Boot
EXPOSE 8080

# Inicializa a aplicacao
ENTRYPOINT ["java", "-jar", "app.jar"]

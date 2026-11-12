# Etapa 1: build com JDK + Maven Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# Etapa 2: imagem final só com o JRE e o .jar (menor e sem ferramentas de build)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home creatorpay
COPY --from=build /app/target/creatorpay-mcp-*.jar app.jar
USER creatorpay
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

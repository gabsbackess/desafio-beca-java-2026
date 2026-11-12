# Etapa 1: compilação da aplicação
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw clean package -DskipTests
# Etapa 2: execução da aplicação
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Certificados extras (opcional): em redes com proxy HTTPS corporativo, coloque o certificado raiz
# (.crt, .cer ou .pem) em transactions/certs/ antes do build. Sem arquivos nessa pasta, nada muda.
COPY certs/ /tmp/certs/
RUN for f in /tmp/certs/*.crt /tmp/certs/*.cer /tmp/certs/*.pem; do \
      [ -f "$f" ] || continue; \
      keytool -importcert -noprompt -trustcacerts -cacerts -storepass changeit \
        -alias "extra-$(basename "$f")" -file "$f"; \
    done; \
    rm -rf /tmp/certs
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

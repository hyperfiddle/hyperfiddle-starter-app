FROM clojure:temurin-17-tools-deps-1.12.0.1501 AS build
WORKDIR /app
COPY deps.edn deps.edn
RUN clojure -M -e ::ok              # preload deps
RUN clojure -A:build -M -e ::ok     # preload build deps
COPY src src
COPY src-prod src-prod
COPY src-build src-build
COPY resources resources
RUN clojure -X:build uberjar :build/jar-name '"app.jar"'

FROM amazoncorretto:17 AS app
# FROM clojure:temurin-17-tools-deps-1.12.0.1501 AS app
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
CMD java -cp app.jar clojure.main -m dustingetz.main

# How to build for prod

## Uberjar (Jetty 10+)

clojure -X:prod:build uberjar :version '"'$(git rev-parse HEAD)'"' :build/jar-name '"hyperfiddle-starter-app.jar"'
java -cp target/hyperfiddle-starter-app.jar clojure.main -m prod

## Uberjar (Jetty 9)

clojure -X:jetty9:prod:build uberjar :version '"'$(git rev-parse HEAD)'"' :shadow-build :prod-jetty9 :aliases '[:jetty9 :prod]' :build/jar-name '"hyperfiddle-starter-app.jar"'
java -cp target/hyperfiddle-starter-app.jar clojure.main -m prod-jetty9

## Docker

docker build --build-arg VERSION=$(git rev-parse HEAD) -t hyperfiddle-starter-app:latest .
docker run --rm -it -p 8080:8080 hyperfiddle-starter-app:latest

## Fly

fly deploy --remote-only --build-arg VERSION=$(git rev-parse HEAD)

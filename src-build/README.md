# How to build for prod

## Uberjar

```shell
clojure -X:prod:build uberjar :build/jar-name '"hyperfiddle-starter-app.jar"'
java -cp target/hyperfiddle-starter-app.jar clojure.main -m dustingetz.main
```

## Docker

```shell
docker build -t hyperfiddle-starter-app:latest .
docker run --rm -it -p 8080:8080 hyperfiddle-starter-app:latest
```

## Fly

```shell
fly deploy --remote-only
```

# Hyperfiddle starter app

## Links

* Hyperfiddle github: https://github.com/hyperfiddle/hyperfiddle

## Getting started - dev setup

Prerequisites
* `java -version` modern version, we use `openjdk version "23.0.2"`
* Clojure CLI https://clojure.org/guides/install_clojure

```shell
git clone git@gitlab.com:hyperfiddle/hyperfiddle-starter-app.git
cd hyperfiddle-starter-app
```

* Shell: `clj -X:dev dev/-main`.
* Login instructions will be printed
* REPL: `:dev` deps alias, `(dev/-main)` at the REPL to start dev build
* App will start on http://localhost:8080

> [!WARNING]
> Electric dev environments must run **one single JVM** that is shared by both the Clojure REPL and shadow-cljs ClojureScript compilation process! Electric uses a custom hot code reloading strategy to ensure that the Electric frontend and backend processes (DAGs) stay in sync as you change one or the other. This starter repo achieves this by booting shadow from the dev entrypoint[src-dev/dev.cljc](src-dev/dev.cljc). I believe this is compatible with both Calva and Cursive's "happy path" shadow-cljs support. For other editors, watch out as your boot sequence may run shadow-cljs in a second process! You will experience undefined behavior.

## License
* free for individual use on local dev machines, mandatory runtime login (we are a business)
* using in prod requires a license, contact us.
* still working out the details
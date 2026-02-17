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
clj -A:dev -M -m dustingetz.main
# Explorer: http://explorer.localhost:8080
# Admin:    http://admin.localhost:8080
```

* REPL: jack-in with `:dev` alias, then eval `(dev/-main)`

## License
* free for individual use on local dev machines, mandatory runtime login (we are a business)
* using in prod requires a license, contact us.
* still working out the details
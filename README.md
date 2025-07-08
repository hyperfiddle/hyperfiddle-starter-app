# Hyperfiddle starter app

## Links

* Hyperfiddle github: https://github.com/hyperfiddle/hyperfiddle

## Getting started

```shell
git clone git@gitlab.com:hyperfiddle/hyperfiddle-starter-app.git
cd hyperfiddle-starter-app

# Run demo app. You’ll be asked to authenticate.
# First via Clojure CLI to see it working. https://clojure.org/guides/install_clojure
clj -X:dev dev/-main

# Now jack in to REPL, :dev alias:
user=> (dev/-main)
```

## License
* free for individual use on local dev machines, mandatory runtime login (we are a business)
* using in prod requires a license, contact us.
* still working out the details
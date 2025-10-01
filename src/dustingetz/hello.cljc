(ns dustingetz.hello
  (:require [hyperfiddle.hfql2 :as hfql]))

#?(:clj (def sitemap {'dustingetz.hello (hfql/hfql (find-ns 'dustingetz.hello))}))

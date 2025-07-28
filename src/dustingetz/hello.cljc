(ns dustingetz.hello
  (:require [hyperfiddle.hfql0 #?(:clj :as :cljs :as-alias) hfql]))

#?(:clj (def sitemap
          (hfql/sitemap
            {})))

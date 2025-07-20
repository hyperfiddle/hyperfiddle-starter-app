(ns dustingetz.hello
  (:require [hyperfiddle.electric3 :as e] ; presence enables hot code reloading
            #?(:clj [hyperfiddle.hfql0 :as hfql])))

#?(:clj (def sitemap
          (hfql/sitemap
            {all-ns []})))

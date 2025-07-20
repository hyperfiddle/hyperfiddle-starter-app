(ns dustingetz.hello
  (:require [hyperfiddle.electric3 :as e]
            #?(:clj [hyperfiddle.hfql0 :as hfql])))

(e/defn Unused []) ; presence of an electric def turns on server-side hot reloading on save for this file

#?(:clj (def sitemap
          (hfql/sitemap
            {all-ns []})))

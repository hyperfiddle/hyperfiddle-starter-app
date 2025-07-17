(ns dustingetz.hyperfiddle-demo
  (:require
    dustingetz.file-explorer
    dustingetz.namespace-explorer
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.electric-dom3 :as dom]
    [hyperfiddle.entrypoint :refer [Hyperfiddle]]
    [hyperfiddle.navigator4 :refer [HfqlRoot]]))

#?(:clj (def sitemap
          (merge
            dustingetz.file-explorer/site-map
            dustingetz.namespace-explorer/site-map)))

(e/defn Explorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (HfqlRoot sitemap
    `[(dustingetz.namespace-explorer/clojure-all-ns)
      (dustingetz.file-explorer/dir-list ".")
      (clojure.java.io/file ".")]))

(e/defn InjectAndRunHyperfiddle [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      (dom/div (dom/props {:style {:display "contents"}}) ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
        (Hyperfiddle
          {'explorer Explorer})))))

(defn hyperfiddle-demo-boot [ring-request]
  #?(:clj  (e/boot-server {} InjectAndRunHyperfiddle (e/server ring-request))
     :cljs (e/boot-client {} InjectAndRunHyperfiddle (e/server (e/amb)))))

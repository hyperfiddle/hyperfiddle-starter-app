(ns dustingetz.hyperfiddle-demo
  (:require
   [dustingetz.file-explorer]
   [dustingetz.namespace-explorer]

   [hyperfiddle.electric3 :as e]
   [hyperfiddle.electric-dom3 :as dom]
   [hyperfiddle.entrypoint :refer [Hyperfiddle]]
   [hyperfiddle.navigator4 :as navigator :refer [HfqlRoot]]
   ))

(e/defn Explorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (dom/p (dom/text " 👀👇 Nav links are broken ")) ; FIXME
  (HfqlRoot (e/server (merge dustingetz.file-explorer/site-map
                        dustingetz.namespace-explorer/site-map))
    ['(clojure.java.io/file ".")]))

(e/defn InjectAndRunHyperfiddle [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      (dom/div (dom/props {:style {:display "contents"}}) ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
               (Hyperfiddle
                 {`Explorer Explorer
                  `dustingetz.file-explorer/FileExplorer dustingetz.file-explorer/FileExplorer
                  `dustingetz.namespace-explorer/NamespaceExplorer dustingetz.namespace-explorer/NamespaceExplorer})))))

(defn hyperfiddle-demo-boot [ring-request]
  #?(:clj  (e/boot-server {} InjectAndRunHyperfiddle (e/server ring-request)) ; client/server entrypoints must be symmetric
     :cljs (e/boot-client {} InjectAndRunHyperfiddle (e/server (e/amb)))))    ; ring-request is server only, client sees nothing in place

(ns dustingetz.hyperfiddle-demo
  (:require
   [dustingetz.file-explorer]

   [hyperfiddle.electric3 :as e]
   [hyperfiddle.electric-dom3 :as dom]
   [hyperfiddle.entrypoint :refer [Hyperfiddle]]))

(e/defn InjectAndRunHyperfiddle [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      (dom/div (dom/props {:style {:display "contents"}}) ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
        (Hyperfiddle
          {`dustingetz.file-explorer/FileExplorer dustingetz.file-explorer/FileExplorer})))))

(defn hyperfiddle-demo-boot [ring-request]
  #?(:clj  (e/boot-server {} InjectAndRunHyperfiddle (e/server ring-request)) ; client/server entrypoints must be symmetric
     :cljs (e/boot-client {} InjectAndRunHyperfiddle (e/server (e/amb)))))    ; ring-request is server only, client sees nothing in place

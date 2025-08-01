(ns dustingetz.hyperfiddle-demo
  (:require
    dustingetz.file-explorer
    #_dustingetz.hello
    dustingetz.namespace-explorer
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.electric-dom3 :as dom]
    [hyperfiddle.entrypoint2 :refer [Hyperfiddle]]
    [hyperfiddle.navigator5 :refer [HfqlRoot]]))

#?(:clj (def index
          '[(all-ns)
            (dustingetz.fs2/dir-list ".")
            (clojure.java.io/file ".")]))

(e/defn Explorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (let [sitemap (e/server (merge ; don't externalize to a global clojure def, it will sever hot reload on sitemap change
                            #_dustingetz.file-explorer/sitemap
                            #_dustingetz.hello/sitemap
                            dustingetz.namespace-explorer/sitemap))]
    (HfqlRoot sitemap index)))

(e/defn InjectAndRunHyperfiddle [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)
              e/*exports* (e/server (e/exports))]
      (dom/div (dom/props {:style {:display "contents"}}) ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
        (Hyperfiddle
          {'explorer Explorer})))))

(defn hyperfiddle-demo-boot [ring-request]
  #?(:clj  (e/boot-server {} InjectAndRunHyperfiddle (e/server ring-request))
     :cljs (e/boot-client {} InjectAndRunHyperfiddle (e/server (e/amb)))))

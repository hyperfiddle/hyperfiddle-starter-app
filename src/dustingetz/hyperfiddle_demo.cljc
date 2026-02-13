(ns dustingetz.hyperfiddle-demo
  (:require
   #?(:clj dustingetz.nav-file)
   #?(:clj dustingetz.nav-clj-ns)
   [hyperfiddle.electric3 :as e]
   [hyperfiddle.electric-dom3 :as dom]
   [hyperfiddle.entrypoint2 :refer [Hyperfiddle]]
   [hyperfiddle.electric-forms5 :refer [Checkbox*]]
   [hyperfiddle.navigator6 :refer [HfqlRoot]]))

#?(:clj (def index
          ['clojure.core/all-ns
           '(file (clojure.java.io/file "."))]))

(e/defn Explorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser2.css"})) ; TODO remove
  (Checkbox* false {:class "data-loader__enabled" :style {:position :absolute, :inset-block-start "1dvw", :inset-inline-end "1dvw"}})
  (let [sitemap (e/server (merge ; don't externalize to a global clojure def, it will sever hot reload on sitemap change
                            dustingetz.nav-clj-ns/sitemap
                            dustingetz.nav-file/sitemap))]
    (HfqlRoot (e/server sitemap) index)))

(e/defn InjectAndRunHyperfiddle [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)
              e/*exports* (e/server (merge (e/exports)))]
      (dom/div (dom/props {:style {:display "contents"}}) ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
        (Hyperfiddle
          {'explorer Explorer})))))

(defn hyperfiddle-demo-boot [ring-request]
  #?(:clj  (e/boot-server {} InjectAndRunHyperfiddle (e/server ring-request))
     :cljs (e/boot-client {} InjectAndRunHyperfiddle (e/server (e/amb)))))

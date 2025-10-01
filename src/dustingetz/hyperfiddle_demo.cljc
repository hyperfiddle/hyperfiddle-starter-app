(ns dustingetz.hyperfiddle-demo
  (:require
    dustingetz.file-explorer
    dustingetz.hello
    dustingetz.namespace-explorer
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.electric-dom3 :as dom]
    [hyperfiddle.entrypoint2 :refer [Hyperfiddle]]
    [hyperfiddle.navigator6 :refer [HfqlRoot]]
    [hyperfiddle.hfql2 :as hfql :refer [hfql]])
  #?(:clj (:import [java.io File])))

#?(:clj (def index
          '[(hello)
            (all-ns)
            (dustingetz.fs2/dir-list ".")
            (clojure.java.io/file ".")]))

(def lorem (clojure.string/join \n 
             ["Aliquam erat volutpat.  Nunc eleifend leo vitae magna.  In id erat non orci commodo lobortis.  Proin neque massa, cursus ut, gravida ut, lobortis eget, lacus.  Sed diam.  Praesent fermentum tempor tellus.  Nullam tempus.  Mauris ac felis vel velit tristique imperdiet.  Donec at pede.  Etiam vel neque nec dui dignissim bibendum.  Vivamus id enim.  Phasellus neque orci, porta a, aliquet quis, semper a, massa.  Phasellus purus.  Pellentesque tristique imperdiet tortor.  Nam euismod tellus id erat."
              "Pellentesque dapibus suscipit ligula.  Donec posuere augue in quam.  Etiam vel tortor sodales tellus ultricies commodo.  Suspendisse potenti.  Aenean in sem ac leo mollis blandit.  Donec neque quam, dignissim in, mollis nec, sagittis eu, wisi.  Phasellus lacus.  Etiam laoreet quam sed arcu.  Phasellus at dui in ligula mollis ultricies.  Integer placerat tristique nisl.  Praesent augue.  Fusce commodo.  Vestibulum convallis, lorem a tempus semper, dui dui euismod elit, vitae placerat urna tortor vitae lacus.  Nullam libero mauris, consequat quis, varius et, dictum id, arcu.  Mauris mollis tincidunt felis.  Aliquam feugiat tellus ut neque.  Nulla facilisis, risus a rhoncus fermentum, tellus tellus lacinia purus, et dictum nunc justo sit amet elit."]))

(defn split-sentences [str]
  (clojure.string/split str #"\.\s+"))

(defn word-count [str] (count (re-seq #"\w+" str)))

(e/defn Explorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (let [sitemap (e/server (merge ; don't externalize to a global clojure def, it will sever hot reload on sitemap change
                            dustingetz.hello/sitemap
                            dustingetz.namespace-explorer/sitemap
                            dustingetz.file-explorer/sitemap
                            {'lorem (hfql {(split-sentences ^:symbolic lorem) {* [identity word-count]}})}))]
    (HfqlRoot (e/server sitemap)

      ['(dustingetz.file-explorer/file (clojure.java.io/file "."))
       '(dustingetz.file-explorer/dir-list (clojure.java.io/file "."))
       '(clojure.core/all-ns)
       'lorem
       'dustingetz.hello])))

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

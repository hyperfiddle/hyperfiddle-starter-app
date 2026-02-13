(ns dustingetz.nav-clj-ns
  (:require
   [hyperfiddle.hfql2 :as hfql :refer [hfql]]
   [hyperfiddle.hfql2.protocols :refer [Identifiable Suggestable -hfql-resolve]]))

(defn doc [!x] (-> !x meta :doc))
(defn author [!x] (-> !x meta :author))
(defn var-arglists [!var] (->> !var meta :arglists seq pr-str))

(def sitemap
  {`all-ns (hfql {(all-ns) {* ^{::hfql/select `ns-publics} [ns-name]}})
   `ns-publics (hfql {ns-publics {vals {* [symbol]}}})})

(extend-type clojure.lang.Namespace
  Identifiable (-identify [ns] `(find-ns ~(ns-name ns)))
  Suggestable (-suggest [_] (hfql [ns-name doc author
                                   ns-publics ; TODO leverage ::hf/select to `ns-publics
                                   meta])))

(extend-type clojure.lang.Var
  Identifiable (-identify [ns] `(find-var ~(symbol ns)))
  Suggestable (-suggest [_] (hfql [symbol var-arglists doc meta .isMacro .isDynamic .getTag])))

(defmethod -hfql-resolve `find-ns [[_ ns-sym]] (find-ns ns-sym))
(defmethod -hfql-resolve `find-var [[_ var-sym]] (find-var var-sym))

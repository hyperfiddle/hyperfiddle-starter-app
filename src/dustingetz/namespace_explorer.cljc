(ns dustingetz.namespace-explorer
  (:require [hyperfiddle.hfql2 :as hfql :refer [hfql]]))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
;; #?(:clj (defn ns-publics2 [!ns] (vals (ns-publics !ns)))) ; collection-record form
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists seq pr-str)))

#?(:clj (def sitemap
          {`all-ns (hfql {(all-ns) {* ^{::hfql/select `ns-publics} [ns-name]}})
           `ns-publics (hfql {ns-publics {vals {* [symbol]}}})}))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [ns] `(find-ns ~(ns-name ns)))
          hfql/Suggestable (-suggest [_] (hfql [ns-name doc author ns-publics meta]))))

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [ns] `(find-var ~(symbol ns)))
          hfql/Suggestable (-suggest [_] (hfql [symbol var-arglists doc {meta [:ns *]} .isMacro .isDynamic .getTag]))))


#?(:clj (defmethod hfql/resolve `find-ns [[_ ns-sym]] (find-ns ns-sym)))
#?(:clj (defmethod hfql/resolve `find-var [[_ var-sym]] (find-var var-sym)))


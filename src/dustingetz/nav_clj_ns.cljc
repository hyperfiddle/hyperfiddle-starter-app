(ns dustingetz.nav-clj-ns
  (:require
   [hyperfiddle.hfql2 :as hfql :refer [hfql]]
   [hyperfiddle.hfql2.protocols :refer [Identifiable Suggestable -hfql-resolve]]))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists seq pr-str)))

#?(:clj (def sitemap
          {`all-ns (hfql {(all-ns) {* ^{::hfql/select `ns-publics} [ns-name]}})
           `ns-publics (hfql {ns-publics {vals {* [symbol]}}})}))

#?(:clj (extend-type clojure.lang.Namespace
          Identifiable (-identify [ns] `(find-ns ~(ns-name ns)))
          Suggestable (-suggest [_] (hfql [ns-name doc author
                                           ns-publics ; TODO leverage ::hf/select to `ns-publics
                                           meta]))))

#?(:clj (extend-type clojure.lang.Var
          Identifiable (-identify [ns] `(find-var ~(symbol ns)))
          Suggestable (-suggest [_] (hfql [symbol var-arglists doc meta .isMacro .isDynamic .getTag]))))

#?(:clj (defmethod -hfql-resolve `find-ns [[_ ns-sym]] (find-ns ns-sym)))
#?(:clj (defmethod -hfql-resolve `find-var [[_ var-sym]] (find-var var-sym)))

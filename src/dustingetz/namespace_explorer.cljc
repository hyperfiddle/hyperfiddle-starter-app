(ns dustingetz.namespace-explorer
  (:require
    #?(:clj [hyperfiddle.hfql0 :as hfql :refer [Identifiable Suggestable]]
       :cljs [hyperfiddle.hfql0 :as-alias hfql])))

#?(:clj (defn clojure-all-ns "List all clojure (jvm) namespaces" [] (vec (sort-by ns-name (all-ns)))))
#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn ns-publics2 [ns-sym] (-> ns-sym ns-publics vals)))
#?(:clj (defn ns-publics-count [ns-sym] (count (ns-publics ns-sym))))
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists str)))
#?(:clj (defn var-name [!var] (-> !var symbol name symbol)))

#?(:clj (extend-type clojure.lang.Var
          Identifiable (-identify [x] (symbol x))
          Suggestable (-suggest [_] (hfql/pull-spec [var-name var-arglists doc meta
                                                     .getTag .isMacro]))))

#?(:clj (extend-type clojure.lang.Namespace
          Identifiable (-identify [^clojure.lang.Namespace ns] (ns-name ns))
          Suggestable (-suggest [_] (hfql/pull-spec [ns-name doc meta author ns-publics2 ns-interns ns-imports]))))

#?(:clj (def site-map
          (hfql/sitemap
            {clojure-all-ns (hfql/props [ns-name ns-publics-count doc] {::hfql/select (ns-publics2 %)})
             ns-publics2 [var-name var-arglists doc type]})))

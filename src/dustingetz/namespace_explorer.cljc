(ns dustingetz.namespace-explorer
  (:require [hyperfiddle.hfql0 #?(:clj :as :cljs :as-alias) hfql]))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn ns-publics2 [ns-sym] (-> ns-sym ns-publics vals)))
#?(:clj (defn ns-publics-count [ns-sym] (count (ns-publics ns-sym))))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [^clojure.lang.Namespace ns] (ns-name ns))
          hfql/Suggestable (-suggest [_] (hfql/pull-spec [ns-name doc meta author ns-publics2 ns-interns ns-imports]))))

#?(:clj (defn var-arglists [!var] (->> !var meta :arglists str)))
#?(:clj (defn var-name [!var] (-> !var symbol name symbol)))

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [x] (symbol x))
          hfql/Suggestable (-suggest [_] (hfql/pull-spec [var-name var-arglists doc meta
                                                     .getTag .isMacro]))))

#?(:clj (def sitemap
          (hfql/sitemap
            {all-ns (hfql/props [ns-name ns-publics-count doc] {::hfql/select (ns-publics2 %)})
             ns-publics2 [var-name var-arglists doc type]})))

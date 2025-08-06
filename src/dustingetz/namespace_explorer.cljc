(ns ^{:hyperfiddle.electric.impl.lang3/has-edef? true} ; enable server hot reloading
  dustingetz.namespace-explorer
  (:require [dustingetz.namespace-explorer :as-alias ns-explorer]
            [clojure.core :as cc]
            [hyperfiddle.hfql1 :as hfql :refer [hfql]])
  #?(:clj (:import [clojure.lang Var])))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn ns-publics2 [ns-sym] (vals (ns-publics ns-sym)))) ; collection-record form
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists seq pr-str)))

#?(:clj (def sitemap
          (hfql {(all-ns) (hfql [cc/ns-name] {::hfql/select '(ns-publics2 %)})
                 ns-publics2 [symbol]})))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [ns] (ns-name ns))
          hfql/Suggestable (-suggest [_] (hfql [ns-name ns-explorer/doc author ns-publics2 meta]))))

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [ns] (symbol ns))
          hfql/Suggestable (-suggest [_] (hfql [symbol var-arglists doc meta .isMacro  Var/.isDynamic clojure.lang.Var/.getTag]))))
(ns dustingetz.namespace-explorer
  (:require [hyperfiddle.hfql1 :as hfql :refer [hfql]]
            [hyperfiddle.hfql1.coalgebra :refer [hfqlT]]))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn ns-publics2 [!ns] (vals (ns-publics !ns)))) ; collection-record form
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists seq pr-str)))

#?(:clj (def sitemap
          (hfql {(all-ns) ^{::hfql/select '(ns-publics %)} [ns-name]
                 ;; ns-publics2 [symbol]
                 ns-publics {* val}})))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [ns] `(find-ns ~(ns-name ns)))
          hfql/Suggestable (-suggest [_] nil #_(hfql [ns-name doc author
                                                ^{::hfql/select '(ns-publics2 %)} ns-publics2
                                                meta]))))

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [ns] (symbol ns))
          hfql/Suggestable (-suggest [_]
                             #_(hfql [symbol var-arglists doc meta .isMacro .isDynamic .getTag])
                             [symbol var-arglists doc meta])))
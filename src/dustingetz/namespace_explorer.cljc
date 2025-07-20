(ns dustingetz.namespace-explorer
  (:require [hyperfiddle.hfql0 #?(:clj :as :cljs :as-alias) hfql]))

#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn author [!x] (-> !x meta :author)))
#?(:clj (defn ns-publics2 [ns-sym] (-> ns-sym ns-publics vals)))
#?(:clj (defn var-name [!var] (-> !var symbol name symbol)))
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists vec str)))

#?(:clj (def sitemap
          (hfql/sitemap
            {all-ns (hfql/props [ns-name doc author] {::hfql/select (ns-publics2 %)})
             ns-publics2 (hfql/props [var-name] {::hfql/select (resolve %)})
             resolve []})))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [ns] (ns-name ns))
          hfql/Suggestable (-suggest [_] (hfql/pull-spec [ns-name ns-publics2 doc author meta]))))

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [x] (symbol x))
          hfql/Suggestable (-suggest [x] (hfql/pull-spec [var-name var-arglists doc meta .isMacro .getTag]))))

(ns dustingetz.namespace-explorer
  (:require
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.electric-dom3 :as dom]
    #?(:clj [hyperfiddle.hfql0 :as hfql :refer [Identifiable Suggestable]]
       :cljs [hyperfiddle.hfql0 :as-alias hfql])
    [hyperfiddle.navigator4 :refer [HfqlRoot]]))

#?(:clj (defn clojure-all-ns [] (vec (sort-by ns-name (all-ns)))))
#?(:clj (defn doc [!x] (-> !x meta :doc)))
#?(:clj (defn ns-publics2 [$ns] (-> $ns ns-publics vals)))
#?(:clj (defn ns-publics-count [!ns] (count (ns-publics !ns))))
#?(:clj (defn var-arglists [!var] (->> !var meta :arglists str)))
#?(:clj (defn var-name [!var] (-> !var symbol name symbol)))

#?(:clj (extend-type clojure.lang.Var
          Identifiable (-identify [x] (symbol x))
          Suggestable (-suggest [_] (hfql/pull-spec [var-name var-arglists doc meta
                                                     .getTag .isMacro]))))

#?(:clj (extend-type clojure.lang.Namespace
          Identifiable (-identify [^clojure.lang.Namespace ns] (ns-name ns))
          Suggestable (-suggest [_] (hfql/pull-spec [ns-name doc meta ns-publics-count ns-publics ns-imports ns-interns]))))

#?(:clj (def site-map
          (hfql/sitemap
            {clojure-all-ns (hfql/props [ns-name doc] {::hfql/select (ns-publics2 %)})
             #_#_find-ns [ns-name (hfql/props "ns-publics2" {::hfql/select (ns-publics2 %)})]
             ns-publics2 (hfql/props [var-name] {::hfql/select (resolve %)})
             resolve [var-name type]})))

(e/defn NamespaceExplorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (HfqlRoot site-map `[(clojure-all-ns)]))

(comment
  (hfql/suggest-java-class-members *ns*)
  (hfql/suggest-java-class-members (first (ns-publics2 'clojure.core)))
  (ns-publics (find-ns 'clojure.core)))

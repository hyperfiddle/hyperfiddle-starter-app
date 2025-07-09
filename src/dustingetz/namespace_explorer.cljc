(ns dustingetz.namespace-explorer
  (:require
    [clojure.string :as str]
    [hyperfiddle.electric-dom3 :as dom]
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.hfql0 #?(:clj :as, :cljs :as-alias) hfql]
    #?(:clj [hyperfiddle.sitemap :refer [sitemap pull-spec]]) ; TODO merge with hfql0
    [hyperfiddle.navigator4 :refer [HfqlRoot]]))

#?(:clj (defn clojure-all-ns [] (vec (sort-by ns-name (all-ns)))))
#?(:clj (defn ns-doc [ns] (-> ns meta :doc)))
#?(:clj (defn ns-publics* [_] '...))
#?(:clj (defn public-vars [ns$] (->> ns$ find-ns ns-publics vals (sort-by symbol))))
#?(:clj (defn var-detail [var$] (resolve var$)))
#?(:clj (defn var-name [vr] (-> vr symbol name symbol)))
#?(:clj (defn var-doc [vr] (-> vr meta :doc)))
#?(:clj (defn var-macro? [vr] (.isMacro ^clojure.lang.Var vr)))
#?(:clj (defn var-arglists [vr] (->> vr meta :arglists (str/join " ") symbol))) ; lol

#?(:clj (extend-type clojure.lang.Var
          hfql/Identifiable (-identify [x] (symbol x))
          hfql/Suggestable (-suggest [_] (pull-spec [.toSymbol meta .getTag .isMacro]))))

#?(:clj (extend-type clojure.lang.Namespace
          hfql/Identifiable (-identify [^clojure.lang.Namespace ns] (ns-name ns))))

#?(:clj (def site-map
          (sitemap
            {clojure-all-ns (hfql/props [ns-name ns-doc]
                              {::hfql/select (find-ns %)})
             find-ns [ns-name
                      ns-doc
                      meta
                      (hfql/props ns-publics* {::hfql/select (public-vars %)})
                      ns-imports
                      ns-interns]
             public-vars (hfql/props [var-name var-doc]
                           {::hfql/select (var-detail %)})
             var-detail [var-name var-doc meta var-macro? var-arglists]})))

(e/defn NamespaceExplorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (HfqlRoot site-map `[(clojure-all-ns)]))

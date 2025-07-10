(ns dustingetz.file-explorer
  #?(:clj (:import [java.nio.file Path Paths Files]
                   [java.nio.file.attribute BasicFileAttributes FileTime]
                   [java.io File]))
  (:require
    #?(:clj clojure.java.io)
    [hyperfiddle.electric-dom3 :as dom]
    [hyperfiddle.electric3 :as e]
    [hyperfiddle.hfql0 #?(:clj :as, :cljs :as-alias) hfql]
    [hyperfiddle.navigator4 :refer [HfqlRoot]]))

(defn get-extension [?path]
  (when ?path
    (when-not (= \. (first ?path)) ; hidden
      (some-> (last (re-find #"(\.[a-zA-Z0-9]+)$" ?path))
              (subs 1)))))

#?(:clj (defn file-path "get java.nio.file.Path of j.n.f.File" [^java.io.File f]
          (-> f .getAbsolutePath (java.nio.file.Paths/get (make-array String 0)))))
#?(:clj (defn path-attrs [^Path p] (Files/readAttributes p BasicFileAttributes (make-array java.nio.file.LinkOption 0))))
#?(:clj (defn file-attrs [^File f] (path-attrs (file-path f))))
#?(:clj (defn file-order-compare [^File x] [(not (.isDirectory x)) (.getName x)]))
#?(:clj (defn dir-list [^File x] (some->> x .listFiles (sort-by file-order-compare) vec)))
#?(:clj (defn dir-parent [^File x] (some-> x file-path .getParent .toFile)))
#?(:clj (defn file-modified [^File x] (let [attrs (file-attrs x)] (-> attrs .lastModifiedTime .toInstant java.util.Date/from))))
#?(:clj (defn file-size [^File x] (.size (file-attrs x))))

#?(:clj (defn file-kind [^File x]
          (let [attrs (file-attrs x)]
            (cond (.isDirectory attrs) ::dir
                  (.isSymbolicLink attrs) ::symlink
                  (.isOther attrs) ::other
                  (.isRegularFile attrs) (if-let [s (get-extension (.getName x))]
                                           (keyword (namespace ::foo) s)
                                           ::unknown-kind)
                  () ::unknown-kind))))

#?(:clj (extend-type java.io.File
          hfql/Identifiable (-identify [^File x] (.getName x))
          hfql/Suggestable
          (-suggest [_]
            (hfql/pull-spec [.getName
                             (hfql/props {file-kind name}
                               {::hfql/label file-kind})
                             file-modified
                             file-size
                             dir-list
                             .listFiles
                             dir-parent]))))

#?(:clj (def site-map (hfql/sitemap
                        {clojure.java.io/file []})))

(e/defn FileExplorer []
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/electric-forms.css"}))
  (dom/link (dom/props {:rel :stylesheet :href "/hyperfiddle/datomic-browser.css"})) ; TODO remove
  (HfqlRoot site-map '[(clojure.java.io/file ".")]))

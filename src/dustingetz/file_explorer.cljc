(ns dustingetz.file-explorer
  #?(:clj (:import [java.nio.file Path Paths Files]
                   [java.nio.file.attribute BasicFileAttributes FileTime]
                   [java.io File]))
  (:require
    #?(:clj clojure.java.io)
    [hyperfiddle.hfql0 #?(:clj :as, :cljs :as-alias) hfql]))

#?(:clj (defn file-order-compare [^File x] [(not (.isDirectory x)) (.getName x)]))
#?(:clj (defn dir-list [^String file-path] (.listFiles (clojure.java.io/file file-path))))
#?(:clj (defn path-attrs [^Path p] (Files/readAttributes p BasicFileAttributes (make-array java.nio.file.LinkOption 0))))
#?(:clj (defn file-path "get java.nio.file.Path of j.n.f.File" [^java.io.File f]
          (-> f .getAbsolutePath (java.nio.file.Paths/get (make-array String 0)))))
#?(:clj (defn file-attrs [^File f] (path-attrs (file-path f))))
;#?(:clj (defn dir-parent [^File x] (some-> x file-path .getParent .toFile)))
#?(:clj (defn file-modified [^File x] (let [attrs (file-attrs x)] (-> attrs .lastModifiedTime .toInstant java.util.Date/from))))
#?(:clj (defn file-size [^File x] (.size (file-attrs x))))

(defn get-extension [?path]
  (when ?path
    (when-not (= \. (first ?path)) ; hidden
      (some-> (last (re-find #"(\.[a-zA-Z0-9]+)$" ?path))
              (subs 1)))))

#?(:clj (defn file-kind [^File x]
          (let [attrs (file-attrs x)]
            (cond (.isDirectory attrs) ::dir
                  (.isSymbolicLink attrs) ::symlink
                  (.isOther attrs) ::other
                  (.isRegularFile attrs) (if-let [s (get-extension (.getName x))]
                                           (keyword (namespace ::foo) s)
                                           ::unknown-kind)
                  () ::unknown-kind))))

#?(:clj (defn file-relative-path [root-file file]
          (-> (.toURI root-file)
            (.relativize (.toURI file))
            (.getPath))))

#?(:clj (extend-type java.io.File
          hfql/Identifiable (-identify [^File x]
                              (file-relative-path (clojure.java.io/file ".") x))
          hfql/Suggestable
          (-suggest [o]
            (hfql/pull-spec
              [.getName
               .getAbsolutePath
               .lastModified
               .listFiles
               {file-kind name}
               file-modified
               file-size]))))

#?(:clj (def sitemap
          (hfql/sitemap
            {clojure.java.io/file []
             dir-list (hfql/props [] {::hfql/select (dir-list %)})})))

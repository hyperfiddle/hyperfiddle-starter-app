(ns dustingetz.nav-file
  (:import
   [java.io File]
   [java.nio.file Path Paths Files LinkOption]
   [java.nio.file.attribute BasicFileAttributes])
  (:require
   [clojure.java.io :refer [file]]
   [hyperfiddle.hfql2 :as hfql :refer [hfql]]
   [hyperfiddle.hfql2.protocols :refer [Identifiable Suggestable -hfql-resolve]]))

(defn jpath-jattrs [^Path !p] (Files/readAttributes !p BasicFileAttributes (make-array LinkOption 0)))
(defn jfile-jpath [^File !f] (-> !f .getAbsolutePath (Paths/get (make-array String 0))))
(defn jfile-jattrs [^File !f] (jpath-jattrs (jfile-jpath !f)))
(defn jfile-modified [^File !f] (let [attrs (jfile-jattrs !f)] (-> attrs .lastModifiedTime .toInstant java.util.Date/from)))

(defn jfile-extension [^File !f]
  (when-let [?path (.getPath !f)]
    (when-not (= \. (first ?path)) ; hidden
      (some-> (last (re-find #"(\.[a-zA-Z0-9]+)$" ?path))
        (subs 1)))))

(defn jfile-kind [^File !f]
  (let [attrs (jfile-jattrs !f)]
    (cond (.isDirectory attrs) ::dir
      (.isSymbolicLink attrs) ::symlink
      (.isOther attrs) ::other
      (.isRegularFile attrs) (if-let [s (jfile-extension !f)]
                               (keyword (namespace ::foo) s)
                               ::unknown-kind)
      () ::unknown-kind)))

(extend-type File
  Identifiable (-identify [^File !f] `(file ~(.getPath !f)))
  Suggestable (-suggest [^File !f]
                (hfql [.getName
                       .getPath
                       .getAbsolutePath
                       {jfile-jattrs [.isRegularFile .isDirectory .isSymbolicLink .isOther]}
                       {jfile-kind name} ; edge threading
                       jfile-modified ; #inst example
                       {.listFiles {* ...}}])))

(def sitemap
  {'file (hfql [.getName {.listFiles {* ...}}])})

(defmethod -hfql-resolve `file [[_ file-path-str]] (file file-path-str))

(ns dustingetz.file-explorer
  #?(:clj (:import
           [java.io File]
           [java.nio.file Path Paths Files LinkOption]
           [java.nio.file.attribute BasicFileAttributes]))
  (:require
   #?(:clj [clojure.java.io :refer [file]])
   [hyperfiddle.hfql2 :as hfql :refer [hfql]]
   [hyperfiddle.hfql2.protocols :refer [Identifiable Suggestable hfql-resolve]]))

#?(:clj (defn jpath-jattrs [^Path !p] (Files/readAttributes !p BasicFileAttributes (make-array LinkOption 0))))
#?(:clj (defn jfile-jpath [^File !f] (-> !f .getAbsolutePath (Paths/get (make-array String 0)))))
#?(:clj (defn jfile-jattrs [^File !f] (jpath-jattrs (jfile-jpath !f))))
#?(:clj (defn jfile-modified [^File !f] (let [attrs (jfile-jattrs !f)] (-> attrs .lastModifiedTime .toInstant java.util.Date/from))))

#?(:clj (defn jfile-extension [^File !f]
          (when-let [?path (.getPath !f)]
            (when-not (= \. (first ?path)) ; hidden
              (some-> (last (re-find #"(\.[a-zA-Z0-9]+)$" ?path))
                (subs 1))))))

#?(:clj (defn jfile-kind [^File !f]
          (let [attrs (jfile-jattrs !f)]
            (cond (.isDirectory attrs) ::dir
              (.isSymbolicLink attrs) ::symlink
              (.isOther attrs) ::other
              (.isRegularFile attrs) (if-let [s (jfile-extension (.getName !f))]
                                       (keyword (namespace ::foo) s)
                                       ::unknown-kind)
              () ::unknown-kind))))

#?(:clj (extend-type File
          Identifiable (identify [^File !f] `(file ~(.getPath !f)))
          Suggestable (suggest [^File !f]
                        (hfql [.getName
                               .getPath
                               .getAbsolutePath
                               {jfile-jattrs [.isRegularFile .isDirectory .isSymbolicLink .isOther]}
                               {jfile-kind name} ; edge threading
                               jfile-modified ; #inst example
                               {.listFiles {* ...}}]))))

#?(:clj (def sitemap
          {'file (hfql [.getName {.listFiles {* ...}}])}))

#?(:clj (defmethod hfql-resolve `file [[_ file-path-str]] (file file-path-str)))

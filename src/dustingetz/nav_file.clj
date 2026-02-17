(ns dustingetz.nav-file
  (:import [java.io File])
  (:require
   [clojure.java.io :as io]
   [dustingetz.fs2 :as fs]
   [hyperfiddle.hfql2 :as hfql :refer [hfql]]
   [hyperfiddle.hfql2.protocols :refer [Suggestable]]))

(defonce ^:dynamic *base-dir* (fs/absolute-path "."))

(extend-type File
  Suggestable (-suggest [^File !f]
                (hfql [.getName
                       .getPath
                       .getAbsolutePath
                       {fs/jfile-jattrs [.isRegularFile .isDirectory .isSymbolicLink .isOther]}
                       {fs/jfile-kind name} ; edge threading
                       fs/jfile-modified ; #inst example
                       {.listFiles {* ...}}])))

(defn files [] (.listFiles (io/file *base-dir*)))

(def sitemap
  {`files (hfql {(files) {* [.getName {.listFiles {* ...}}]}})})

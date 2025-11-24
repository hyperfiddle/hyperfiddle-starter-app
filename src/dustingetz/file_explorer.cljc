(ns dustingetz.file-explorer
  #?(:clj (:import [java.io File]))
  (:require #?(:clj clojure.java.io)
            #?(:clj [dustingetz.fs2 :as fs])
            [hyperfiddle.hfql2 :as hfql :refer [hfql]]))

#?(:clj (extend-type File
          hfql/Identifiable (-identify [^File o] `(clojure.java.io/file ~(.getPath o)))
          hfql/Suggestable (-suggest [o] nil
                             (hfql [File/.getName
                                    File/.getPath
                                    File/.getAbsolutePath
                                    {fs/jfile-kind name} ; edge threading
                                    fs/jfile-modified ; #inst example
                                    {File/.listFiles {* ...}}]))))

#?(:clj (def sitemap
          {`file     (hfql [File/.getName {File/.listFiles {* ...}}])
           `dir-list (hfql {File/.listFiles {* [File/.getName ...]}})}))

#?(:clj (defmethod hfql/resolve 'clojure.java.io/file [[_ file-path-str]] (clojure.java.io/file file-path-str)))

; Homework

; 1. Implement Suggestable for a File. Show the file's name and lastModifiedTime using java methods
; on the object. The dustingetz.fs2 namespace contains some helper files, play around and add more
; optional columns. How can we render lastModifiedTime as a date? How can we show the list of files
; in the folder?
; Hint: (-suggest [o] (hfql [.getName]))
; Hint: fs/jfile-modified
; Hint: .listFiles

; 2. Make a new route that starts at a folder, showing it's contents, and then navigating from
; folder to folder recursively, so that when you select a folder, its children open in the subsequent view.
; Hint: fs/dir-list (can't route to a java method .listFiles yet)
; Hint: recursive ::hfql/select target
; Hint: (-identify [^File o] (fs/file-path "." o))
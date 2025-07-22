(ns ^{:hyperfiddle.electric.impl.lang3/has-edef? true} ; enable server hot reloading
  dustingetz.file-explorer
  #?(:clj (:import [java.io File]))
  (:require #?(:clj clojure.java.io)
            #?(:clj [dustingetz.fs2 :as fs])
            [hyperfiddle.hfql0 #?(:clj :as, :cljs :as-alias) hfql]))

#?(:clj (extend-type File
          hfql/Identifiable (-identify [^File o] (fs/file-path "." o))
          hfql/Suggestable (-suggest [o]
                             (hfql/pull-spec
                               [.getName
                                .getAbsolutePath
                                {fs/file-kind name} ; edge threading
                                fs/file-modified ; #inst example
                                .listFiles]))))

#?(:clj (def sitemap
          (hfql/sitemap
            {clojure.java.io/file [.getName]
             fs/dir-list (hfql/props [] {::hfql/select (fs/dir-list %)})})))


; Homework

; 1. Implement Suggestable for a File. Show the file's name and lastModifiedTime using java methods
; on the object. The dustingetz.fs2 namespace contains some helper files, play around and add more
; optional columns. How can we render lastModifiedTime as a date?

; 2. Make a new route that starts at a folder, showing it's contents, and then navigating from
; folder to folder recursively, so that when you select a folder, its children open in the subsequent view.
; Hint: fs/dir-list
; Hint: recursive ::hfql/select target
; Hint: (-identify [^File o] (fs/file-path "." o))
(ns ^{:hyperfiddle.electric.impl.lang3/has-edef? true} ; enable server hot reloading
  dustingetz.file-explorer
  #?(:clj (:import [java.io File]))
  (:require #?(:clj clojure.java.io)
            #?(:clj [dustingetz.fs2 :as fs])
            #_[hyperfiddle.hfql0 :as hfql #?(:clj :as, :cljs :as-alias) hfql]
            [hyperfiddle.hfql1 :as hfql :refer [hfql]]))

#?(:clj (extend-type File
          hfql/Identifiable (-identify [^File o] (fs/jfile-path "." o))
          hfql/Suggestable (-suggest [o]
                             (hfql
                               [.getName
                                .getAbsolutePath
                                ;; {fs/jfile-kind name} ; edge threading
                                ;; fs/jfile-modified ; #inst example
                                .listFiles]))))

#?(:clj (def sitemap
          (hfql
            {clojure.java.io/file [.getName]
             fs/dir-list (hfql [] {::hfql/select '(fs/dir-list %)})})))


; Homework

; 1. Implement Suggestable for a File. Show the file's name and lastModifiedTime using java methods
; on the object. The dustingetz.fs2 namespace contains some helper files, play around and add more
; optional columns. How can we render lastModifiedTime as a date? How can we show the list of files
; in the folder?
; Hint: (-suggest [o] (hfql/pull-spec [.getName]))
; Hint: fs/jfile-modified
; Hint: .listFiles

; 2. Make a new route that starts at a folder, showing it's contents, and then navigating from
; folder to folder recursively, so that when you select a folder, its children open in the subsequent view.
; Hint: fs/dir-list (can't route to a java method .listFiles yet)
; Hint: recursive ::hfql/select target
; Hint: (-identify [^File o] (fs/file-path "." o))
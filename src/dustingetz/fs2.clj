(ns dustingetz.fs2
  (:import [clojure.lang ExceptionInfo]
           [java.io File]
           [java.nio.file Path Paths Files]
           [java.nio.file.attribute BasicFileAttributes FileTime])
  (:require clojure.java.io
            [contrib.assert :refer [check]]
            [hyperfiddle.rcf :refer [tests]]))

(defn absolute-path [^String path-str & more]
  (-> (java.nio.file.Path/of ^String path-str (into-array String more))
    .normalize .toAbsolutePath str))

(tests
  (absolute-path "./") := "/Users/dustin/src/hf/monorepo/hyperfiddle-starter-app2"
  (absolute-path "node_modules") := "/Users/dustin/src/hf/monorepo/hyperfiddle-starter-app2/node_modules"
  (absolute-path "") := "/Users/dustin/src/hf/monorepo/hyperfiddle-starter-app2"
  (absolute-path nil) :throws NullPointerException
  (absolute-path "path-does-not-exist") := "/Users/dustin/src/hf/monorepo/hyperfiddle-starter-app2/path-does-not-exist")

(defn maybe-file
  "Wraps clojure.java.io/file, but returns nil when file doesn't exist"
  [& args]
  (let [f (apply clojure.java.io/file args)]
    (if (.exists f) f nil)))

(tests
  (some? (maybe-file (absolute-path "./"))) := true
  (some? (maybe-file (absolute-path "./does-not-exist"))) := false)

(defn ensure-file [filepath initial-content]
  (let [!f (clojure.java.io/file filepath)]
    (when-not (.exists !f)
      (spit !f initial-content))))

(defn dir-list [^String file-path] (.listFiles (clojure.java.io/file file-path)))

(defn jpath-jattrs [^Path p] (Files/readAttributes p BasicFileAttributes (make-array java.nio.file.LinkOption 0)))

(defn jfile-jpath "get java.nio.file.Path of j.n.f.File"
  [^File f]
  (-> f .getAbsolutePath (java.nio.file.Paths/get (make-array String 0))))

(defn jfile-jattrs [^File f] (jpath-jattrs (jfile-jpath f)))
(defn file-jattrs [filepath]
  (println `file-jattrs (pr-str filepath))
  (jpath-jattrs (jfile-jpath (check (maybe-file filepath)))))

(defn jfile-modified [^File x] (let [attrs (jfile-jattrs x)] (-> attrs .lastModifiedTime .toInstant java.util.Date/from)))
(defn jfile-size [^File x] (.size (jfile-jattrs x)))

(defn relativize-path "Convert an absolute path to one relative to base-dir"
  [base-dir abs-path]
  (let [base (.toPath (clojure.java.io/file (check base-dir)))
        full (.toPath (clojure.java.io/file (check abs-path)))]
    (when (-> full .normalize (.startsWith (.normalize base)))
      (str (.relativize (.normalize base) (.normalize full))))))

(tests
  (relativize-path (absolute-path "./") (absolute-path "./src/dustingetz")) := "src/dustingetz"
  (relativize-path (absolute-path "./") (absolute-path "./src/dustingetz/")) := "src/dustingetz"
  (relativize-path (absolute-path "./") (absolute-path "src/dustingetz")) := "src/dustingetz"
  (relativize-path (absolute-path "./") (absolute-path "src/dustingetz/")) := "src/dustingetz"
  (relativize-path (absolute-path "../") (absolute-path "src/dustingetz")) := "hyperfiddle-starter-app2/src/dustingetz"
  (relativize-path (absolute-path "./") (absolute-path "./")) := ""
  (relativize-path (absolute-path "./") (absolute-path "")) := ""
  (relativize-path (absolute-path "./") (absolute-path "../")) := nil
  (relativize-path "/fake/" (absolute-path "./src")) := nil
  (relativize-path (absolute-path "./") "/fake/") := nil
  (relativize-path (absolute-path "./") "fake") := nil
  (relativize-path (absolute-path ".") "src/dustingetz/fs2.clj"))

(defn jfile-path
  ([^File f] (-> f .toPath .normalize .toAbsolutePath str))
  ([base-dir, ^File !file] (relativize-path (absolute-path base-dir) (jfile-path !file))))

(comment
  (jfile-path (maybe-file (absolute-path "./src"))) := "/Users/dustin/src/hf/monorepo/hyperfiddle-starter-app2/src"
  (jfile-path "." (maybe-file (absolute-path *1))) := "src")

(defn file-extension [?path]
  (when ?path
    (when-not (= \. (first ?path)) ; hidden
      (some-> (last (re-find #"(\.[a-zA-Z0-9]+)$" ?path))
        (subs 1)))))

(tests
  (file-extension nil) := nil
  (file-extension "") := nil
  (file-extension ".") := nil
  (file-extension "..") := nil
  (file-extension "image") := nil
  (file-extension "image.") := nil
  (file-extension "image..") := nil
  (file-extension "image.png") := "png"
  (file-extension "image.blah.png") := "png"
  (file-extension "image.blah..png") := "png"
  (file-extension ".png") := nil
  (file-extension ".gitignore") := nil)

(defn jfile-kind [^File x]
  (let [attrs (jfile-jattrs x)]
    (cond (.isDirectory attrs) ::dir
          (.isSymbolicLink attrs) ::symlink
          (.isOther attrs) ::other
          (.isRegularFile attrs) (if-let [s (file-extension (.getName x))]
                                   (keyword (namespace ::foo) s)
                                   ::unknown-kind)
          () ::unknown-kind)))

(defn jfile-extension-predicate [ext-set, ^File !file]
  (and (.isFile !file)
    (some? ((set ext-set) (file-extension (.getName !file))))))

(defn dir-children [rel-dirpath f?] ; just that layer
  (->> (seq (.listFiles (check (maybe-file rel-dirpath))))
    (filter f?)
    (map #(.getPath %))))

(tests
  (dir-children "src/dustingetz" (partial jfile-extension-predicate #{"clj" "cljs" "cljc"}))
  := ["src/dustingetz/fs2.clj"
      "src/dustingetz/hello.cljc"
      "src/dustingetz/hyperfiddle_demo.cljc"
      "src/dustingetz/file_explorer.cljc"
      "src/dustingetz/namespace_explorer.cljc"])

(def treeseq-dir-files
  (memoize
    (fn [rel-dirpath pred?]
      (->> (file-seq (check (maybe-file rel-dirpath)))
        (filter #(.isFile %))
        (filter pred?)
        (map #(.getPath %))))))

(tests
  (treeseq-dir-files "src" (partial jfile-extension-predicate #{"clj"}))
  := ["src/dustingetz/fs2.clj"]
  (time (count (treeseq-dir-files "src" #{"clj" "cljs" "cljc"})))
  (time (count (distinct (treeseq-dir-files "src" #{"clj" "cljs" "cljc"})))))

(def treeseq-dir-folders
  (memoize
    (fn [rel-dirpath]
      (->> (file-seq (check (maybe-file rel-dirpath)))
        (filter #(.isDirectory %))
        (map #(.getPath %))))))

(tests
  (treeseq-dir-folders "src") := ["src" "src/dustingetz"])

#_
(defn path-parent "slow version"
  [filepath-str]
  (when filepath-str
    (->> (clojure.string/split filepath-str #"/")
      reverse (drop-while clojure.string/blank?) reverse
      (butlast) ; first non-blank segment
      (clojure.string/join "/"))))

;(defn path-parent "parent is the butlast on the path segments"
;  [filepath-str]
;  (when filepath-str
;    (when-let [last-slash (.lastIndexOf filepath-str "/")] ; why stringwise
;      (when (pos? last-slash)
;        (subs filepath-str 0 last-slash)))))
;
;(tests
;  (path-parent "foo/bar/baz") := "foo/bar"
;  (path-parent "/foo/bar/baz") := "/foo/bar"
;  (path-parent "//foo/bar/baz") := "//foo/bar"
;  (path-parent "foo") := nil
;  (path-parent "./src") := "."
;  (path-parent "/") := nil
;  (path-parent "foo.txt") := nil
;  (path-parent "/foo.txt") := nil
;  (path-parent "/bar/foo.txt") := "/bar"
;  (path-parent nil) := nil)

(defn path-filename [filepath-str]
  (when filepath-str
    (.getName (check (File. filepath-str))))) ; doesn't have to exist

(tests
  (path-filename "foo/bar/baz.txt") := "baz.txt"
  (path-filename "/foo/bar/baz.txt") := "baz.txt"
  (path-filename "//foo/bar/baz.txt") := "baz.txt"
  (path-filename "foo.txt") := "foo.txt"
  (path-filename "foo") := "foo"
  (path-filename "") := ""
  ;(path-filename "/") := nil - broken, returns ""
  ;(path-filename "/foo/") := "" - broken, returns "foo"
  (path-filename nil) := nil)

(defn file-dir [filepath-str]
  (let [s (absolute-path filepath-str) ; resolve and normalize "./" etc
        !f (check (maybe-file s))
        _ (check true? (.isFile !f))
        !p (check (.getParentFile !f))
        _ (check true? (.isDirectory !p))]
    (check (relativize-path (check (absolute-path ".")) (check (.getPath !p))))))

(tests
  (file-dir "src/dustingetz/fs2.clj") := "src/dustingetz"
  (file-dir "./src/dustingetz/fs2.clj") := "src/dustingetz"
  (file-dir (str (absolute-path "./") "/src/dustingetz/fs2.clj")) := "src/dustingetz"
  (file-dir (str (absolute-path "./") "///././//src/dustingetz/fs2.clj")) := "src/dustingetz"
  (file-dir "src/dustingetz") :throws ExceptionInfo ; not a file
  (file-dir "") :throws ExceptionInfo ; not a file
  (file-dir "./") :throws ExceptionInfo ; not a file
  (file-dir nil) :throws NullPointerException
  nil)

(defn dir-name [dirpath-str]
  (let [s (absolute-path dirpath-str) ; resolve and normalize "./" etc
        !f (check some? (maybe-file s))]
    (check (.isDirectory !f))
    (check (.getName !f))))

(tests
  (dir-name "src/dustingetz/fs2.clj")
  (dir-name "/usr/local") := "local"
  (dir-name "src/dustingetz") := "dustingetz"
  (dir-name "src") := "src"
  (dir-name "./src/dustingetz") := "dustingetz"
  (dir-name "./") := "hyperfiddle-starter-app2"
  (dir-name "") := "hyperfiddle-starter-app2"
  (dir-name "foo/bar") :throws ExceptionInfo
  (dir-name "/foo/bar") :throws ExceptionInfo
  (dir-name "./foo/bar") :throws ExceptionInfo
  (dir-name nil) :throws NullPointerException
  nil)

(defn strip-suffix [s sufs]
  (if-let [matching-suf (some #(when (clojure.string/ends-with? s %) %) sufs)]
    (subs s 0 (- (count s) (count matching-suf)))
    s))

(tests
  (strip-suffix "src/dustingetz/fs2.clj" #{".cljs" ".clj"}) := "src/dustingetz/fs2")

(defn strip-prefix [s pre]
  (if (clojure.string/starts-with? s pre)
    (subs s (count pre))
    s))

(tests
  (strip-prefix "src/dustingetz/fs2.clj" "src/") := "dustingetz/fs2.clj"
  (strip-prefix "src/dustingetz/fs2.clj" "/not-the-prefix") := "src/dustingetz/fs2.clj")

(defn prefix-path [a b]
  (->> (concat (clojure.string/split a #"/") (clojure.string/split b #"/"))
    (flatten)
    (filter some?)
    (clojure.string/join "/")))

;(defn path-rest [filepath-str] ; why
;  (when filepath-str
;    (->> (clojure.string/split filepath-str #"/")
;      (drop-while clojure.string/blank?)
;      (drop 1) ; first non-blank segment
;      (clojure.string/join "/"))))
;
;(comment
;  (path-rest "foo/bar/baz") := "bar/baz"
;  (path-rest "/foo/bar/baz") := "bar/baz"
;  (path-rest "//foo/bar/baz") := "bar/baz"
;  (path-rest "foo") := ""
;  (path-rest "") := ""
;  (path-rest nil) := nil)
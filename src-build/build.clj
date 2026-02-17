(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.logging :as log]))

(def class-dir "target/classes")

(defn uberjar
  "Build uberjar. No client build — compiled JS comes from hyperfiddle-agent JAR.
   Usage: clj -X:build uberjar :build/jar-name '\"app.jar\"'"
  [{:keys [::jar-name :aliases]
    :or {aliases [:prod]}
    :as args}]
  (log/info 'uberjar (pr-str args))
  (b/delete {:path "target"})
  (b/copy-dir {:target-dir class-dir :src-dirs ["src" "src-prod" "resources"]})
  (let [jar-name (or (some-> jar-name str) ; override for Dockerfile builds to avoid needing to reconstruct the name
                   "hyperfiddle-starter-app.jar")]
    (b/uber {:class-dir class-dir
             :uber-file (str "target/" jar-name)
             :basis     (b/create-basis {:project "deps.edn" :aliases aliases})})
    (log/info jar-name)))

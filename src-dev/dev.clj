(ns dev
  "Dev entrypoint. Starts the Starter App with default settings."
  (:require [dustingetz.main :as main]))

(comment (-main))

(defn -main [& _]
  (main/-main))

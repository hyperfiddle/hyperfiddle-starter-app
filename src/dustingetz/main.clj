(ns dustingetz.main
  "Hyperfiddle Starter App — self-contained single process.
   Starts a cloud proxy and auto-connects an explorer agent that serves
   a Clojure namespace browser and filesystem browser.

   Usage:
     clj -M -m dustingetz.main                    ;; defaults (port 8080)
     clj -M -m dustingetz.main http-port 9090     ;; custom port"
  (:require [clojure.tools.logging :as log]
            [dustingetz.fs2 :as fs]
            [hyperfiddle.cloud-proxy :as proxy]
            [hyperfiddle.navigator-agent :as agent]
            [hyperfiddle.nav-cloud-agents :as nav-cloud-agents]
            [dustingetz.nav-clj-ns :as nav-clj-ns]
            [dustingetz.nav-file :as nav-file]))

(defn -main
  "Entrypoint. Starts cloud proxy + auto-connects explorer agent.
   Args are clojure.main style key-value strings."
  [& {:strs [http-port]}]
  (let [port (or (some-> http-port parse-long) 8080)
        sitemap (merge nav-clj-ns/sitemap nav-file/sitemap)]
    (log/info "Starting Starter App" (pr-str {:port port}))

    ;; 1. Start cloud proxy (serves the agent web UI from hyperfiddle-agent JAR)
    (def server (proxy/start-proxy! :port port))

    ;; 2. Auto-connect admin agent (shows connected agents dashboard)
    (def admin-agent
      (agent/connect! (str "ws://localhost:" port "/agent?id=admin")
        nav-cloud-agents/sitemap
        (fn [] {#'nav-cloud-agents/*agents* proxy/agents})))

    ;; 3. Auto-connect explorer agent (namespace + file browser)
    (def explorer-agent
      (agent/connect! (str "ws://localhost:" port "/agent?id=explorer")
        sitemap
        (fn [] {#'nav-file/*base-dir* (fs/absolute-path ".")})))

    (.addShutdownHook (Runtime/getRuntime)
      (Thread. (fn []
                 (agent/disconnect! explorer-agent)
                 (agent/disconnect! admin-agent)
                 (proxy/stop-proxy! server))))

    (log/info (format "Explorer: http://explorer.localhost:%d" port))
    (log/info (format "Admin:    http://admin.localhost:%d" port))))

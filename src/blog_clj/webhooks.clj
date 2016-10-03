(ns blog-clj.webhooks
  (:require [pandect.algo.sha1 :refer [sha1-hmac]]
            [clojure.edn :as edn]
            [ring.util.response :refer [status response]]
            [clojure.java.shell :refer [sh]]
            [blog-clj.sync :refer [sync-blogs]]))

(def config (edn/read-string (slurp "config.clj")))

(defn sync-hook
  [req]
  (let [event-type (get req "x-github-event")
        signature (get req "x-hub-signature")
        post-body (slurp (:body req))
        hook-key (:github-webhook-secret config)
        hmac (str "sha1=" (sha1-hmac post-body hook-key))]
    (cond
      (and (= hmac signature)
           (= event-type "push"))
      (do
        (sh "git" "pull" :dir (:html-path config))
        (sync-blogs)
        "A push event received!")
      (and (= hmac signature)
           (not= event-type "push"))
      (format "Event %s received!" event-type)
      :else (status (response "Fake POST") 403))))

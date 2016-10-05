(ns blog-clj.webhooks
  (:require [pandect.algo.sha1 :refer [sha1-hmac]]
            [clojure.edn :as edn]
            [ring.util.response :refer [status response]]
            [clojure.java.shell :refer [sh]]
            [blog-clj.sync :refer [sync-blogs]]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.set :refer [union difference]]))

(def config (edn/read-string (slurp "config.clj")))

(defn get-file-changes
  [push-payload]
  (let [payload (json/read-str push-payload :key-fn keyword)
        commits (:commits payload)
        is-publish? #(string/starts-with? % "resources/published-html/")
        trim-prefix #(string/replace % "resources/published-html/" "")
        get-changed-files (fn [change-type]
                            (map trim-prefix
                                 (filter is-publish?
                                         (apply union
                                                (map #(set (change-type %))
                                                     commits)))))
        [added removed modified] (map #(set (get-changed-files %)) [:added :removed :modified])
        real-added (difference added removed)
        real-removed (difference removed added)
        real-modified (difference modified added removed)]
    [real-added real-removed real-modified]))

(defn sync-hook
  [req]
  (let [event-type (get (:headers req) "x-github-event")
        signature (get (:headers req) "x-hub-signature")
        post-body (slurp (:body req))
        hook-key (:github-webhook-secret config)
        hmac (str "sha1=" (sha1-hmac post-body hook-key))]
    (cond
      (and (= hmac signature)
           (= event-type "push"))
      (do
        (sh "git" "pull" :dir (:html-path config))
        (apply sync-blogs (get-file-changes post-body))
        "A push event received!")
      (and (= hmac signature)
           (not= event-type "push"))
      (format "Event %s received!" event-type)
      :else (status (response "Fake POST") 403))))

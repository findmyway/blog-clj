(ns blog-clj.sync
  (:require [clojure.java.shell :refer [sh]]
            [environ.core :refer  [env]]
            [hickory.select :as s]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [hickory.render :refer [hickory-to-html]]
            [clojure.string :as string]
            [blog-clj.redis-io :refer [get-all-blogs-titles get-blog new-blog delete-blog update-blog]]
            [clojure.data :refer [diff]]
            [hickory.core :as hk]
            [blog-clj.upload-download :refer [upload]]
            [taoensso.timbre :refer [info]])
  (:import [java.io File]))

(defn refact-node
  [root]
  (let [try-remove (fn [cur-loc]
                     (if (or (= "tags" (:id (:attrs (zip/node cur-loc))))
                             (= "toc" (:class (:attrs (zip/node cur-loc)))))
                       (zip/remove cur-loc)
                       cur-loc))
        try-refact (fn [cur-loc]
                     (let [href (:href (:attrs (zip/node cur-loc)))
                           src (:src (:attrs (zip/node cur-loc)))]
                       (cond
                         (and (not (nil? href)) (string/starts-with? href "../public/"))
                         (do
                           (let [upload-result (upload href)]
                             (info (format "uploade:%s, upload status:%d" href (:status upload-result))))
                           (zip/edit cur-loc #(assoc-in % [:attrs :href] (string/replace href #"\.\./public/" (env :upload-path)))))
                         (and (not (nil? src)) (string/starts-with? src "../public/"))
                         (do
                           (let [upload-result (upload src)]
                             (info (format "uploade:%s, upload status:%d" src (:status upload-result))))
                           (zip/edit cur-loc #(assoc-in % [:attrs :src] (string/replace src #"\.\./public/" (env :upload-path)))))
                         :else cur-loc)))]
    (loop [cur-loc root]
      (if (zip/end? (zip/next cur-loc))
        (zip/root (-> cur-loc
                      try-refact
                      try-remove))
        (recur (-> cur-loc
                   try-refact
                   try-remove
                   zip/next))))))

(defn get-meta
  [file]
  (let [file-abs-path (str (env :html-path) file)
        file-hk (hk/as-hickory (hk/parse (slurp file-abs-path)))
        content-of #(->> file-hk
                         (s/select (s/id %))
                         first
                         :content
                         first)
        toc-str (first
                 (s/select (s/class :toc) file-hk))]
    {:toc toc-str
     :title (string/replace file #"\.html" "")
     :body (refact-node (hickory-zip (first (s/select (s/tag :body) file-hk))))
     :update-time (f/unparse
                   (f/formatter-local  "yyyy-MM-dd HH:mm:ss")
                   (t/to-time-zone
                    (l/local-now)
                    (t/time-zone-for-offset 8)))
     :tags (let [tag-str (content-of :tags)]
             (if (nil? tag-str)
               #{}
               (set (string/split tag-str #","))))}))

(defn sync-blogs
  [added-blogs removed-blogs modified-blogs]
  (info "syncing...")
  (let [all-blogs-infos (get-all-blogs-titles)
        all-htmls-ids (into {} (for [[title id timestamp] all-blogs-infos] [title id]))]
    (do
      (when (not-empty added-blogs)
        (info (format "new blogs found: %s."
                      (string/join ", " added-blogs))))
      (when (not-empty removed-blogs)
        (info (format "blogs to delete: %s."
                      (string/join ", " removed-blogs))))
      (when (not-empty modified-blogs)
        (info (format "blogs are updated: %s."
                      (string/join ", " modified-blogs))))
      (doall (map #(new-blog (get-meta %)) added-blogs))
      (doall (map #(update-blog (let [blog-meta (get-meta %)]
                                  (assoc blog-meta
                                         :blog-id
                                         (get all-htmls-ids
                                              (:title blog-meta)))))
                  modified-blogs))
      (doall (map
              #(delete-blog (get all-htmls-ids (string/replace % #"\.html" "")))
              removed-blogs)))))

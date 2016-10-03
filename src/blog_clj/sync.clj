(ns blog-clj.sync
  (:require [clojure.java.shell :refer [sh]]
            [clojure.edn :as edn]
            [hickory.select :as s]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [clj-time.format :as f]
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

(def config (edn/read-string (slurp "config.clj")))

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
                           (zip/edit cur-loc #(assoc-in % [:attrs :href] (string/replace href #"\.\./public/" (:upload-path config)))))
                         (and (not (nil? src)) (string/starts-with? src "../public/"))
                         (do
                           (let [upload-result (upload src)]
                             (info (format "uploade:%s, upload status:%d" src (:status upload-result))))
                           (zip/edit cur-loc #(assoc-in % [:attrs :src] (string/replace src #"\.\./public/" (:upload-path config)))))
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
  (let [file-abs-path (str (:html-path config) file)
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
                    (c/from-long (.lastModified (File. file-abs-path)))
                    (t/time-zone-for-offset 8)))
     :tags (set (string/split (content-of :tags) #","))}))

(defn sync-blogs
  []
  (info "syncing...")
  (let [html-dir (:html-path config)
        all-htmls (string/split (:out (sh "ls" html-dir)) #"\n")
        all-htmls-updatetime (map #(.lastModified (File. (str html-dir %))) all-htmls)
        all-blogs-infos (get-all-blogs-titles)
        all-htmls-ids (into {} (for [[title id timestamp] all-blogs-infos] [title id]))
        new-stats (zipmap all-htmls all-htmls-updatetime)
        old-stats (into {} (map #(vector (str (first %) ".html") (edn/read-string (last %))) all-blogs-infos))
        [changed-new-blogs changed-old-blogs _] (diff new-stats old-stats)
        [new-blogs delete-blogs updated-blogs] (diff (set (keys changed-new-blogs)) (set (keys changed-old-blogs)))]
    (do
      (when (not-empty new-blogs)
        (info (format "new blogs found: %s."
                      (string/join ", " new-blogs))))
      (when (not-empty delete-blogs)
        (info (format "blogs to delete: %s."
                      (string/join ", " delete-blogs))))
      (when (not-empty updated-blogs)
        (info (format "blogs are updated: %s."
                      (string/join ", " updated-blogs))))
      (doall (map #(new-blog (get-meta %)) new-blogs))
      (doall (map #(update-blog (let [blog-meta (get-meta %)]
                                  (assoc blog-meta
                                         :blog-id
                                         (get all-htmls-ids
                                              (:title blog-meta)))))
                  updated-blogs))
      (doall (map
              #(delete-blog (get all-htmls-ids (string/replace % #"\.html" "")))
              delete-blogs)))))

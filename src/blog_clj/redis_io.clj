(ns blog-clj.redis-io
  (:require [taoensso.carmine :as car :refer  (wcar)]
            [clojure.set :refer (difference)]
            [clj-time.local :as l]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

(def server1-conn {:pool {} :spec {}})

(defmacro wcar*  [& body] `(car/wcar server1-conn ~@body))

(def ^:dynamic site "tianjun.me:")  ; global prefix
(defn mk-key [k] #(str site k %))
(def blog-key (mk-key "blog_"))
(def tag-key (mk-key "tag_"))
(defn all-blogs [] (str site "all_blogs"))
(defn blog-id-key [] (str site "blog_id"))

(defn now
  "get current time string"
  []
  (f/unparse
   (f/formatter-local  "yyyy-MM-dd HH:mm:ss")
   (l/local-now)))

(defn get-blog
  "get all contents given a blog-id"
  [blog-id]
  (let [bkey (blog-key blog-id)]
    (apply hash-map (wcar* (car/hgetall bkey)))))

(defn get-blogids-with-tag
  [tag]
  (wcar* (car/smembers (tag-key tag))))

(defn get-all-blogs-titles
  "this function is used to generate blog-roll
  the result is sorted by update-time in default
  
  return ((title blog-id timestamp) ...)
  "
  []
  (let [blog-ids-with-time (partition 2 (wcar* (car/zrevrange (all-blogs) 0 -1 :withscores)))
        titles (wcar* (mapv #(car/hget (blog-key (first %)) :title) blog-ids-with-time))]
    (map #(conj %1 %2) blog-ids-with-time titles)))

(defn- update-tags
  "get the tags to delete and tags to add
  then update the blog's tags and 
  related invert-index-set"
  [blog-id new-tags]
  (let [bkey (blog-key blog-id)
        old-tags (wcar* (car/hget bkey :tags))
        tags-to-add (difference new-tags old-tags)
        tags-to-delete (difference old-tags new-tags)]
    (wcar* (car/hmset bkey :tags new-tags)
           (mapv #(car/sadd (tag-key %) blog-id) tags-to-add)
           (mapv #(car/srem (tag-key %) blog-id) tags-to-delete))))

(defn update-blog
  "update the contents of a blog
  
  `contents` is a map of following keys: 

  `:title`: the blog title
  `:tags`: the tags of blog
  `:body`: the content of blog in markdown
  `:create_time`: the create_time of blog
  `:update_time`: last update time"
  [blog-id contents]
  (let [bkey (blog-key blog-id)
        updated-contents (assoc contents :update_time (now))]
    (when (contains? contents :tags)
      (update-tags blog-id (:tags contents)))
    (wcar* (mapv #(apply car/hmset bkey %)  updated-contents)
           (car/zadd (all-blogs) (c/to-long (now)) blog-id))))

(defn new-blog
  [contents]
  (let [new-blog-id (wcar* (car/incr (blog-id-key)))
        new-contents (assoc contents :create_time (now))]
    (update-blog new-blog-id new-contents)))

(defn delete-blog
  [blog-id]
  (update-tags blog-id [])  ; clean tags and contents
  (wcar* (car/zrem (all-blogs) blog-id)  ; remove from all-blog set
         (car/del (blog-key blog-id))  ; del blog-key
         ))

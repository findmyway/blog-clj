(ns blog-clj.upload-download
  [:require [clj.qiniu :as qiniu]
   [clojure.edn :as edn]
   [clojure.string :as string]])

(def bucket "ontheroad")
(def config (edn/read-string (slurp "config.clj")))

(qiniu/set-config!
 :access-key (:qiniu-ak config)
 :secret-key (:qiniu-sk config)
 :up-host "http://up.qiniug.com")

(defn upload
  [f]
  (let [file-path (str (:html-path config) f)
        upload-key (str "upload/" (last (string/split f #"/")))]
    (qiniu/upload-bucket bucket upload-key file-path)))

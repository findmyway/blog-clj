(ns blog-clj.upload-download
  [:require [clj.qiniu :as qiniu]
   [clojure.edn :as edn]])

(def bucket "ontheroad")
(def config (edn/read-string (slurp "config.clj")))

(qiniu/set-config!
 :access-key (:qiniu-ak config)
 :secret-key (:qiniu-sk config)
 :up-host "http://up.qiniug.com")

(qiniu/bucket-file-seq bucket "upload/")

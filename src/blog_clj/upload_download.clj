(ns blog-clj.upload-download
  (:require [clj.qiniu :as qiniu]
            [environ.core :refer  [env]]
            [clojure.string :as string]))

(def bucket "ontheroad")

(qiniu/set-config!
 :access-key (env :qiniu-ak)
 :secret-key (env :qiniu-sk)
 :up-host "http://up.qiniug.com")

(defn upload
  [f]
  (let [file-path (str (env :html-path) f)
        upload-key (str "upload/" (last (string/split f #"/")))]
    (qiniu/upload-bucket bucket upload-key file-path)))

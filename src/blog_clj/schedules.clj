(ns blog-clj.schedules
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]))

(chime-at  (take 5 (periodic-seq (t/now) (-> 1 t/seconds)))
           (fn  [time]
             (spit "test.txt" (str time "\n") :append true)))

(vec (take 2 (periodic-seq (t/now) (-> 5 t/minutes))))

(ns blog-clj.schedules
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [taoensso.timbre :refer [info]]))

;; start some schedule tasks when server start
;(def job (chime-at  (take 10 (periodic-seq (t/now) (-> 10 t/seconds)))
                    ;(fn  [time]
                      ;(spit "test.txt" (str time "\n") :append true))))

(defn start
  "start some schedules when server starts"
  []
  (info "Starting Server..."))

(defn stop
  "stop some schedules when server shutdown"
  []
  (info "Stoping Server...")
  ;(job)
  )

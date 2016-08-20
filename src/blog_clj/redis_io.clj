(ns blog-clj.redis-io
  (:require [taoensso.carmine :as car :refer  (wcar)]))

(def server1-conn {:pool {} :spec {}})

(defmacro wcar*  [& body] `(car/wcar server1-conn ~@body))

(apply hash-map (wcar*
                 (car/hgetall "blog_2")))

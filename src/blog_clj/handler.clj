(ns blog-clj.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [blog-clj.parse :refer [base-test]]))

(defroutes app-routes
  (GET "/" [] (base-test))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

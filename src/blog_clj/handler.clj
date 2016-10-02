(ns blog-clj.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [blog-clj.page-generators :refer [gen-blog-page
                                              gen-blogtag-search-page
                                              gen-about-page
                                              gen-rss]]
            [blog-clj.parse :refer [page-404]]))

(defroutes app-routes
  (GET "/" [] (gen-blog-page "/essays/"))
  (GET "/about/" [] (gen-about-page))
  (GET "/essays/:blog-id" [blog-id :as {uri :uri}] (gen-blog-page blog-id uri))
  (GET "/search/" [tag-type tag] (gen-blogtag-search-page tag tag-type))
  (GET "/rss/" [] (gen-rss))
  (route/resources "/")
  (route/not-found (page-404)))

(def app
  (wrap-defaults app-routes site-defaults))

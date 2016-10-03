(ns blog-clj.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [blog-clj.page-generators :refer [gen-blog-page
                                              gen-blogtag-search-page
                                              gen-about-page
                                              gen-rss]]
            [blog-clj.parse :refer [page-404]]
            [blog-clj.webhooks :refer [sync-hook]]))

(defroutes app-routes
  (GET "/" [] (gen-blog-page "/essays/"))
  (GET "/about/" [] (gen-about-page))
  (GET "/essays/:blog-id" [blog-id :as {uri :uri}] (gen-blog-page blog-id uri))
  (GET "/search/" [tag-type tag] (gen-blogtag-search-page tag tag-type))
  (GET "/rss/" [] (gen-rss))
  (POST "/github-webhooks/" req (sync-hook req))
  (route/resources "/")
  (route/not-found (page-404)))

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

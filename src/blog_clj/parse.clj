(ns blog-clj.parse
  (:require [hiccup.core :as hc]
            [clojure.string]
            [clojure.java.io :as jio]
            [net.cgrand.enlive-html :as el :refer [defsnippet deftemplate]]))

;; highlight the navbar
(defsnippet navbar-sp "templates/base.html"
  [:div.col-xs-3]
  [url-prefix]
  [[:a (el/attr-has :href url-prefix)]] (el/add-class "active"))

;; fill blog-post title 
(defsnippet blog-title "templates/base.html"
  [:h1.post-title]
  [blog-id blog-title]
  [:a] (el/do-> (el/content blog-title)
             (el/set-attr :href (str "/essays/" blog-id))))

;; generate blogroll sidebar
(defn blogroll-sp
  [blogrolls]
  (el/html [:div {:class "sidebar-container"}
         [:h1 "Blogroll"]
         (for [[url blog-name desc] blogrolls]
           [:p [:a {:href url} blog-name] [:span desc]])]))

;; generate archives sidebar
(defn archives-sp
  [archives]
  (el/html [:div {:class "sidebar-container"}
         [:h1 "Archives"]
         [:ul
          (for [[title id] archives]
            [:li [:a {:href (str "/essays/" id)} title]])]]))

;; generate tags sidebar
(defn tags-sp
  [tag-list tag-type]
  (el/html [:div {:class "sidebar-container"}
         [:h1 "Tags"]
         [:ul
          (for [[tag counts] tag-list]
            [:li [:a {:href (format "/search/?tag-type=%s&tag=%s" tag-type tag)}
                  (if (empty? tag) "NIL" tag)]
             [:span {:class "badge"} (str counts)]])]]))

;; generate toc sidebar
(defn toc-sp
  [toc]
  (el/html-snippet
   (hc/html [:div {:class "sidebar-container"}
             [:h1 "TOC"]
             (apply str (el/emit* toc))])))

(defn blog-tag-sp
  [tags]
  (clojure.string/join
   ","
   (for [tag tags]
     (hc/html [:a {:href (str "/search/?tag-type=blog&tag=" tag)} tag]))))

(defn disqus-sp
  [page-url page-id]
  (let [disqus-template (.getResource  (clojure.lang.RT/baseLoader) "templates/disqus.html")]
    (el/html-snippet (format (slurp disqus-template)
                          page-url
                          page-id))))

(deftemplate base-template "templates/base.html"
  [{:keys [nav-type blogroll archives all-blog-tags body title
           update-time create-time tags blog-id blog-url toc]}]
  [:title] (el/content title)
  [:div.blog-nav] (el/content (navbar-sp nav-type))
  [:h1.post-title] (el/substitute (blog-title blog-id title))
  [:span.update-time] (el/content update-time)
  [:span.create-time] (el/content create-time)
  [:span.blog-tags] (el/html-content (blog-tag-sp tags))
  [:div.post-content] (el/content body)
  [:div.post] (el/after (disqus-sp blog-url blog-id))
  ;; sidebars
  [:div.sidebar] (el/do->
                  (el/prepend (toc-sp toc))
                  (el/append (blogroll-sp blogroll))
                  (el/append (archives-sp archives))
                  (el/append (tags-sp all-blog-tags "blog"))))

; ============
(defn search-tag-sp
  [tag blog-infos]
  (el/html [:div {:class "content"}
         [:h1 {:class "text-center"} (str "Tag:" tag)]
         [:p {:class "post-title"}
          (for [[title id] blog-infos]
            [:p [:a {:href (str "/essays/" id)} title]])]]))

(deftemplate blogtag-search-template "templates/base.html"
  [tag blog-infos all-blog-tags]
  [:div.content] (el/substitute (search-tag-sp tag blog-infos))
  [:div.sidebar] (el/append (tags-sp all-blog-tags "blog")))

; ============
(defn about-sp
  []
  (let [about-html (.getResource (clojure.lang.RT/baseLoader) "templates/about.html")]
    (el/html-snippet (slurp about-html))))

(deftemplate about-template "templates/base.html"
  []
  [:div.blog-nav] (el/content (navbar-sp "/about/"))
  [:div.content] (el/substitute (about-sp)))

(deftemplate page-404  "templates/base.html"
  []
  [:div.content] (el/content "抱歉，最近网站做了一些调整，删除了部分内容，如果你对将要访问的内容非常感兴趣，请直接联系我~"))

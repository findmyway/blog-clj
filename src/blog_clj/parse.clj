(ns blog-clj.parse
  (:use net.cgrand.enlive-html)
  (:require [hiccup.core :as hc]
            [clojure.string]
            [clojure.java.io :as jio]))

;; highlight the navbar
(defsnippet navbar-sp "templates/base.html"
  [:div.col-xs-3]
  [url-prefix]
  [[:a (attr-has :href url-prefix)]] (add-class "active"))

;; fill blog-post title 
(defsnippet blog-title "templates/base.html"
  [:h1.post-title]
  [blog-id blog-title]
  [:a] (do-> (content blog-title)
             (set-attr :href (str "/essays/" blog-id))))

;; generate blogroll sidebar
(defn blogroll-sp
  [blogrolls]
  (html [:div {:class "sidebar-container"}
         [:h1 "Blogroll"]
         (for [[url blog-name desc] blogrolls]
           [:p [:a {:href url} blog-name] [:span desc]])]))

;; generate archives sidebar
(defn archives-sp
  [archives]
  (html [:div {:class "sidebar-container"}
         [:h1 "Archives"]
         [:ul
          (for [[title id] archives]
            [:li [:a {:href (str "/essays/" id)} title]])]]))

;; generate tags sidebar
(defn tags-sp
  [tag-list tag-type]
  (html [:div {:class "sidebar-container"}
         [:h1 "Tags"]
         [:ul
          (for [[tag counts] tag-list]
            [:li [:a {:href (format "/search/?tag-type=%s&tag=%s" tag-type tag)}
                  (if (empty? tag) "NIL" tag)]
             [:span {:class "badge"} (str counts)]])]]))

;; generate toc sidebar
(defn toc-sp
  [toc]
  (html-snippet
   (hc/html [:div {:class "sidebar-container"}
             [:h1 "TOC"]
             (apply str (emit* toc))])))

(defn blog-tag-sp
  [tags]
  (clojure.string/join
   ","
   (for [tag tags]
     (hc/html [:a {:href (str "/search/?tag-type=blog&tag=" tag)} tag]))))

(defn disqus-sp
  [page-url page-id]
  (let [disqus-template (jio/file (jio/resource "templates/disqus.html"))]
    (html-snippet (format (slurp disqus-template)
                          page-url
                          page-id))))

(deftemplate base-template "templates/base.html"
  [{:keys [nav-type blogroll archives all-blog-tags body title
           update-time create-time tags blog-id blog-url toc]}]
  [:title] (content title)
  [:div.blog-nav] (content (navbar-sp nav-type))
  [:h1.post-title] (substitute (blog-title blog-id title))
  [:span.update-time] (content update-time)
  [:span.create-time] (content create-time)
  [:span.blog-tags] (html-content (blog-tag-sp tags))
  [:div.post-content] (content body)
  [:div.post] (after (disqus-sp blog-url blog-id))
  ;; sidebars
  [:div.sidebar] (do->
                  (prepend (toc-sp toc))
                  (append (blogroll-sp blogroll))
                  (append (archives-sp archives))
                  (append (tags-sp all-blog-tags "blog"))))

; ============
(defn search-tag-sp
  [tag blog-infos]
  (html [:div {:class "content"}
         [:h1 {:class "text-center"} (str "Tag:" tag)]
         [:p {:class "post-title"}
          (for [[title id] blog-infos]
            [:p [:a {:href (str "/essays/" id)} title]])]]))

(deftemplate blogtag-search-template "templates/base.html"
  [tag blog-infos all-blog-tags]
  [:div.content] (substitute (search-tag-sp tag blog-infos))
  [:div.sidebar] (append (tags-sp all-blog-tags "blog")))

; ============
(defn about-sp
  []
  (let [about-html (jio/file (jio/resource "templates/about.html"))]
    (html-snippet (slurp about-html))))

(deftemplate about-template "templates/base.html"
  []
  [:div.blog-nav] (content (navbar-sp "/about/"))
  [:div.content] (substitute (about-sp)))

(deftemplate page-404  "templates/base.html"
  []
  [:div.content] (content "抱歉，最近网站做了一些调整，删除了部分内容，如果你对将要访问的内容非常感兴趣，请直接联系我~"))

(ns blog-clj.parse
  (:use net.cgrand.enlive-html)
  (:require [hiccup.core :as hc]))

(defsnippet highlight-navbar "templates/base-test.html"
  [:div.col-xs-3]
  [url-prefix]
  [[:a (attr-has :href url-prefix)]] (add-class "active"))

(defsnippet blog-roll "templates/base-test.html"
  [:div#blogroll]
  [blogrolls]
  [:p] (clone-for [[url blog-name desc] blogrolls]
                  [:a] (content blog-name)
                  [:a] (set-attr :href url)))

(defsnippet archive "templates/base-test.html"
  [:div#archive]
  [archives]
  [:ul :li] (clone-for [[id title] archives]
                       [:a] (content title)
                       [:a] (set-attr :href id)))

(defsnippet tags "templates/base-test.html"
  [:div#tag-list]
  [tag-list]
  [:ul :li] (clone-for [[tag-url tag] tag-list]
                       [:a] (content tag)
                       [:a] (set-attr :href tag-url)))

(defsnippet sp-test "templates/header.html"
  [:header]
  []
  [:ul :li] (content (html [:script "alter('hello world')"])))

(deftemplate base-test "templates/base-test.html"
  []
  ;; highlight blog-nav bar
  [:div.blog-nav] (content (highlight-navbar "/home/"))
  [:div#blogroll] (substitute (blog-roll [["http://www.example.com" "example" "xyz"]
                                          ["http://www.example.com" "example" "xyz"]
                                          ["http://www.example.com" "example" "xyz"]]))
  [:div#archive] (substitute (archive [["a" "a"]
                                       ["b" "b"]
                                       ["c" "c"]
                                       ["d" "d"]]))
  [:div#tag-list] (substitute (tags [["123" "tag1"]
                                     ["123" "tag1"]
                                     ["123" "tag1"]])))

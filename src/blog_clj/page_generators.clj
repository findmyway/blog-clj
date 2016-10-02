(ns blog-clj.page-generators
  (:require [blog-clj.parse :refer [base-template blogtag-search-template about-template page-404]]
            [blog-clj.redis-io :refer [get-all-blogs-titles get-blog get-blogids-with-tag]]
            [clojure.set]
            [hickory.render :refer [hickory-to-html]]
            [clj-rss.core :as rss]))

(def blogroll
  ;; [[url name description]
  [["http://laike9m.com/" "laike9m" "左兄写的文档是我等学习的典范~"]
   ["http://hujiaweibujidao.github.io/" "Hujiawei" "大牛不解释，望其项背，哈哈~"]
   ["http://oilbeater.com/" "Oilbeater" "北京大学操作系统实验室滴葫芦娃~"]
   ["http://jacoxu.com/" "jacoxu" "实验室里的师兄哦，更新滴灰常勤快~"]
   ["http://qimingyu.com/" "Qi Mingyu" "我高中同学，在做安全相关的工作~"]])

(defn- tag-counts
  [tag-sets]
  (sort-by last > (apply merge-with + (map #(zipmap % (repeat 1)) tag-sets))))

(defn gen-blog-page
  "generate the blog content page"
  ([blog-id blog-url]
   (let [blog-detail (get-blog blog-id)
         nav-type "/"
         archives  (map #(take 2 %) (get-all-blogs-titles))
         all-blog-ids (map second archives)
         all-blog-tags (tag-counts (map #(:tags (get-blog % :tags)) all-blog-ids))]
     (if (some #{blog-id} all-blog-ids)
       (base-template (merge blog-detail
                             {:nav-type nav-type
                              :archives archives
                              :all-blog-tags all-blog-tags
                              :blogroll blogroll
                              :blog-url blog-url}))
       (page-404))))
  ([base-url]
   (let [blog-id (-> (get-all-blogs-titles)
                     first
                     second)]
     (gen-blog-page blog-id (str base-url blog-id)))))

(defn gen-blogtag-search-page
  [tag tag-type]
  (let [blog-ids (get-blogids-with-tag tag)
        blog-titles (map #(:title (get-blog % :title)) blog-ids)
        archives  (map #(take 2 %) (get-all-blogs-titles))
        all-blog-ids (map second archives)
        all-blog-tags (tag-counts (map #(:tags (get-blog % :tags)) all-blog-ids))]
    (blogtag-search-template
     tag
     (map vector blog-titles blog-ids)
     all-blog-tags)))

(defn gen-about-page
  []
  (about-template))

(defn gen-rss
  []
  (rss/channel-xml
   {:title "TianJun MachineLearning"
    :link "http://tianjun.me"
    :description "在这里，记录些自己的学习、思考与感悟。"}
   (for [[title id _] (take 5 (get-all-blogs-titles))]
     (let [{:keys [title blog-id body]} (get-blog id :title :blog-id :body)]
       {:title title
        :description (hickory-to-html body)
        :link (str "http://tianjun.me" "/essays/" blog-id)}))))

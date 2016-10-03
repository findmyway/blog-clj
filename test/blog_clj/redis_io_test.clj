(ns blog-clj.redis-io-test
  (:require [taoensso.carmine :as car :refer  (wcar)]
            [clj-time.coerce :as c])
  (:use midje.sweet)
  (:use blog-clj.redis-io))

(def test-site "test.tianjun.me:")
(defn clean-test-keys! [] (when-let [ks (seq (wcar* (car/keys (str test-site "*"))))]
                            (wcar* (apply car/del ks))))

(facts "about redis read and write"
       (against-background [(before :contents (clean-test-keys!))
                            (after :contents (clean-test-keys!))
                            (around :facts (binding [site test-site] ?form))]
                           (fact "check create new blog process"
                                 (new-blog {:title "[1]This is a test title!"
                                            :tags #{"tag1" "tag2" "tag3"}
                                            :body "[1]blog body: this is the blog body"
                                            :update-time  "2016-08-25 07:07:07"})
                                 => truthy

                                 (new-blog {:title "[2]This is a test title!"
                                            :tags #{"tag1" "tag4"}
                                            :body "[2]blog body: this is the blog body"
                                            :update-time  "2016-08-25 08:08:08"})
                                 => truthy

                                 (new-blog {:title "[3]This is a test title!"
                                            :tags #{"tag3" "tag4" "tag5"}
                                            :body "[3]blog body: this is the blog body"
                                            :update-time  "2016-08-25 09:09:09"})
                                 => truthy)
                           (fact "check the newly created blog"
                                 (get-blog 1) => (contains [[:title "[1]This is a test title!"]
                                                            [:tags #{"tag1" "tag2" "tag3"}]
                                                            [:body "[1]blog body: this is the blog body"]
                                                            [:update-time "2016-08-25 07:07:07"]
                                                            [:create-time "2016-08-25 07:07:07"]])
                                 (get-blog 2) => (contains [[:title "[2]This is a test title!"]
                                                            [:tags #{"tag1" "tag4"}]
                                                            [:body "[2]blog body: this is the blog body"]
                                                            [:update-time "2016-08-25 08:08:08"]
                                                            [:create-time "2016-08-25 08:08:08"]])
                                 (get-blog 3 :title :update-time) => (contains [[:title "[3]This is a test title!"]
                                                                                [:update-time "2016-08-25 09:09:09"]]))
                           (fact "check all blog titles"
                                 (get-all-blogs-titles) => (just (just ["[3]This is a test title!"
                                                                        "3"
                                                                        (str (c/to-long "2016-08-25 09:09:09"))])
                                                                 (just ["[2]This is a test title!" "2"
                                                                        (str (c/to-long "2016-08-25 08:08:08"))])
                                                                 (just ["[1]This is a test title!"
                                                                        "1"
                                                                        (str (c/to-long "2016-08-25 07:07:07"))])))
                           (fact "check tags "
                                 (get-blogids-with-tag "tag1") => (just #{"1" "2"})
                                 (get-blogids-with-tag "tag2") => (just #{"1"})
                                 (get-blogids-with-tag "tag3") => (just #{"1" "3"})
                                 (get-blogids-with-tag "tag4") => (just #{"2" "3"})
                                 (get-blogids-with-tag "tag5") => (just #{"3"}))
                           (fact "check updates"
                                 (update-blog {:body "Body 1 changed!"
                                               :blog-id "1"
                                               :update-time "2016-08-25 11:11:11"}) => truthy
                                 (get-blog 1) => (contains [[:body "Body 1 changed!"]
                                                            [:update-time "2016-08-25 11:11:11"]])
                                 (update-blog {:body "Body 2 changed!"
                                               :blog-id "2"
                                               :update-time "2016-08-25 12:12:12"}) => truthy
                                 (get-blog 2) => (contains [[:body  "Body 2 changed!"]
                                                            [:update-time "2016-08-25 12:12:12"]])
                                 (update-blog {:tags #{"tag1" "tag2" "tag4"}
                                               :blog-id "3"
                                               :update-time "2016-08-25 13:13:13"}) => truthy

                                 (get-blog 3) => (contains [[:tags #{"tag1" "tag2" "tag4"}]])
                                 (get-blogids-with-tag "tag1") => (just #{"1" "2" "3"})
                                 (get-blogids-with-tag "tag2") => (just #{"1" "3"})
                                 (get-blogids-with-tag "tag3") => (just #{"1"})
                                 (get-blogids-with-tag "tag4") => (just #{"2" "3"})
                                 (get-blogids-with-tag "tag5") => (just [])
                                 (get-all-blogs-titles) => (just (just ["[3]This is a test title!"
                                                                        "3"
                                                                        (str (c/to-long "2016-08-25 13:13:13"))])
                                                                 (just ["[2]This is a test title!"
                                                                        "2"
                                                                        (str (c/to-long "2016-08-25 12:12:12"))])
                                                                 (just ["[1]This is a test title!"
                                                                        "1"
                                                                        (str (c/to-long "2016-08-25 11:11:11"))])))
                           (fact "check deletes"
                                 (delete-blog 1) => truthy
                                 (get-blog 1) => (just {})
                                 (get-blogids-with-tag "tag1") =not=> (contains "1")
                                 (get-blogids-with-tag "tag2") =not=> (contains "1")
                                 (get-blogids-with-tag "tag3") =not=> (contains "1"))))

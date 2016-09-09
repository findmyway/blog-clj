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
                                            :body "[1]blog body: this is the blog body"})
                                 => truthy
                                 (provided (now) => "2016-08-25 07:07:07")

                                 (new-blog {:title "[2]This is a test title!"
                                            :tags #{"tag1" "tag4"}
                                            :body "[2]blog body: this is the blog body"})
                                 => truthy
                                 (provided (now) => "2016-08-25 08:08:08")

                                 (new-blog {:title "[3]This is a test title!"
                                            :tags #{"tag3" "tag4" "tag5"}
                                            :body "[3]blog body: this is the blog body"})
                                 => truthy
                                 (provided (now) => "2016-08-25 09:09:09"))
                           (fact "check the newly created blog, here we ignore the create & update time"
                                 (get-blog 1) => (contains [["title" "[1]This is a test title!"]
                                                            ["tags" #{"tag1" "tag2" "tag3"}]
                                                            ["body" "[1]blog body: this is the blog body"]])
                                 (get-blog 2) => (contains [["title" "[2]This is a test title!"]
                                                            ["tags" #{"tag1" "tag4"}]
                                                            ["body" "[2]blog body: this is the blog body"]]))
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
                                 (update-blog 1 {:title "[1] Blog title changed!"}) => truthy
                                 (provided (now) =>  "2016-08-25 17:17:17")
                                 (get-blog 1) => (contains [["title" "[1] Blog title changed!"]])
                                 (update-blog 2 {:body "[2] Blog body changed!"}) => truthy
                                 (provided (now) =>  "2016-08-25 18:18:18")
                                 (get-blog 2) => (contains [["body"  "[2] Blog body changed!"]])
                                 (update-blog 3 {:tags #{"tag1" "tag2" "tag4"}}) => truthy
                                 (provided (now) =>  "2016-08-25 19:19:19")
                                 (get-blog 3) => (contains [["tags" #{"tag1" "tag2" "tag4"}]])
                                 (get-blogids-with-tag "tag1") => (just #{"1" "2" "3"})
                                 (get-blogids-with-tag "tag2") => (just #{"1" "3"})
                                 (get-blogids-with-tag "tag3") => (just #{"1"})
                                 (get-blogids-with-tag "tag4") => (just #{"2" "3"})
                                 (get-blogids-with-tag "tag5") => (just [])
                                 (get-all-blogs-titles) => (just (just ["[3]This is a test title!"
                                                                        "3"
                                                                        (str (c/to-long "2016-08-25 19:19:19"))])
                                                                 (just ["[2]This is a test title!"
                                                                        "2"
                                                                        (str (c/to-long "2016-08-25 18:18:18"))])
                                                                 (just ["[1] Blog title changed!"
                                                                        "1"
                                                                        (str (c/to-long "2016-08-25 17:17:17"))])))
                           (fact "check deletes"
                                 (delete-blog 1) => truthy
                                 (get-blog 1) => (just {})
                                 (get-blogids-with-tag "tag1") =not=> (contains "1")
                                 (get-blogids-with-tag "tag2") =not=> (contains "1")
                                 (get-blogids-with-tag "tag3") =not=> (contains "1"))))

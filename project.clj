(defproject blog-clj "0.1.0-SNAPSHOT"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :author "Tian Jun"
            :email "tianjun@tianjun.me"
            :year 2016
            :key "mit"}
  :description "The source code of my blog"
  :url "http://tianjun.me"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [com.taoensso/carmine  "2.14.0"]
                 [enlive  "1.1.6"]
                 [clj.qiniu "0.1.2"]
                 [jarohen/chime "0.1.9"]
                 [clj-time "0.12.0"]
                 [hickory "0.6.0"]
                 [clj-rss  "0.2.3"]
                 [com.taoensso/timbre  "4.7.4"]
                 [ring/ring-json  "0.4.0"]
                 [pandect  "0.6.0"]
                 [org.clojure/data.json  "0.2.6"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ  "1.1.0"]]
  :ring {:handler blog-clj.handler/app
         :init blog-clj.schedules/start
         :destroy blog-clj.schedules/stop}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [midje "1.6.3"]]}})

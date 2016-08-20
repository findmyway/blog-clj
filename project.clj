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
                 [com.taoensso/carmine  "2.14.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler blog-clj.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})

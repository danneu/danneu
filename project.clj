(defproject danneu "0.1.0"
  :description "Dan Neumann's blog"
  :url "http://danneu.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.3"]
                 [ring "1.2.0"]
                 [markdown-clj "0.9.29"]
                 [compojure "1.1.5"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler danneu.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :main danneu.handler)

(ns danneu.handler
  (:use [compojure.core]
        [hiccup core page util element]
        [ring.util response]
        [ring.middleware file-info]
        [danneu.middleware ensure-trailing-slash])
  (:require [danneu.post :as post]
            [danneu.markdown :as markdown]
            [clojure.string :as str]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.route :as route])
  (:gen-class))



(defn layout
  ([page] (layout page nil))
  ([page title]
     (html5
      [:head
       [:title (if title
                 (str title " - danneu's blog")
                 "danneu's blog")]
       [:style ".syntaxhighlighter { padding-top: 10px;
                                  padding-bottom: 10px; }"]
       (include-css "/syntax-highlighter/shCoreRDark.css"
                    "/syntax-highlighter/shThemeRDark.css")
       (include-css "/css/base.css"
                    "/css/layout.css"
                    "/css/skeleton.css"
                    "/css/style.css"
                    "/css/pygments.css")]
      [:body
       [:div.container


        [:div.header
         (link-to {:class "logo"} "/" (list [:div.back "&larr;"]
                                            [:h1 "danneu"]))
         " &mdash; "
         [:ul [:li (link-to "/about-me/" "about me")]
          ;" &mdash; "
          ;[:li (link-to "/projects" "projects")]
          ]]
        
        page


        [:script
         "var _gaq = _gaq || [];
        _gaq.push(['_setAccount', 'UA-25561085-1']);
        _gaq.push(['_trackPageview']);
        (function() {
          var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
          ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
          var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
        })();"]

        ]

       (include-js "/syntax-highlighter/shCore.js"
                   "/syntax-highlighter/brushes/shBrushBash.js"
                   "/syntax-highlighter/brushes/shBrushCss.js"
                   "/syntax-highlighter/brushes/shBrushJava.js"
                   "/syntax-highlighter/brushes/shBrushJScript.js"
                   "/syntax-highlighter/brushes/shBrushPlain.js"
                   "/syntax-highlighter/brushes/shBrushPython.js"
                   "/syntax-highlighter/brushes/shBrushRuby.js"
                   "/syntax-highlighter/brushes/shBrushSql.js"
                   "/syntax-highlighter/brushes/shBrushXml.js")
       [:script "SyntaxHighlighter.defaults['gutter'] = true;
              SyntaxHighlighter.defaults['toolbar'] = false;
              SyntaxHighlighter.all();"]])))

;; Views

(defn show-all-posts [posts]
  (html
   [:div.posts
    (for [p posts]
      [:div.row
       [:div.thirteen.columns.alpha
        (link-to (str "/posts/" (:permalink p) "/") (:title p))]
       [:div.three.columns.omega (:published-at p)]])]))

(defn show-post [post]
  (html
   [:h1 (:title post)]

   [:div.post-meta.row
    [:div.eight.columns.alpha.post-author
     "by " (link-to "/about-me" "Dan")]
    [:div.eight.columns.omega.post-date
     (:published-at post)]]

   
   [:div.post-content (post/html post)]

   [:hr]
   [:h1 "Comments"]
   [:div#disqus_thread]
   [:script
    (str
     "var disqus_shortname = 'danneu',
      disqus_identifier = '" (post/disqus-id post)  "',
      disqus_title = '" (post/title post) "',
      disqus_url = 'http://danneu.com/posts/"
      (post/permalink post)
      "',
      disqus_developer = 0;

  (function() {
   var dsq = document.createElement('script');
   dsq.type = 'text/javascript',
   dsq.async = true,
   dsq.src = 'http://' + disqus_shortname + '.disqus.com/embed.js';

   (document.getElementsByTagName('head')[0] || 
    document.getElementsByTagName('body')[0]).appendChild(dsq);
  })();")]))

(defn about-me []
  (html

   [:h1 "About Me"]

   [:div.row
    [:div.ten.columns.alpha
   (markdown/to-html "
I decided to finally claim my own personal nook of the internet. This blog is an effort to give back to the communities in which I've participated, at the very least.

- I'm Dan ([Email](mailto:danrodneu@gmail.com), [Github](https://github.com/danneu))
- I live in Austin, Texas
- Graduated from [Univ of Texas](http://www.utexas.edu/). I also studied at [Univ of Economics, Prague](http://en.wikipedia.org/wiki/University_of_Economics,_Prague).
- I enjoy Rails, Vim, Autodidacticism, Seneca, and Ricky Gervais' laugh.")

     ]
    [:div.six.columns.omega
     [:img {:src "http://gravatar.com/avatar/c42635853936e509f76ece9d8187c4aa?size=200"}]
     ]]
   
   ))


;; Routes

(defroutes app-routes
  (GET "/" [] (layout (show-all-posts (post/all-posts))))
  (GET "/about-me/" [] (layout (about-me) "About Me"))
  (GET "/posts/:permalink/" [permalink]
    (let [p (post/find-by-permalink permalink)]
      (layout (show-post p) (:title p))))
  (-> (GET "/posts/:permalink/*" {{:keys [permalink *]} :params}
        (let [p (post/find-by-permalink permalink)
              path (str "resources/posts/" (:name p) "/" *)]
          (file-response path)))
      (wrap-file-info))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (ensure-trailing-slash (handler/site app-routes)))

;; Server

(defn start-server [port]
  (run-jetty app {:port port}))

(defn -main [& args]
  (let [port (Integer. (or (first args) "5004"))]
    (start-server port)))

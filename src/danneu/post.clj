(ns danneu.post
  (:require [danneu.markdown :as markdown]
            [clojure.string :as str])
  (:import [java.io File]))

;; Private

(defn dirs
  "Returns coll of Files that represent each directory
   in resources/posts."
  []
  (filter #(.isDirectory %) (.listFiles (File. "resources/posts"))))

(defn parse-dir [dir]
  (let [path (.getAbsolutePath dir)
        meta (read-string (slurp (str path "/content.md")))
        created-at (re-find #"\d+-\d+-\d+" (.getName dir))
        published-at created-at]
    (merge {:created-at created-at
            :published-at published-at
            :name (.getName dir)
            :path path}
           meta)))

;; Public

(defn all-posts []
  (reverse (sort-by :published-at (map parse-dir (dirs)))))

(defn text [post]
  (let [contents (slurp (str (:path post) "/content.md"))]
    (str/trim (str/replace-first contents #"\{[^\}]+\}" ""))))

(defn html [post]
  (markdown/to-html (text post)))

(defn disqus-id [post]
  (or (:disqus-id post) (:permalink post)))

(defn title [post]
  (:title post))

(defn permalink [post]
  (:permalink post))

(defn find-by-permalink [permalink]
  (first (filter #(= permalink (:permalink %)) (all-posts))))

;; (defn underscorize [s] (str/replace s "-" "_"))
;; (defn hyphenize [s] (str/replace s "_" "-"))
;; (defn string-to-date
;;   "String should be of form year-month-day. Ex: 2012-12-30."
;;   [s]
;;   (let [formatter (SimpleDateFormat. "yyyy-MM-dd")]
;;     (.parse formatter s)))

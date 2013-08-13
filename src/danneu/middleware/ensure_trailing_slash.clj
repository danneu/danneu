(ns danneu.middleware.ensure-trailing-slash
  (:require [clojure.string :as str]
            [ring.util.response :as response]))

;; Private

(defn- resource-route? [url]
  (> (count (str/split url #"\.")) 1))

(defn- trailing-slash? [url]
  (= (last url) \/))

(defn- add-trailing-slash [url]
  (str url "/"))

;; Public

(defn ensure-trailing-slash
  "Redirects non-trailing-slash URIs to an URI with trailing slash.
   Does not redirect resource URIs like /cat.jpg"
  [handler]
  (fn [request]
    (let [uri (:uri request)]
      (if (or (resource-route? uri) (trailing-slash? uri))
        (handler request)
        (response/redirect (add-trailing-slash uri))))))

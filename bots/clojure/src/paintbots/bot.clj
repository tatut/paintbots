(ns paintbots.bot
  (:require [org.httpkit.client :as http]
            [clojure.string :as str])
  (:import (java.net URLEncoder URLDecoder)))

(def url (or (System/getenv "PAINTBOTS_URL")
             "http://localhost:31173"))

(def integer-fields #{:x :y})
;; Bot interface to server

(def ^:dynamic *retry?* false)

(defn- post [& {:as args}]
  (let [{:keys [status body headers] :as _resp} @(http/post url {:form-params args :as :text})]
    (cond
      (and (= status 409) *retry?*)
      (do (Thread/sleep 1000)
          (binding [*retry?* false]
            (post args)))

      (>= status 400)
      (throw (ex-info "Unexpected status code" {:status status :body body}))

      (= (:content-type headers) "application/x-www-form-urlencoded")
      (into {}
            (for [field (str/split body #"&")
                  :let [[k v] (str/split field #"=")
                        kw (keyword (URLDecoder/decode k))
                        v (URLDecoder/decode v)]]
              [kw (if (integer-fields kw)
                    (Integer/parseInt v)
                    v)]))

      :else
      body)))

(defn register [name]
  {:name name :id (post :register name)})

(defn move [bot dir]
  (merge
   bot
   (post :id (:id bot) :move dir)))

(defn paint [bot]
  (merge bot (post :id (:id bot) :paint "1")))

(defn color [bot c]
  (merge bot (post :id (:id bot) :color c)))

(defn- rotate [dir]
  (case dir
    "LEFT" "DOWN"
    "DOWN" "RIGHT"
    "RIGHT" "UP"
    "UP" "LEFT"))

(defn dragon [c]
  ;; concatenate rotated c to c
  ;; add marker between iterations to change color
  (into (conj c :change-col)
        (map rotate)
        (remove #(= % :change-col) (reverse c))))

(def ^:const palette "23489abcef")

(defn draw [bot n c]
  (let [curve (nth (iterate dragon ["LEFT"]) n)]
    (loop [bot bot
           [dir & curve] curve

           ;; start with a random color in palette
           colors (drop (rand-int 10) (cycle palette))]
      (cond
        (nil? dir) :done

        (= :change-col dir)
        (recur (color bot (str (first colors)))
               curve
               (rest colors))
        :else
        (recur (reduce (fn [bot _]
                         (-> bot paint (move dir)))
                       bot (range c))
               curve colors)))))

(defn move-to [bot to-x to-y]
  (let [dx (- (:x bot) to-x)
        dy (- (:y bot) to-y)

        ;; move randomly either x or y (to look cool ;)
        r (rand-int 2)]
    (cond
      (and (zero? dx) (zero? dy))
      bot

      (and (= 0 r) (not= 0 dx))
      (recur (move bot (if (neg? dx) "RIGHT" "LEFT")) to-x to-y)

      (and (= 1 r) (not= 0 dy))
      (recur (move bot (if (neg? dy) "DOWN" "UP")) to-x to-y)

      :else
      (recur bot to-x to-y))))

(defn -main [& args]
  (-> (register (str "dragon" (System/currentTimeMillis)))
      (move "LEFT")
      (move-to (+ 75 (rand-int 10)) (+ 45 (rand-int 10)))
      (draw 9 4)))

(defn stress-test
  "Run n bots to stress test the server.
  Returns 0 arity function to stop the stress test."
  [n-bots]
  (let [done? (atom false)]
    (dotimes [i n-bots]
      (.start (Thread. #(binding [*retry?* true]
                          (loop [bot (register (str "stress" i))]
                            (dotimes [i 10]
                              (move bot "LEFT")
                              (paint bot))
                            (dotimes [i 10]
                              (move bot "RIGHT")
                              (paint bot))
                            (when-not @done?
                              (recur (color bot (rand-nth "0123456789abcdef")))))))))
    #(reset! done? true)))

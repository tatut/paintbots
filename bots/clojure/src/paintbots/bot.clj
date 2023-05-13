(ns paintbots.bot
  (:require [org.httpkit.client :as http]
            [clojure.string :as str]
            [bortexz.resocket :as ws]
            [clojure.core.async :refer [<!! >!!] :as async])
  (:import (java.net URLEncoder URLDecoder)))

(def url (or (System/getenv "PAINTBOTS_URL")
             "ws://localhost:31173"))

(def integer-fields #{:x :y})
;; Bot interface to server

(def ^:dynamic *retry?* false)

(defn- form->params [body]
  (into {}
        (for [field (str/split body #"&")
              :let [[k v] (str/split field #"=")
                    kw (keyword (URLDecoder/decode k))
                    v (when v (URLDecoder/decode v))]]
          [kw (if (integer-fields kw)
                (Integer/parseInt v)
                v)])))

(defn- params->form [p]
  (str/join "&"
            (for [[k v] p]
              (str (URLEncoder/encode (name k)) "=" (URLEncoder/encode (str v))))))

(defn- post [args]
  (let [{:keys [status body headers] :as _resp} @(http/post url {:form-params args :as :text})]
    (cond
      (and (= status 409) *retry?*)
      (do (Thread/sleep 1000)
          (binding [*retry?* false]
            (post args)))
      (>= status 400)
      (throw (ex-info "Unexpected status code" {:status status :body body}))

      (= (:content-type headers) "application/x-www-form-urlencoded")
      (form->params body)

      (contains? args :register)
      {:id body}

      :else body)))



(defn send! [{id :id :as bot} msg]
  (if-let [ws (::ws bot)]
    ;; Use WS connection
    (do
      (def *ws ws)
      ;; id is implicit in the WS connection and not needed
      (->> (dissoc msg :id) params->form (>!! (:output ws)))
      (->> ws :input <!! form->params (merge bot)))

    ;; Use HTTP
    (merge bot (post (merge msg
                            (when id
                              {:id id}))))))

(defn register [name]
  (send! (merge {:name name}
                (when (str/starts-with? url "ws")
                  ;; if url is ws:// or wss:// use websocket connection
                  {::ws (<!! (ws/connection url))}))

         {:register name}))

(defn move [bot dir]
  (send! bot {:move dir}))

(defn paint [bot]
  (send! bot {:paint "1"}))

(defn color [bot c]
  (send! bot {:color c}))

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
                            (when-not @done?
                              (-> bot
                                  (move (rand-nth ["LEFT" "RIGHT" "UP" "DOWN"]))
                                  paint
                                  (color (rand-nth "0123456789abcdef"))
                                  recur)))))))
    #(reset! done? true)))

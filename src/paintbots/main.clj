(ns paintbots.main
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :as params]
            [clojure.core.async :refer [go <! timeout]]
            [clojure.string :as str]
            [ripley.html :as h]
            [ripley.live.context :as context]
            [clojure.java.io :as io]
            [ripley.live.source :as source]
            [ripley.live.poll :as poll]
            [paintbots.png :as png]))

(defonce server nil)

;; State contains currently registered bots (map of bot id to info)
;; and canvas (map of [x,y] => color)
(defonce state (agent {:bots {}
                       :canvas {}}))

(def colors [;; red, green, blue
             [255 0 0] [0 255 0] [0 0 255]
             ;; yellow
             [255 255 0]
             ;; pink
             [255 0 255]
             ;; cyan
             [0 255 255]])

(defn register [{{name :register} :form-params :as _req}]
  (let [name (str/trim name)]
    (if (some (fn [[_ {bot-name :name}]]
                (= bot-name name)) (:bots @state))
      {:status 409
       :body "Already registered!"}
      (let [id (str (random-uuid))]
        (send state
              (fn [{:keys [width height] :as state}]
                (assoc-in state [:bots id]
                          {:name name
                           :x (rand-int width)
                           :y (rand-int height)
                           :color (rand-nth colors)
                           :registered-at (java.util.Date.)})))
        {:status 200
         :body id}))))

(defn bot-command [{{:keys [id] :as params} :form-params :as req} command-fn]
  (cond
    (not (contains? (:bots @state) id))
    {:status 409
     :body (str "Bot with id " id " is not registered!")}

    (get-in @state [:bots id :in-command?])
    {:status 409
     :body "Already issuing a command in another request!"}

    :else
    (do
      (send state assoc-in [:bots id :in-command?] true)
      (httpkit/as-channel
       req
       {:on-open (fn [ch]
                   (go
                     (<! (timeout (:command-duration-ms @state)))
                     (send state (fn [state]
                                   (-> state
                                       (command-fn params)
                                       (assoc-in [:bots id :in-command?] false))))
                     (let [{:keys [x y]} (get-in @state [:bots id])]
                       (httpkit/send! ch {:status 200
                                          :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                          :body (str "x=" x "&y=" y)}
                                      true))))}))))
(defn move [req]
  (bot-command
   req
   (fn [{:keys [width height bots] :as state} {:keys [id move]}]
     (let [{:keys [x y]} (bots id)
           [new-x new-y]
           (case move
             "UP" [x (dec y)]
             "DOWN" [x (inc y)]
             "LEFT" [(dec x) y]
             "RIGHT" [(inc x) y])]
       (update-in state [:bots id] assoc
                  :x new-x
                  :y new-y)))))

(defn paint [req]
  (bot-command
   req
   (fn [state {:keys [id]}]
     (let [{:keys [x y color]} (get-in state [:bots id])
           [r g b] color
           avg (fn [a b]
                 (int (/ (+ (or a 0) (or b 0)) 2)))]
       (update-in state [:canvas [x y]]
                  (fn [[r1 g1 b1]]
                    [(avg r r1) (avg g g1) (avg b b1)]))))))

(defn app-bar []
  (h/html
   [:nav.navbar.bg-base-100
    [:div.flex-1
     [:span.font-semibold "PaintBots"]]

    [:div.navbar-start
     [:ul.menu.menu-compact.lg:menu-horizontal.md:menu-horizontal
      ;; links here?
      ]]

    [:div.navbar-end
     [:button.btn.btn-sm.mx-2 {:on-click #(send state assoc :canvas {})} "admin: clear"]
     ]]))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn page [{:keys [width height] :as _config}]
  (let [state-source (poll/poll-source 1000 #(deref state))
        canvas (source/computed :canvas state-source)
        bots (source/computed :bots state-source)]
    (h/out! "<!DOCTYPE html>\n")
    (h/html
     [:html {:data-theme "light"}
      [:head
       [:meta {:charset "UTF-8"}]
       [:link {:rel "stylesheet" :href "/paintbots.css"}]
       (h/live-client-script "/ws")]
      [:body
       (app-bar)
       [:div.page
        [::h/live bots
         (fn [bots]
           (h/html
            [:div.bots
             "Connected bots: "
             [::h/for [{n :name c :color} (vals bots)
                       :let [col-style (str "width: 16px; height: 16px; "
                                            "position: absolute; left: 2px; top: 2px;"
                                            "background-color: " (rgb c) ";")]]
              [:div.relative.pl-5 n [:div.inline {:style col-style}]]]]))]

        [:div.border
         [:svg {:width "100%" :viewBox (str "0 0 " width " " height)}
          [::h/live canvas
           (fn [art]
             (h/html
              [:g.art
               [::h/for [[[x y] [r g b]] art
                         :let [fill (str "rgb(" r "," g "," b ")")]]
                [:rect {:x x :y y :width 1 :height 1 :fill fill}]]]))]]]]]])))

(defn handle-post [req]
  (let [{p :form-params :as req}
        (update req :form-params
                #(into {}
                      (map (fn [[k v]]
                             [(keyword k) v]))
                      %))]
    (cond
      (contains? p :register)
      (register req)

      (contains? p :move)
      (move req)

      (contains? p :paint)
      (paint req)

      :else
      {:status 404
       :body "I don't recognize those parameters, try something else."})))

(let [ws-handler (context/connection-handler "/ws" :ping-interval 45)]
  (defn handler [config {m :request-method uri :uri :as req}]
    (def *req req)
    (if (= uri "/ws")
      (ws-handler req)
      (cond
        (= :post m)
        (handle-post req)

        (= uri "/")
        (h/render-response (partial page config))

        (= uri "/paintbots.css")
        {:status 200
         :body (slurp (io/resource "public/paintbots.css"))}

        :else
        (do
          (println "Someone tried: " (pr-str (:uri req)))
          {:status 404
           :body "Ain't nothing more here for you, go away!"})))))

(defn -main [& [config-file :as _args]]
  (let [config-file (or config-file "config.edn")
        _ (println "Reading config from: " config-file)
        {:keys [ip port] :as config} (read-string (slurp config-file))]
    (println "Config: " (pr-str config))
    (send state merge (select-keys config [:width :height :command-duration-ms]))
    (png/listen! state config)
    (alter-var-root #'server
                    (fn [_]
                      (httpkit/run-server (params/wrap-params
                                           (partial #'handler config))
                                          {:ip ip :port port
                                           :thread 32})))))

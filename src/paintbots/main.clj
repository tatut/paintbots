(ns paintbots.main
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :as params]
            [ring.util.io :as ring-io]
            [clojure.core.async :refer [go <! timeout] :as async]
            [clojure.string :as str]
            [ripley.html :as h]
            [ripley.live.context :as context]
            [clojure.java.io :as io]
            [ripley.live.source :as source]
            [ripley.live.poll :as poll]
            [paintbots.png :as png]
            [paintbots.state :as state])
  (:import (java.awt.image BufferedImage)
           (java.awt Color Graphics2D)))

(defonce server nil)

(defn canvas-of [req]
  (let [c (-> req :uri (subs 1))]
    (if (= c "")
      "scratch"
      c)))

(defn register [{{name :register} :form-params :as req}]
  (let [canvas (canvas-of req)
        name (str/trim name)
        state (state/current-state)]
    (cond
      (not (state/has-canvas? state canvas))
      {:status 404
       :body "No such art, try again :("}

      (state/bot-registered? state canvas name)
      {:status 409
       :body "Already registered!"}

      :else

      (let [id (state/cmd-sync! :register
                                :canvas canvas
                                :name name)]
        {:status 200 :body id}))))

(defn bot-command [{{:keys [id] :as params} :form-params :as req} command-fn]
  (let [canvas (canvas-of req)
        state (state/current-state)
        bot (state/bot-by-id state canvas id)]
    (cond
      (nil? bot)
      {:status 409
       :body (str "Bot with id " id " is not registered!")}

      (:in-command? bot)
      {:status 409
       :body "Already issuing a command in another request!"}

      :else
      (do
        (state/cmd! :in-bot-command :canvas canvas :id id)
        (httpkit/as-channel
         req
         {:on-open (fn [ch]
                     (go
                       (<! (timeout (:command-duration-ms state)))
                       (let [{:keys [x y color]} (state/cmd-sync! :bot-command
                                                                  :canvas canvas
                                                                  :id id
                                                                  :command-fn (partial command-fn params))]
                         (httpkit/send! ch {:status 200
                                            :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                            :body (str "x=" x "&y=" y "&color=" (state/color-name color))}
                                        true))))})))))
(defn move [req]
  (bot-command
   req
   (fn [{:keys [move]} {:keys [x y] :as bot} _img]
     (let [[new-x new-y] (case move
                           "UP" [x (dec y)]
                           "DOWN" [x (inc y)]
                           "LEFT" [(dec x) y]
                           "RIGHT" [(inc x) y])]
       (assoc bot
              :x new-x
              :y new-y)))))

(defn change-color [req]
  (bot-command
   req
   (fn [{:keys [color]} bot _img]
     (if-let [c (get state/colors color)]
       (assoc bot :color c)
       (do
         (println "Unrecognized color: " color)
         bot)))))

(defn say [req]
  (bot-command
   req
   (fn [{:keys [msg]} bot _img]
     (assoc bot :msg msg))))

(def ->col (memoize (fn [[r g b]]
                      (.getRGB (Color. ^int r ^int g ^int b)))))

(defn paint [req]
  (bot-command
   req
   (fn [_ {:keys [x y color] :as bot} ^BufferedImage img]
     (.setRGB img x y (->col color))
     bot)))

(defn clear [req]
  (bot-command
   req
   (fn [state {:keys [id]}]
     (let [{:keys [x y]} (get-in state [:bots id])
           ]
       ;; clear! gfx
       ))
   ))

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
     [:button.btn.btn-sm.mx-2 {:on-click #(println "FIXME")} "admin: clear"]
     ]]))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn page [{:keys [width height] :as _config} req]
  (let [canvas-name (canvas-of req)
        state-source (poll/poll-source 1000 #(state/current-state))
        canvas-changed (source/computed #(get-in % [:canvas canvas-name :last-command]) state-source)
        bots (source/computed #(get-in % [:canvas canvas-name :bots]) state-source)]
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
             [::h/for [{n :name c :color m :msg} (vals bots)
                       :let [col-style (str "width: 16px; height: 16px; "
                                            "position: absolute; left: 2px; top: 2px;"
                                            "background-color: " (rgb c) ";")]]
              [:div.relative.pl-5 n [:div.inline {:style col-style}]
               [::h/when m
                [:q.italic.mx-4 m]]]]]))]

        [:div.border
         [::h/live canvas-changed
          (fn [_ts]
            (with-open [out (java.io.ByteArrayOutputStream.)]
              (png/png-to (state/canvas-image (state/current-state) canvas-name) out)
              (let [b (.toByteArray out)
                    b64 (.encodeToString (java.util.Base64/getEncoder) b)
                    src (str "data:image/png;base64," b64)]
                (h/html
                 [:img {:src src :style "width: 100%;"}])))
            ;; This works, but has flicker!
            #_(let [url (str "/" canvas-name ".png?_=" ts)]
                (h/html
                 [:img {:style "width: 100%;" :src url}])))]]]]])))

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

      (contains? p :color)
      (change-color req)

      (contains? p :msg)
      (say req)

      (contains? p :clear)
      (clear req)

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
        (h/render-response (partial #'page config req))

        (= uri "/paintbots.css")
        {:status 200
         :body (slurp (io/resource "public/paintbots.css"))}

        ;; Try to download PNG of a canvas
        (str/ends-with? uri ".png")
        (let [canvas (some-> req  canvas-of (str/replace #".png$" ""))]
          (if-let [img (state/canvas-image (state/current-state) canvas)]
            {:status 200
             :headers {"Cache-Control" "no-cache"}
             :body (ring-io/piped-input-stream (partial png/png-to img))}
            {:status 404
             :body "No such canvas!"}))

        :else
        (do
          (println "Someone tried: " (pr-str (:uri req)))
          {:status 404
           :body "Ain't nothing more here for you, go away!"})))))

(defn -main [& [config-file :as _args]]
  (let [config-file (or config-file "config.edn")
        _ (println "Reading config from: " config-file)
        {:keys [ip port width height command-duration-ms] :as config}
        (read-string (slurp config-file))]
    (println "Config: " (pr-str config))
    (state/cmd! :config
                :width width
                :height height
                :command-duration-ms command-duration-ms)
    (state/cmd! :create-canvas :name "scratch")
    #_(png/listen! state config)
    (alter-var-root #'server
                    (fn [_]
                      (httpkit/run-server (params/wrap-params
                                           (partial #'handler config))
                                          {:ip ip :port port
                                           :thread 32})))))

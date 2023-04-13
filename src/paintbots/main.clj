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
            [ripley.live.protocols :as p]
            [ripley.impl.dynamic :as dynamic]
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
                       (let [response (atom nil)
                             {:keys [x y color] :as bot}
                             (state/cmd-sync! :bot-command
                                              :canvas canvas
                                              :id id
                                              :command-fn (partial command-fn (assoc params ::response response)))
                             resp @response]
                         (httpkit/send!
                          ch
                          (if (string? resp)
                            {:status 200 :headers {"Content-Type" "text/plain"} :body resp}
                            {:status 200
                             :headers {"Content-Type" "application/x-www-form-urlencoded"}
                             :body (str "x=" x "&y=" y "&color=" (state/color-name color))})
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
     (let [msg (if (> (count msg) 100)
                 (subs msg 0 100)
                 msg)]
       (assoc bot :msg msg)))))

(def ->col (memoize (fn [[r g b]]
                      (.getRGB (Color. ^int r ^int g ^int b)))))

(defn paint [req]
  (bot-command
   req
   (fn [_ {:keys [x y color] :as bot} ^BufferedImage img]
     (when (and (< -1 x (.getWidth img))
                (< -1 y (.getHeight img)))
       (.setRGB img x y (->col color)))
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

(def from-col
  (memoize
   (fn [rgb]
     (let [c (java.awt.Color. rgb)]
       [(.getRed c) (.getGreen c) (.getBlue c)]))))

(defn look [req]
  (bot-command
   req
   (fn [{r ::response} bot ^BufferedImage img]
     ;; A hacky way to pass out a response
     (let [w (.getWidth img)
           h (.getHeight img)]
       (reset! r (with-out-str
                   (loop [y 0
                          x 0]
                     (cond
                       (= x w)
                       (do
                         (print "\n")
                         (recur (inc y) 0))

                       (= y h)
                       :done

                       :else
                       (let [c (.getRGB img x y)]
                         (print (if (zero? c) "." (state/color-name (from-col c))))
                         (recur y (inc x))))))))
     bot)))

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
     [:button.btn.btn-sm.mx-2 {:on-click "toggleBots()"} "toggle bots"]
     [:button.btn.btn-sm.mx-2 {:on-click #(println "FIXME")} "admin: clear"]
     ]]))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn page [{:keys [width height] :as _config} req]
  (let [canvas-name (canvas-of req)
        state-source (poll/poll-source 1000 #(state/current-state))
        canvas-changed (source/computed #(get-in % [:canvas canvas-name :last-command]) state-source)
        bots (source/computed #(get-in % [:canvas canvas-name :bots]) state-source)
        resize-callback (p/register-callback! dynamic/*live-context*
                                              (fn [& args]
                                                (println "resize callback: " (pr-str args))))]
    (h/out! "<!DOCTYPE html>\n")
    (h/html
     [:html {:data-theme "dracula"}
      [:head
       [:meta {:charset "UTF-8"}]
       [:link {:rel "stylesheet" :href "/paintbots.css"}]
       (h/live-client-script "/ws")
       [:script {:type "text/javascript"}
        "function toggleBots() { let b = document.querySelector('#bot-positions'); b.style.display = b.style.display == '' ? 'none' : ''; }; "
        "window.addEventListener('resize', (event) => {_rs(" resize-callback ", [document.querySelector('#gfx').width])})"]
       [:style
        "#gfx { image-rendering: pixelated; width: 100%; position: absolute; }"]]
      [:body
       (app-bar)
       [:div.page
        [::h/live bots
         (fn [bots]
           (h/html
            [:div.bots.flex.flex-row
             "Painters: "
             [::h/for [{n :name c :color m :msg} (vals bots)
                       :let [col-style (str "width: 16px; height: 16px; "
                                            "position: absolute; left: 2px; top: 2px;"
                                            "background-color: " (rgb c) ";")]]
              [:div.inline.relative.ml-5.pl-5 n [:div.inline {:style col-style}]
               [::h/when m
                [:q.italic.mx-4 m]]]]]))]

        [:div
         [::h/live canvas-changed
          (fn [_ts]
            (with-open [out (java.io.ByteArrayOutputStream.)]
              (png/png-to (state/canvas-image (state/current-state) canvas-name) out)
              (let [b (.toByteArray out)
                    b64 (.encodeToString (java.util.Base64/getEncoder) b)
                    src (str "data:image/png;base64," b64)]
                (h/html
                 [:img {:id "gfx" :src src}])))
            ;; This works, but has flicker!
            #_(let [url (str "/" canvas-name ".png?_=" ts)]
                (h/html
                 [:img {:style "width: 100%;" :src url}])))]
         [:svg#bot-positions {:viewBox (str "0 0 " width " " height)}
          [::h/live bots
           (fn [bots]
             (def *b bots)
             (let [bots (vals bots)
                   s (pr-str bots)]
               (h/html
                [:g.bots
                 [::h/for [{:keys [x y color name]} bots
                           :let [c (apply format "#%02x%02x%02x" color)]]
                  [:g
                   [:text {:x (- x 2) :y (- y 2.5) :font-size 2 :fill "white"} name]
                   [:circle {:cx (+ 0.5 x) :cy (+ 0.5 y) :r 2 :stroke c :stroke-width 0.25}]]]])))]]]]]])))

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

      (contains? p :look)
      (look req)

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

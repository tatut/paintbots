(ns paintbots.main
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.params :as params]
            [ring.util.io :as ring-io]
            [ring.util.codec :as ring-codec]
            [clojure.core.async :refer [go <! timeout] :as async]
            [clojure.string :as str]
            [ripley.html :as h]
            [ripley.live.context :as context]
            [ripley.js :as js]
            [clojure.java.io :as io]
            [ripley.live.source :as source]
            [ripley.live.poll :as poll]
            [paintbots.png :as png]
            [paintbots.state :as state]
            [paintbots.video :as video]
            [paintbots.client :as client]
            [cheshire.core :as cheshire])
  (:import (java.awt.image BufferedImage)
           (java.awt Color)))

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
      (let [id (state/cmd-sync! :register :canvas canvas :name name)]
        {:status 200 :body id}))))

(defn handle-bot-command [canvas command-duration-ms
                          command-fn params id
                          ch]
  (go
    (<! (timeout command-duration-ms))
    (let [response (atom nil)
          cmd-ch (state/cmd<! :bot-command
                              :canvas canvas
                              :id id
                              :command-fn (partial command-fn (assoc params ::response response)))
          {:keys [x y color] :as bot} (<! cmd-ch)
          resp @response]
      (httpkit/send!
       ch
       (cond
         (string? resp)
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body resp}

         (some? resp)
         {:status 200
          :headers {"Content-Type" "application/json"}
          :body (cheshire/encode resp)}

         :else
         {:status 200
          :headers {"Content-Type" "application/x-www-form-urlencoded"}
          :body (str "x=" x "&y=" y "&color=" (state/color-name color))})))))

(defn bot-command [req command-fn]
  (let [state (state/current-state)]
    (if (::ws req)
      ;; WebSocket bot command
      (let [{:keys [id canvas params]} req]
        (handle-bot-command canvas (:command-duration-ms state)
                            command-fn params id (::ws req)))

      ;; Regular HTTP command
      (let [{{:keys [id] :as params} :form-params} req
            canvas (canvas-of req)
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
             {:on-open (partial handle-bot-command canvas (:command-duration-ms state)
                                command-fn params id)})))))))

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

(defn paint
  "Paint the pixel at the current position with the current color."
  [req]
  (bot-command
   req
   (fn [_ {:keys [x y color] :as bot} with-img]
     (with-img
       (fn [^BufferedImage img]
         (when (and (< -1 x (.getWidth img))
                    (< -1 y (.getHeight img)))
           (.setRGB img x y (->col color)))))
     bot)))

(defn info
  "No-op command for returning bots own info."
  [req]
  (bot-command
   req
   (fn [_ bot _] bot)))

(let [clear-color 0]
  (defn clear [req]
    (bot-command
     req
     (fn [_state {:keys [x y] :as bot} with-img]
       (with-img
         (fn [^BufferedImage img]
           (when (and (< -1 x (.getWidth img))
                      (< -1 y (.getHeight img)))
             (.setRGB img x y clear-color))))
       bot))))


(defn bye [{{:keys [id]} :form-params :as req}]
  (state/cmd-sync! :deregister
                   :canvas (canvas-of req)
                   :id id)
  {:status 204})

(def from-col
  (memoize
   (fn [rgb]
     (let [c (java.awt.Color. rgb)]
       [(.getRed c) (.getGreen c) (.getBlue c)]))))

(defn look [req]
  (bot-command
   req
   (fn [{r ::response} bot with-img]
     (with-img
       (fn [^BufferedImage img]
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
                             (recur y (inc x))))))))))
     bot)))

(defn bots [req]
  (bot-command
   req
   (fn [{r ::response} bot _]
     (reset! r
             (let [c (canvas-of req)
                   state (state/current-state)]
               (if (state/has-canvas? state c)
                 (state/canvas-bots (state/current-state) c)
                 [])))
     bot)))

(defn app-bar [req]
  (h/html
   [:nav.navbar.bg-base-100
    [:div.flex-1
     [:span.font-semibold "PaintBots"]]

    [:div.navbar-start.ml-4
     [::h/live (state/source :canvas keys)
      (fn [canvas-opts]
        (let [canvas (canvas-of req)]
          (h/html
           [:select.select {:on-change "window.location.pathname = window.event.target.value;"}
            [::h/for [o canvas-opts
                      :let [selected? (= canvas o)]]
             [:option {:value o :selected selected?} o]]])))]]

    [:div.navbar-end
     [:button.btn.btn-sm.mx-2 {:on-click "toggleBots()"} "toggle bots"]]]))

(defn rgb [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(def background-image-css
  (str "#canvas::before { content: ''; position: absolute; top: 50; left: 0; width: 100%; height: 100%; "
        "opacity: 0.1; z-index: -1; background-image: url('/logo.png'); "
        "background-repeat: no-repeat; background-size: cover; }"))

(defn with-page [head-fn body-fn]
  (h/out! "<!DOCTYPE html>\n")
  (h/html
   [:html {:data-theme "dracula"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:link {:rel "stylesheet" :href "/paintbots.css"}]
     [:link {:rel "icon" :href "/favicon.ico" :type "image/x-icon"}]
     (h/live-client-script "/ws")
     (head-fn)]
    [:body
     (body-fn)]]))

(defn page [{:keys [width height background-logo?] :as _config} req]
  (let [canvas-name (canvas-of req)
        state-source (poll/poll-source 1000 #(state/current-state))
        canvas-changed (png/png-bytes-source canvas-name)
        bots (source/computed #(get-in % [:canvas canvas-name :bots]) state-source)
        client? (contains? (:query-params req) "client")]
    (with-page
      ;; head stuff
      #(do
         (h/html
          [:script {:type "text/javascript"}
           "function toggleBots() { let b = document.querySelector('#bot-positions'); b.style.display = b.style.display == '' ? 'none' : ''; }; "])
         (h/html
          [:style
           "#gfx { image-rendering: pixelated; width: 100%; position: absolute; z-index: 99; } "
           "#bot-positions { z-index: 100; } "
           (when background-logo?
             (h/out! background-image-css))])
         (when client?
           (client/client-head)))

      ;; page content
      #(do
         (app-bar req)
         (h/html
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

           [:div#canvas
            [::h/live canvas-changed
             (fn [b]
               (let [b64 (.encodeToString (java.util.Base64/getEncoder) b)
                     src (str "data:image/png;base64," b64)]
                 (h/html
                  [:img {:id "gfx" :src src}])))]
            [:svg#bot-positions {:viewBox (str "0 0 " width " " height)}
             [::h/live bots
              (fn [bots]
                (try
                  (let [bots (vals bots)]
                    (h/html
                     [:g.bots
                      [::h/for [{:keys [x y color name]} bots
                                :let [c (apply format "#%02x%02x%02x" color)]]
                       [:g
                        [:text {:x (- x 2) :y (- y 2.5) :font-size 2 :fill "white"} name]
                        [:circle {:cx (+ 0.5 x) :cy (+ 0.5 y) :r 2 :stroke c :stroke-width 0.25}]]]]))
                  (catch Exception e
                    (println "EX: " e ", BOTS: " (pr-str bots)))))]]]
           [::h/when client?
            (client/client-ui)]])))))

(defn admin-panel [config req]
  (h/html
   [:div.m-4

    [:h2 "Canvases"]
    [::h/live (state/source :canvas #(for [[name {bots :bots}] %]
                                       [name (for [[id {name :name}] bots]
                                               [id name])]))
     (fn [canvases]
       (h/html
        [:div.canvases.flex.flex-wrap
         [::h/for [[canvas-name bots] canvases
                   :let [img (str "/" canvas-name ".png")]]
          [:div {:class "card w-96 bg-base-100 shadow-xl m-4 max-w-1/3"}
           [:figure [:img {:src img}]]
           [:div.card-body
            [:h2.card-title canvas-name]
            [:a {:href (str "/" canvas-name ".mp4") :target :_blank} "video"]
            "Bots:"
            [:ul
             [::h/for [[id name] bots]
              [:li name [:button.btn.btn-xs.ml-2 {:on-click #(state/cmd! :deregister {:canvas canvas-name
                                                                                      :id id})}
                         "kick"]]]]]

           [:div.card-actions.justify-end
            [:button.btn.btn-md.btn-warning
             {:on-click (js/js-when "confirm('Really lose this fine art?')"
                                    #(state/cmd! :clear-canvas :name canvas-name))} "Clear canvas"]]]]]))]

    [:div
     [:h2 "Add new canvas"]
     [:input.input.input-bordered#newcanvas
      {:placeholder "name (letters only)"}]
     [:button.btn.btn-md.btn-primary
      {:on-click (js/js (fn [name-input]
                          (let [name (reduce str (filter #(Character/isLetter %) name-input))]
                            (when (and (not (str/blank? name))
                                       (not= "admin" name))
                              (state/cmd! :create-canvas :name name))))
                        (js/input-value :newcanvas))} "Create"]]]))

(defn admin-page [config req]
  (let [[activated set-activated!] (source/use-state false)
        activate! (fn [password]
                    (when (= password (get-in config [:admin :password]))
                      (set-activated! true)))]
    (with-page
      (constantly nil)
      #(h/html
        [:div.admin
         [::h/live activated
          (fn [activated]
            (if activated
              (admin-panel config req)
              (h/html
               [:div.form-control.m-4
                [:label.label "Yeah. Whatsda passwoid?"]
                [:input#adminpw
                 {:autofocus true
                  :type :password
                  :on-keypress (js/js-when js/enter-pressed?
                                           activate!
                                           (js/input-value "adminpw"))}]])))]]))))


(def command-handlers
  [[:register #'register]
   [:info #'info]
   [:move #'move]
   [:paint #'paint]
   [:color #'change-color]
   [:msg #'say]
   [:clear #'clear]
   [:look #'look]
   [:bots #'bots]
   [:bye #'bye]])

(defn- keywordize-params [p]
  (into {}
        (map (fn [[k v]]
               [(keyword k) v]))
        p))

(defn- params->command [p]
  (some (fn [[required-param handler-fn]]
          (when (contains? p required-param)
            handler-fn))
        command-handlers))

(defn handle-post [req]
  (let [{p :form-params :as req}
        (update req :form-params keywordize-params)
        cmd-handler (params->command p)]

    (or (when cmd-handler
          (cmd-handler req))
        {:status 404
         :body "I don't recognize those parameters, try something else."})))

(defn handle-bot-ws [req]
  (let [canvas (canvas-of req)
        bot-id (atom nil)
        deregister! #(when-let [id @bot-id]
                       (state/cmd! :deregister :canvas canvas :id id))
        close! (fn [ch]
                 (deregister!)
                 (httpkit/close ch))]
    (httpkit/as-channel
     req
     {:on-open (fn [ch]
                 (if-not (httpkit/websocket? ch)
                   (httpkit/close ch)
                   (println "WS connected" req)))
      :on-close (fn [_ch _status] (deregister!))
      :on-receive (fn [ch msg]
                    (let [form (some-> msg str/trim (ring-codec/form-decode "UTF-8"))
                          params (when (map? form)
                                   (keywordize-params form))
                          id @bot-id]
                      (cond
                        ;; Something unreadable, just close
                        (nil? params)
                        (do
                          (println "Closing bot WS due to unreadable stuff: " msg)
                          (close! ch))

                        ;; Not registered yet, handle registration
                        (and (nil? id) (:register params))
                        (let [res (state/cmd-sync! :register :canvas canvas :name (:register params))]
                          (if (string? res)
                            (do (reset! bot-id res)
                                (httpkit/send! ch "OK"))
                            (do (httpkit/send! ch (:error res))
                                (close! ch))))

                        ;; Registered, handle a command
                        id
                        (let [params (dissoc params :register)
                              cmd (params->command params)]
                          (cmd {::ws ch
                                :params params
                                :id id
                                :canvas canvas})))))})))

(def assets
  {"/paintbots.css" {:t "text/css"}
   "/favicon.ico" {:t "image/x-icon"}
   "/logo.png" {:t "image/png"}

   "/client/swipl-bundle.js" {:t "text/javascript" :enc "gzip" :f "/client/swipl-bundle.js.gz"}
   "/client/logo.pl" {:t :text/prolog}})

(defn asset [{uri :uri :as req}]
  (when-let [asset (assets uri)]
    (let [file (->> (or (:f asset) uri) (str "public") io/resource)]
      {:status 200
       :headers (merge {"Content-Type" (:t asset)}
                       (when-let [enc (:enc asset)]
                         {"Content-Encoding" enc}))
       :body (ring-io/piped-input-stream
              (fn [out]
                (with-open [in (.openStream file)]
                  (io/copy in out))))})))

(let [ws-handler (context/connection-handler "/ws" :ping-interval 45)]
  (defn handler [config {m :request-method uri :uri :as req}]
    (if (= uri "/ws")
      (ws-handler req)
      (or (asset req)
          (cond
            (= :post m)
            (handle-post req)

            ;; Support bots connecting via WS to increase speed!
            (contains? (:headers req) "upgrade")
            (handle-bot-ws req)

            (= uri "/admin")
            (h/render-response (partial #'admin-page config req))

            ;; Try to download PNG of a canvas
            (str/ends-with? uri ".png")
            (let [canvas (some-> req canvas-of (str/replace #".png$" "") state/valid-canvas)]
              (if-let [png (png/current-png-bytes canvas)]
                {:status 200
                 :headers {"Cache-Control" "no-cache"}
                 :body png}
                {:status 404
                 :body "No such canvas!"}))

            ;; Try to download MP4 video of canvas snapshots
            (str/ends-with? uri ".mp4")
            (if-let [canvas (some-> req canvas-of (str/replace #".mp4$" "") state/valid-canvas)]
              {:status 200
               :headers {"Content-Type" "video/mp4"}
               :body (ring-io/piped-input-stream
                      (fn [out]
                        (video/generate (:video config) canvas out)))}
              {:status 404
               :body "No such canvas!"})

            :else
            (if (state/has-canvas? (state/current-state) (canvas-of req))
              (h/render-response (partial #'page config req))
              (do (println "Someone tried: " (pr-str (:uri req)))
                  {:status 404
                   :body "Ain't nothing more here for you, go away!"})))))))

(defn -main [& [config-file :as _args]]
  (let [config-file (or config-file "config.edn")
        _ (println "Reading config from: " config-file)
        {:keys [ip port width height command-duration-ms] :as config}
        (read-string (slurp config-file))]
    (println "Config: " (pr-str config))
    (state/cmd-sync! :config
                     :width width
                     :height height
                     :command-duration-ms command-duration-ms)
    (state/cmd-sync! :create-canvas :name "scratch")
    (png/listen! state/state config)
    (alter-var-root #'server
                    (fn [_]
                      (httpkit/run-server (params/wrap-params
                                           (partial #'handler config))
                                          {:ip ip :port port
                                           :thread 32})))))

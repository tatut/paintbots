(ns paintbots.state
  "Keeper of state!
  Contains all the game state in a single atom, and multimethods to
  process the state.

  A single go-loop process listens to commands that modify the game state
  and processes them in order."
  (:require [clojure.core.async :refer [go go-loop <! >! timeout] :as async]
            [clojure.string :as str]
            [ripley.live.source :as source])
  (:import (java.awt.image BufferedImage)
           (java.awt Color Graphics2D)))

(defn- hex->rgb [hex]
  (let [hex (if (= \# (.charAt hex 0)) (subs hex 1) hex)]
    (mapv #(Integer/parseInt % 16)
          [(subs hex 0 2)
           (subs hex 2 4)
           (subs hex 4 6)])))

(defn- palette [& cols]
  (into {}
        (map-indexed
         (fn [i col]
           [(Integer/toHexString i) (hex->rgb col)]))
        cols))

;; pico-8 16 color palette from https://www.pixilart.com/palettes/pico-8-51001
(def colors
  (palette "#000000"
           "#1D2B53"
           "#7E2553"
           "#008751"
           "#AB5236"
           "#5F574F"
           "#C2C3C7"
           "#FFF1E8"
           "#FF004D"
           "#FFA300"
           "#FFEC27"
           "#00E436"
           "#29ADFF"
           "#83769C"
           "#FF77A8"
           "#FFCCAA"))


#_(def colors {"R" [255 0 0]              ; red
             "G" [0 255 0]              ; green
             "B" [0 0 255]              ; blue
             "Y" [255 255 0]            ; yellow
             "P" [255 0 255]            ; pink
             "C" [0 255 255]            ; cyan
             })

(def color-name (into {} (map (juxt val key)) colors))

;; State is a map containing configuration and a canvases
;; under key :canvas which maps from canvas name to
;; map containing currently registered bots (map of bot id to info)
;; and canvas (BufferedImage)
;;
;; Admin can create new canvases
;;
;; the default canvas (no path) is named just the empty string ""
;; A new canvas is created with create-canvas function that can be
;; sent to the agent
(defonce state (atom {:canvas {}}))

(defmulti process! (fn [_old-state command] (::command command)))

(defonce state-processor-ch
  (let [ch (async/chan 32)]
    (go-loop [cmd (<! ch)]
      ;;(println "GOT: " (pr-str cmd))
      (if (= cmd ::stop)
        (println "Stop command issued, bye!")
        (let [{reply-ch ::reply} cmd
              old-state @state
              [new-state result] (try
                                   (process! old-state cmd)
                                   (catch Throwable e
                                     (println "[ERROR] cmd: " cmd " => " e)
                                     [old-state {:error (.getMessage e)}]))]
          ;; We don't use swap! as we need new-state and result, and
          ;; no other code will modify state
          (reset! state new-state)
          (when reply-ch
            (>! reply-ch result)
            (async/close! reply-ch))
          (recur (<! ch)))))
    ch))

(defn cmd!
  "Issue a command."
  [command & {:as cmd-args}]
  (async/>!! state-processor-ch (assoc cmd-args ::command command)))

(defn cmd-sync!
  "Issue a command synchronously, returns commdn result."
  [command & {:as cmd-args}]
  (let [ch (async/chan 1)]
    (async/>!! state-processor-ch (assoc cmd-args
                                         ::command command
                                         ::reply ch))
    (async/<!! ch)))

(defmethod process! :config [old-state config]
  [(merge old-state (dissoc config ::command ::reply))
   :ok])

(defmethod process! :create-canvas [{:keys [width height] :as state} {name :name}]
  (let [img (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        gfx (.createGraphics img)]
    [(assoc-in state [:canvas name]
               {:bots {} :img img :gfx gfx})
     :ok]))

(defmethod process! :clear-canvas [{:keys [width height] :as state} {name :name}]
  (when-let [img (get-in state [:canvas name :img])]
    (doto img
      (.setRGB 0 0 width height (int-array width 0) 0 0)))
  [state :ok])

(defn next-free-start-pos
  "Split canvas in zones and attempt to find a free zone for a registering bot.
  If a free zone is found, put the bot around the middle of the free zone.
  If there are no free zones available, put the bot in a random position on canvas."
  [canvas-width canvas-height bots]
  (let [bot-positions (map (juxt :x :y) (vals bots))
        n-cols 3
        n-rows 3
        zone-width (Math/round ^float (/ canvas-width n-cols))
        zone-height (Math/round ^float (/ canvas-height n-rows))
        zones (for [col (range n-cols)
                    row (range n-rows)]
                ;; [min-x min-y max-x max-y]
                [(* col zone-width) (* row zone-height)
                 (* (inc col) zone-width) (* (inc row) zone-height)])
        ;; Filter out zones that have a bot already
        free-zones (filter (fn [zone]
                             (let [[min-x min-y max-x max-y] zone]
                               (not (some (fn [[bot-x bot-y]]
                                            (and (<= min-x bot-x max-x) (<= min-y bot-y max-y)))
                                          bot-positions))))
                           zones)]
    (if (seq free-zones)
      ;; Put the bot around the middle of a free zone. Add some randomness in the position.
      (let [[min-x min-y max-x max-y] (first free-zones)]
        {:x (int (+ min-x (Math/floor (/ (- max-x min-x) 2)) (rand-int 5)))
         :y (int (+ min-y (Math/floor (/ (- max-y min-y) 2)) (rand-int 5)))})
      ;; No free zones available, return a random position.
      {:x (rand-int canvas-width)
       :y (rand-int canvas-height)})))

(defmethod process! :register [{:keys [width height] :as state} {:keys [canvas name]}]
  (let [id (str (random-uuid))
        bots (get-in (:canvas state) [canvas :bots])
        free-start-pos (next-free-start-pos width height bots)
        new-state (assoc-in state [:canvas canvas :bots id]
                            {:name name
                             :x (:x free-start-pos)
                             :y (:y free-start-pos)
                             :color (rand-nth (vals colors))
                             :registered-at (java.util.Date.)})]
    [new-state id]))

(defmethod process! :deregister [old-state {:keys [canvas id] :as args}]

  [(if (get-in old-state [:canvas canvas :bots id])
     (update-in old-state [:canvas canvas :bots] dissoc id)
     old-state) :ok])

(defmethod process! :in-bot-command [state {:keys [canvas id]}]
  [(assoc-in state [:canvas canvas :bots id :in-command?] true)
   :ok])

(defmethod process! :bot-command [state {:keys [canvas id command-fn]}]
  (let [bot (get-in state [:canvas canvas :bots id])
        img (get-in state [:canvas canvas :img])
        new-bot (command-fn bot img)]
    [(-> state
         (assoc-in [:canvas canvas :bots id] (assoc new-bot :in-command? false))
         (assoc-in [:canvas canvas :last-command] (System/currentTimeMillis)))
     new-bot]))

(defn current-state [] @state)

(defn has-canvas? [state canvas-name]
  (and (string? canvas-name)
       (contains? (:canvas state) canvas-name)))

(defn bot-registered? [state canvas-name candidate-name]
  (some (fn [[_ {bot-name :name}]]
          (= bot-name candidate-name))
        (get-in state [:canvas canvas-name :bots])))

(defn bot-by-id [state canvas-name bot-id]
  (get-in state [:canvas canvas-name :bots bot-id]))

(defn canvas-image [state canvas-name]
  (get-in state [:canvas canvas-name :img]))

(defn canvas-bots
  "Return information on the bots currently registered for the given canvas."
  [state canvas-name]
  (vals (get-in state [:canvas canvas-name :bots])))

(defn source
  "Return a ripley live source reflecting the given path.
  Path-fns may be keywords or other getter functions."
  [& path-fns]
  (source/computed (fn [current-state]
                     (reduce (fn [here f] (f here)) current-state path-fns))
                   state))

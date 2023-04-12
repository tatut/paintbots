(ns paintbots.state
  "Keeper of state!
  Contains all the game state in a single atom, and multimethods to
  process the state.

  A single go-loop process listens to commands that modify the game state
  and processes them in order."
  (:require [clojure.core.async :refer [go go-loop <! >! timeout] :as async]
            [clojure.string :as str])
  (:import (java.awt.image BufferedImage)
           (java.awt Color Graphics2D)))

(def colors {"R" [255 0 0]              ; red
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
      (println "GOT: " (pr-str cmd))
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

(defmethod process! :register [{:keys [width height] :as state} {:keys [canvas name]}]
  (let [id (str (random-uuid))
        new-state (assoc-in state [:canvas canvas :bots id]
                            {:name name
                             :x (rand-int width)
                             :y (rand-int height)
                             :color (rand-nth (vals colors))
                             :registered-at (java.util.Date.)})]
    [new-state id]))

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

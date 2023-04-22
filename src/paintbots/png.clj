(ns paintbots.png
  "Export PNGs from canvas data.

  The canvas-exporter thread periodically polls the
  state and creates byte arrays of all canvases.

  The process also outputs the canvas data to files
  so they can be exported into mp4 movies using ffmpeg."
  (:require [clojure.java.io :as io]
            [clojure.core.async :refer [go-loop <! timeout] :as async]
            [ripley.live.source :as source])
  (:import (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (java.awt Color Graphics2D)))

(set! *warn-on-reflection* true)

(defonce image-data (atom {}))

(defn img->bytes
  [^BufferedImage img]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (ImageIO/write img "png" out)
    (.flush out)
    (.toByteArray out)))

(defn listen!
  "Start a listener go block that polls state every `interval`
  milliseconds and writes a PNG if it has changed."
  [state {:keys [interval]
          :or {interval 500}}]
  (go-loop [canvas-changed {}]
    (let [current-state @state
          new-canvas-changed
          (reduce-kv
           (fn [changed-map canvas-name {:keys [lock img changed]}]
             (let [{previous-changed :changed i :i
                    :or {previous-changed 0 i 0}} (get changed-map canvas-name)
                   changed? (< previous-changed changed)]
               (when changed?
                 ;;(println "Canvas " canvas-name " changed at " changed)
                 (let [b (locking lock (img->bytes img))]
                   (swap! image-data assoc canvas-name b)
                   (io/copy b (io/file (format "%s_%06d.png" canvas-name i)))))
               (assoc changed-map canvas-name {:changed changed
                                               :i (if changed? (inc i) i)})))
           canvas-changed (:canvas current-state))]
      (<! (timeout interval))
      (recur new-canvas-changed))))

(defn current-png-bytes [canvas-name]
  (get @image-data canvas-name))

(defn png-bytes-source [canvas-name]
  (source/computed #(get % canvas-name) image-data))

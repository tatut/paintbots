(ns paintbots.png
  "Export PNG from the current canvas"
  (:require [clojure.java.io :as io]
            [clojure.core.async :refer [go-loop <! timeout] :as async])
  (:import (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (java.awt Color Graphics2D)))

(set! *warn-on-reflection* true)

(defn create-img [{:keys [width height canvas name]
                   :or {name "unnamed art"}}]
  (let [img (BufferedImage. width (+ height 30) BufferedImage/TYPE_INT_ARGB)
        ^Graphics2D g (.createGraphics img)
        ->col (memoize (fn [[r g b]]
                         (.getRGB (Color. ^int r ^int g ^int b))))]
    (doseq [x (range width)
            y (range height)
            :let [c (some-> canvas (get [x y]) ->col)]
            :when c]
      (.setRGB img x y c))
    (.setColor g Color/BLACK)
    (.drawString g
                 (str name "    " (java.util.Date.))
                 10 (int (+ (* 4 height) 20)))
    img))

(defn png-to [^BufferedImage img ^java.io.OutputStream out]
  (ImageIO/write img "png" out))

(defn png
  ([state] (png "art_" state))
  ([file-name state]
   (let [^BufferedImage img (create-img state)]
     (ImageIO/write img "png" (io/file file-name)))))

(defn listen!
  "Start a listener go block that polls state every `interval`
  milliseconds and writes a PNG if it has changed."
  [state {:keys [interval prefix]
          :or {interval 1000
               prefix "art_"}}]
  (go-loop [prev nil
            i 0]
    (let [current @state
          changed? (not= current prev)]
      (when changed?
        ;; State has changed, write a new image
        (png (format "%s%06d.png" prefix i) current))
      (<! (timeout interval))
      (recur current (if changed? (inc i) i)))))

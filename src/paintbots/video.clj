(ns paintbots.video
  "Generate video (using ffmpeg) from exported PNG images."
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]))

(defn generate [{:keys [ffmpeg-executable framerate]
                 :or {ffmpeg-executable "ffmpeg"
                      framerate 10}}
                canvas-name to]
  (let [f (java.io.File/createTempFile canvas-name ".mp4")]
    (try
      (let [res
            (sh/sh "ffmpeg" "-framerate" (str framerate) "-i" (str canvas-name "_%06d.png")
                   "-c:v" "libx264" "-pix_fmt" "yuv420p" "-y"
                   (.getAbsolutePath f))]
        (when (not= 0 (:exit res))
          (println "Video generation failed: " (:err res))))
      (io/copy f to)
      (finally
        (io/delete-file f true)))))

(ns nxbrowse.sound
  (:import (com.jogamp.openal ALFactory
                              AL)
           (com.jogamp.openal.util ALut)
           (java.nio ByteBuffer))
  (:require [clojure.tools.logging :as log]))



(def sound-props
  {:origin             (float-array [0 0 0])
   :listen-orientation (float-array [0 0 -1 0 1 0])
   :buffer             (int-array [0])
   :source             (int-array [0])
   :format (int-array [0])
   :size (int-array [0])
   :freq (int-array [0])
   :loop (int-array [0])
   :data (make-array ByteBuffer 1)
   })

(defn check-error
  [al msg]
  (when-not (= (.alGetError al) AL/AL_NO_ERROR)
    (throw (Exception. msg))))

(defn load-al-data
  "Loads AL buffer and stuff."
  [audio-input-stream]
  (try (let [{:keys [origin buffer source format size freq loop data]} sound-props
             al (ALFactory/getAL)
             msg "Could not generate audio buffer, there is probably not enough memory."]
         (.alGenBuffers al 1 buffer 0)
         (check-error al msg)
         (ALut/alutLoadWAVFile audio-input-stream
                               format
                               data
                               size
                               freq
                               loop)
         (.alBufferData al
                        (first buffer)
                        (first format)
                        (first data)
                        (first size)
                        (first freq))
         (.alGenSources al 1 source 0)
         (check-error al msg)
         (let [s (first source)]
           (-> al
               (.alSourcei s AL/AL_BUFFER (first buffer))
               (.alSourcef s AL/AL_PITCH 1.0)
               (.alSourcef s AL/AL_GAIN 1.0)
               (.alSourcefv s AL/AL_POSITION origin 0)
               (.alSourcefv s AL/AL_VELOCITY origin 0)
               (.alSourcei s AL/AL_LOOPING (first loop))))
         (check-error al "Couldn't set sound source properties."))
       (catch Exception e (log/warn (.getMessage e)))))

(defn play
  []
  (.alSourcePlay (ALFactory/getAL) (first (:source sound-props))))

(defn pause
  []
  (.alSourcePause (ALFactory/getAL) (first (:source sound-props))))

(defn stop
  []
  (.alSourceStop (ALFactory/getAL) (first (:source sound-props))))

(defn kill-al-data
  []
  (let [al (ALFactory/getAL)
        {:keys [buffer source]} sound-props]
    (-> al
        (.alDeleteBuffers 1 buffer 0)
        (.alDeleteBuffers 1 source 0))
    (ALut/alutExit)))

(defn init-al
  []
  (ALut/alutInit)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (log/info "Closing sound resources.")
                               (kill-al-data))))
  )
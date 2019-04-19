(ns microtonality.synth
  (:require [tonejs]))

(defn scale [v from to from-new to-new]
  (let [old-span (- to from)
        ratio (/ (- v from) old-span)
        new-span (- to-new from-new)]
    (+ from-new (* ratio new-span))))

(def synth-settings (clj->js {:envelope {:attack 0.1
                                         :decay 0.025
                                         :sustain 0.3
                                         :release 0.9}
                              :oscillator {:type "triangle"}}))

(defn to-master [n]
  (.toMaster n))

(defn create-synth! []
  (let [synth (-> (new js/Tone.Synth synth-settings)
                  to-master)]
    (set! (.-volume synth) -32)
    synth))

(defn set-master-defaults! []
  (set! (.-volume js/Tone.Master) -12))

(defn play-note! [synth frequency]
  (println "play-note! " synth " " frequency)
  (.triggerAttackRelease synth frequency))

(defn release-note! [synth]
  (.triggerRelease synth))
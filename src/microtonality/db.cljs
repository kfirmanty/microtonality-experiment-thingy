(ns microtonality.db)

(defn init-db []
  {:base-freq 440
   :upper-freq 880
   :sequencer {}
   :step 0
   :playing? false
   :tempo 100
   :scale-picker-width 500
   :safari-tonejs-fix-triggered? false})

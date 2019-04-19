(ns microtonality.db)

(defn init-db []
  {:base-freq 440
   :upper-freq 880
   :sequencer {}
   :step 0})

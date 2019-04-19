(ns microtonality.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::base-freq
 :base-freq)

(rf/reg-sub
 ::upper-freq
 :upper-freq)

(rf/reg-sub
 ::freqs
 (fn [db]
   (:freqs db)))

(rf/reg-sub
 ::sequencer
 :sequencer)

(rf/reg-sub
 ::step
 :step)

(rf/reg-sub
 ::tempo
 :tempo)

(rf/reg-sub
 ::playing?
 :playing?)

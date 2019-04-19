(ns microtonality.events
  (:require [re-frame.core :as rf]
            [microtonality.db :as db]
            [microtonality.synth :as synth]))

(rf/reg-event-db
 ::init
 (fn [_ _]
   (db/init-db)))

(rf/reg-event-db
 ::set-upper-freq
 (fn [db [_ value]]
   (assoc db :upper-freq value)))

(rf/reg-event-db
 ::set-base-freq
 (fn [db [_ value]]
   (assoc db :base-freq value)))

(rf/reg-event-db
 ::add-freq
 (fn [db [_ x y]]
   (update db :freqs conj x)))

(rf/reg-event-db
 ::remove-freq
 (fn [db [_ freq]]
   (let [freqs (->> db :freqs (into #{}))]
     (assoc db :freqs (remove #{freq} freqs)))))


(rf/reg-event-db
 ::step-clicked
 (fn [db [_ x y]]
   (update-in db [:sequencer x y] not)))

(rf/reg-event-fx
 ::sequencer-start
 (fn [{:keys [db]} _]
   (let [synth (or (:synth db) (synth/create-synth!))]
     (if (not (:playing? db))
       {:db (assoc db :step 0 :playing? true :synth synth)
        :dispatch [::sequencer-play-step]}
       {:db (assoc db :synth synth :playing? false)}))))

(rf/reg-fx
 ::trigger-synth
 (fn [[synth freq]]
   (when freq
     (synth/play-note! synth freq))))

(defn current-note [db step]
  (let [turned-on (get-in db [:sequencer step])]
    (->> turned-on (filter (fn [[k v]] (when v [k v]))) (filter some?) first first)))

(rf/reg-event-fx
 ::sequencer-play-step
 (fn [{:keys [db]} [_ step]]
   (let [freqs (:freqs db)
         step (mod step 16)
         current-note (current-note db step)
         tempo-ms 500]
     (if (:playing? db)
       {:db (assoc db :step step)
        :dispatch-later [{:ms tempo-ms
                          :dispatch [::sequencer-play-step (inc step)]}]
        ::trigger-synth [(:synth db) (when current-note
                                       (synth/scale (nth freqs current-note) 0 500 (:base-freq db) (:upper-freq db)))]}
       {:db db}))))

(ns microtonality.events
  (:require [re-frame.core :as rf]
            [microtonality.db :as db]
            [microtonality.synth :as synth]
            [clojure.string :as string]))

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

(defn index-of-insert [freq freqs]
  (let [freqs (sort freqs)
        lower (filter #(< % freq) freqs)]
    (count lower)))

(defn update-steps [sequencer index update-fn]
  (->> sequencer
       (map (fn [[k v]] [k (if (>= v index)
                             (update-fn v)
                             v)]))
       (into {})))

(defn remove-steps [sequencer index]
  (->> sequencer
       (map (fn [[k v]] (when (not= v index) [k v])))
       (filter some?)
       (into {})))

(rf/reg-event-db
 ::add-freq
 (fn [db [_ x y]]
   (let [index (index-of-insert x (:freqs db))]
     (-> db (update :freqs conj x)
         (update :sequencer update-steps index inc)))))

(rf/reg-event-db
 ::remove-freq
 (fn [db [_ freq]]
   (let [freqs (->> db :freqs (into #{}))
         index (index-of-insert freq (:freqs db))]
     (assoc db :freqs (remove #{freq} freqs)
            :sequencer (-> db :sequencer
                           (remove-steps index)
                           (update-steps index dec))))))


(rf/reg-event-db
 ::step-clicked
 (fn [db [_ x y]]
   (assoc-in db [:sequencer x] y)))

(rf/reg-event-fx
 ::sequencer-start
 (fn [{:keys [db]} _]
   (let [synth (or (:synth db) (synth/create-synth!))
         db (assoc db :safari-tonejs-fix-triggered? true)] ;;UGLY SAFARI FIX FOR TONEJS TO PLAY
     (if (not (:playing? db))
       {:db (assoc db :step 0 :playing? true :synth synth)
        :dispatch [::sequencer-play-step]}
       {:db (assoc db :synth synth :playing? false)}))))

(rf/reg-event-db
 ::set-tempo
 (fn [db [_ tempo]]
   (assoc db :tempo tempo)))

(rf/reg-fx
 ::trigger-synth
 (fn [[synth freq]]
   (when freq
     (synth/play-note! synth freq))))

(defn current-note [db step]
  (get-in db [:sequencer step]))

(rf/reg-event-fx
 ::sequencer-play-step
 (fn [{:keys [db]} [_ step]]
   (let [{:keys [freqs tempo playing? base-freq upper-freq scale-picker-width synth]} db
         step (mod step 16)
         current-note (current-note db step)
         tempo-ms (- 1000 (* 6 tempo))]
     (if playing?
       {:db (assoc db :step step)
        :dispatch-later [{:ms tempo-ms
                          :dispatch [::sequencer-play-step (inc step)]}]
        ::trigger-synth [synth (when (and current-note (not-empty freqs))
                                 (synth/scale (nth (sort freqs) current-note) 0 scale-picker-width base-freq upper-freq))]}
       {:db db}))))

(defn ->scala-file [cents file-name desc]
  (string/join \newline (flatten [(str "!" file-name)
                                  desc
                                  (count cents)
                                  cents])))

(rf/reg-event-db
 ::export-scala-file
 (fn [{:keys [freqs base-freq upper-freq scale-picker-width] :as db} _]
   (let [freqs (->> freqs
                    sort
                    (map #(synth/scale % 0 scale-picker-width base-freq upper-freq)))
         cents (synth/freqs->cents freqs)
         base64-contents (-> (->scala-file cents "met.scl"
                                           (str "Generated using http://firmanty.com/met/ " "frequencies: " (string/join " " (sort freqs))))
                             js/btoa)]
     (assoc db :scala-file (str "data:application/octet-stream;charset=utf-8;base64," base64-contents)))))

(rf/reg-event-db
 ::scala-file-downloaded
 (fn [db _]
   (dissoc db :scala-file)))

(ns ^:figwheel-hooks microtonality.core
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [microtonality.events :as events]
   [microtonality.subs :as subs]
   [microtonality.synth :as synth]))

(defn get-app-element []
  (gdom/getElement "app"))

(defn slider [event value min max]
  [:input {:type "range" :value value :min min :max max
           :style {:width "50%"}
           :on-change (fn [e]
                        (rf/dispatch [event (int (.. e -target -value))]))}])
"    var e = evt.target;
    var dim = e.getBoundingClientRect();
    var x = evt.clientX - dim.left;
    var y = evt.clientY - dim.top;
"
(defn svg-event->xy [evt]
  (let [e (.-target evt)
        dim (.getBoundingClientRect e)]
    [(- (.-clientX evt) (.-left dim))
     (- (.-clientY evt) (.-top dim))]))

(defn on-svg-click [evt]
  (let [[x y] (svg-event->xy evt)]
    (rf/dispatch [::events/add-freq x y])))

(defn sequencer []
  (let [width 500
        height 500
        steps 16
        notes (count @(rf/subscribe [::subs/freqs]))
        size-x (/ width steps)
        size-y (/ height notes)
        sequencer @(rf/subscribe [::subs/sequencer])
        step @(rf/subscribe [::subs/step])]
    [:svg {:width width
           :height height}
     (for [x (range steps) y (range notes)]
       (let [xr (* x size-x)
             yr (* y size-y)]
         ^{:key (str (* 3.14 x) (* 100 y))} [:rect {:x xr :y yr
                                                    :fill-opacity (if (= y (get sequencer x -1))
                                                                    1.0
                                                                    0.0)
                                                    :stroke "black"
                                                    :fill (if (not= x step) "black" "grey")
                                                    :width size-x
                                                    :height size-y
                                                    :on-click #(rf/dispatch [::events/step-clicked x y])}]))]))

(defn page []
  (let [base-freq @(rf/subscribe [::subs/base-freq])
        upper-freq @(rf/subscribe [::subs/upper-freq])
        freqs @(rf/subscribe [::subs/freqs])
        svg-height 100
        svg-width 500
        playing? @(rf/subscribe [::subs/playing?])
        tempo @(rf/subscribe [::subs/tempo])]
    [:article
     [:h1 "Microtonality Experiment Thingy"]
     [:section
      [:svg {:height svg-height
             :width svg-width}
       [:rect {:on-click on-svg-click
               :x 0 :y 0 :width svg-width :height svg-height
               :stroke "black"
               :stroke-opacity 0.8
               :fill-opacity 0.0}]
       (for [freq freqs]
         ^{:key freq} [:rect {:x freq :y 0 :width 10 :height svg-height
                              :on-click #(rf/dispatch [::events/remove-freq freq])}])]
      [:p (str "Base frequency: " base-freq)]
      [slider ::events/set-base-freq base-freq 40 880]
      [:p (str "Upper frequency: " upper-freq)]
      [slider ::events/set-upper-freq upper-freq base-freq 880]
      [:p (str "Frequencies: " (->> freqs (map #(synth/scale % 0 svg-width base-freq upper-freq))
                                    (map #(.toFixed % 2))
                                    sort
                                    (clojure.string/join " ")))]
      [:h3 "Sequencer:"]
      [sequencer]
      [:p "Tempo:"]
      [slider ::events/set-tempo tempo 10 200]
      [:p
       [:button {:type "button"
                 :style {:height "50px" :width "100px"}
                 :on-click #(rf/dispatch [::events/sequencer-start])} (if playing?
                                                                        "Stop!"
                                                                        "Start!")]]]]))

(defn mount [el]
  (reagent/render-component [page] el))

(defn mount-app-element []
  (rf/dispatch-sync [::events/init])
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))

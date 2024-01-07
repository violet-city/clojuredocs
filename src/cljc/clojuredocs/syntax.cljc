;; From https://github.com/holmsand/reagent

(ns clojuredocs.syntax
  (:require [clojure.string :as string]))

(def builtins #{"def" "defn" "ns" "atom" "let" "if" "when"
                "cond" "merge" "assoc" "swap!" "reset!" "for"
                "range" "nil?" "int" "or" "->" "->>" "%" "fn" "if-not"
                "empty?" "case" "str" "pos?" "zero?" "map" "remove"
                "empty" "into" "assoc-in" "dissoc" "get-in" "when-not"
                "filter" "vals" "count" "complement" "identity" "dotimes"
                "update-in" "sorted-map" "inc" "dec" "false" "true" "not"
                "=" "partial" "first" "second" "rest" "list" "conj"
                "drop" "when-let" "if-let" "add-watch" "mod" "quot"
                "bit-test" "vector"})

(def styles {:comment  {:style {:color "#008200"}}
             :str-litt {:style {:color "#2D2DFE"}}
             :keyw     {:style {:color "#77f"}}
             :builtin  {:style {:font-weight "normal"
                                :color "#687868"}}
             :def      {:style {:color "#55c"
                                :font-weight "normal"}}})

(def paren-styles [{:style {:color "#272"}}
                   {:style {:color "#940"}}
                   {:style {:color "#44a"}}])

(defn tokenize [src]
  (let [ws " \\t\\n"
        open "\\[\\(\\{"
        close "\\)\\]\\}"
        sep (str ws open close)
        comment-p ";.*"
        str-p "\"[^\"]*\""
        open-p (str "[" open "]")
        close-p (str "[" close "]")
        iden-p (str "[^" sep "]+")
        meta-p (str "\\^" iden-p)
        any-p (str "[" ws "]+" "|\\^[^" sep "]+|.")
        patt (re-pattern (str "("
                              (string/join ")|(" [comment-p
                                                  str-p open-p
                                                  close-p meta-p iden-p any-p])
                              ")"))
        keyw-re #"^:"]
    (for [[s comment str-litt open close met iden any] (re-seq patt src)]
      (cond
        comment [:comment s]
        str-litt [:str-litt s]
        open [:open s]
        close [:close s]
        met [:other s]
        iden (cond
               (re-find keyw-re s) [:keyw s]
               (builtins s) [:builtin s]
               :else [:iden s])
        any [:other s]))))

(defn format-style-clj [s]
  (->> s
       (map (fn [[k v]]
              (str (name k) ":" v)))
       (interpose ";")
       (apply str)))

(defn format-style [{:keys [style] :as opts}]
  (assoc opts :style (format-style-clj style)))

(defn syntaxify [src & opts]
  (let [{:keys [stringify-style?]} (apply hash-map opts)
        def-re #"^def|^ns\b"
        ncol (count paren-styles)
        paren-style (fn [level]
                      (nth paren-styles (mod level ncol)))]
    (loop [tokens (tokenize src)
           prev nil
           level 0
           res []]
      (let [[kind val] (first tokens)
            level' (case kind
                     :open (inc level)
                     :close (dec level)
                     level)
            style (case kind
                    :iden  (when (and prev (re-find def-re prev))
                             (:def styles))
                    :open  (paren-style level)
                    :close (paren-style level')
                    (styles kind))
            remain (rest tokens)]
        (if-not (empty? remain)
          (recur remain
                 (if (= kind :other) prev val)
                 level'
                 (conj res [:span
                            (if stringify-style?
                              (format-style style)
                              style)
                            val]))
          (apply vector :pre.syntaxify (conj res [:span style val])))))))

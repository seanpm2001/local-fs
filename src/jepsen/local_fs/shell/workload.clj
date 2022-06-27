(ns jepsen.local-fs.shell.workload
  "A combined workload for testing a local filesystem via shell operations: a
  generator, client, and checker which work together."
  (:require [jepsen [generator :as gen]]
            [jepsen.local-fs.shell [client :refer [client]]
                                   [checker :refer [checker]]]
            [clojure.test.check [clojure-test :refer :all]
                                [generators :as g]
                                [properties :as prop]
                                [results :refer [Result]]]))

(def gen-path
  "test.check generator for a random fs-test path. Generates vectors of length
  1 or 2 of \"a\" or \"b\"--we want a real small state space here or we'll
  almost never do anything interesting on the same files."
  (g/vector (g/elements ["a" "b"]) 1 2))

(def data-gen
  "Generates a short string of data to write to a file for an fs test"
  (g/scale #(/ % 100) g/string-alphanumeric))

(def data-gen
  "Generates a short string of data to write to a file for an fs test"
  (g/scale #(/ % 100) g/string-alphanumeric))

(def fs-op-gen
  "test.check generator for fs test ops. Generates append, write, mkdir, mv,
  read, etc."
  (g/one-of
    [(g/let [path gen-path]
       {:f :read, :value [path nil]})
     (g/let [path gen-path]
       {:f :touch, :value path})
     (g/let [path gen-path, data data-gen]
       {:f :append, :value [path (str " " data)]})
     (g/let [source gen-path, dest gen-path]
       {:f :mv, :value [source dest]})
     (g/let [path gen-path, data data-gen]
       {:f :write, :value [path data]})
     (g/let [path gen-path]
       {:f :mkdir, :value path})
     (g/let [path gen-path]
       {:f :rm, :value path})
     (g/let [from gen-path, to gen-path]
       {:f :ln, :value [from to]})]))

(def fs-history-gen
  "Generates a whole history"
  (g/scale (partial * 1000)
    (g/vector fs-op-gen)))

(defn workload
  "Constructs a new workload for a test."
  [{:keys [dir]}]
  {:client          (client dir)
   :checker         (checker)
   :test-check-gen  fs-history-gen})

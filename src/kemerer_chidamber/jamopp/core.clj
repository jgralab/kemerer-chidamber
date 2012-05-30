(ns kemerer-chidamber.jamopp.core
  (:require [funnyqt.utils :as u])
  (:use funnyqt.emf)
  (:use funnyqt.query)
  (:use funnyqt.query.emf))

;;* Convenience

(defn concrete-classifier-by-name
  "Returns the Type with the given (fully qualified) name n."
  [m n]
  (let [nm (name n)
        cun (str nm ".java")]
    (if-let [cu (first (filter #(= (eget % :name) cun)
                               (eallobjects m 'CompilationUnit)))]
      (let [sn (last (clojure.string/split nm #"\."))]
        (the #(= sn (eget % :name))
             (eget cu :classifiers)))
      (if-let [cs (seq (filter #(= (eget % :name) nm)
                               (eallobjects m 'ConcreteClassifier)))]
        (cond
         (next cs)   (u/error (format "%s is ambiguous." nm))
         :else       (first cs))
        (u/error (format "No such ConcreteClassifier %s." nm))))))


;;* Metrics

;;** Chidamber & Kemerer


(defn class-qname
  [c]
  (clojure.string/replace (eget (econtainer c) :name)
                          #"\.java$" ""))

(def ^{:dynamic true
       :doc "A function that should return all classes for which the metrics
  should be calculated."}
  *get-classes-fn*
  (fn [m]
    (eallobjects m 'Class)))

(defn apply-metric
  "Applies the given metric to all JGraLab classes."
  [g metric]
  (sort
   (seq-compare (constantly 0) #(- %2 %1) compare)
   (for [c (*get-classes-fn* g)]
     [c (metric c) (class-qname c)])))

(u/compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
              (do
                (def fj-pool (java.util.concurrent.ForkJoinPool.))
                (defn fj-do [vs metric]
                  (proxy [java.util.concurrent.RecursiveTask] []
                    (compute []
                      (if (< (count vs) 5)
                        (map (fn [c]
                               [c (metric c) (class-qname c)])
                             vs)
                        (let [half (int (/ (count vs) 2))
                              vs1 (subvec vs 0 half)
                              fj1 (.fork ^java.util.concurrent.RecursiveTask (fj-do vs1 metric))
                              vs2 (subvec vs half)
                              r2 (.compute ^java.util.concurrent.RecursiveTask (fj-do vs2 metric))
                              r1 (.join ^java.util.concurrent.RecursiveTask fj1)]
                          (concat r1 r2)))))))
              nil)

(defn apply-metric-forkjoin
  "Applies the given metric to all JGraLab classes in parallel using a
  ForkJoinPool."
  [g metric]
  (u/compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
                (sort
                 (seq-compare (constantly 0) #(- %2 %1) compare)
                 (let [vs (vec (*get-classes-fn* g))]
                   (.invoke fj-pool ^java.util.concurrent.RecursiveTask (fj-do vs metric))))
                (do (println "Sorry, ForkJoin application disabled.  Get a JDK7."
                             "Right now, we are simply falling back to sequential application.")
                    (apply-metric g metric))))


;;*** Depth of Inheritance Tree

(defn depth-of-inheritance-tree
  "Returns the depth of the inheritance tree of Type t.
  This only works for types whose source code has been parsed, but not for
  types that were merely referenced in some jar.  For example, the jamopp model
  does not contain the information that java.lang.Integer extends
  java.lang.Number, so that its DIT is actually 2, not 1."
  [t]
  (let [supers (reachables t [p-seq :extends :classifierReferences :target])]
    (cond
     (seq supers) (inc (apply max (map depth-of-inheritance-tree supers)))
     (let [cu (econtainer t)]
       (= (eget cu :name) "java.lang.Object.java")) 0
     :else 1)))

(defn classes-by-depth-of-inheritance-tree
  [g]
  (apply-metric g depth-of-inheritance-tree))

(defn classes-by-depth-of-inheritance-tree-forkjoin
  [g]
  (apply-metric-forkjoin g depth-of-inheritance-tree))


;;*** Coupling between Objects

(defn coupled-classes
  "Given a Class `c', calculates all coupled classes."
  [c]
  (reachables c
    [p-seq :members
     ;; :statements for ClassMethods, :initialValue for Fields
     [p-alt :statements :initialValue]
     [p-* [<>--]]
     [p-restr '[references.MethodCall references.IdentifierReference]]
     ;; Matches both the ClassMethod that is the target of a MethodCall as well
     ;; as the Field that is the target of a IdentifierReference
     :target
     [p-restr '[members.ClassMethod members.Field]]
     --<>
     [p-restr 'Class #(not (= c %1))]]))

(defn classes-by-coupling-between-objects
  [g]
  (apply-metric g #(count (coupled-classes %))))

(defn classes-by-coupling-between-objects-forkjoin
  [g]
  (apply-metric-forkjoin g #(count (coupled-classes %))))


;;*** Weighted Methods per Class

(defn cyclomatic-complexity
  "Returns the cyclomatic complexity of the given method."
  [m]
  (-> (reachables
       m [p-seq :statements
          [p-* <>--]
          [p-restr '[statements.NormalSwitchCase statements.DefaultSwitchCase
                     statements.Condition statements.ForLoop
                     statements.DoWhileLoop statements.WhileLoop]]])
      count
      inc))

(defn weighted-method-per-class
  "Returns the WMC metric for the given class c."
  [c]
  (reduce + (map cyclomatic-complexity
                 (reachables c [p-seq :members
                                      [p-restr 'members.ClassMethod]]))))

(defn classes-by-weighted-methods-per-class
  [g]
  (apply-metric g weighted-method-per-class))

(defn classes-by-weighted-methods-per-class-forkjoin
  [g]
  (apply-metric-forkjoin g weighted-method-per-class))


;;*** Number of Children

(defn subtypes
  "Returns all direct subtypes of the given type t that are contained in
  classifiers."
  [classes t]
  ;; Well, this is pretty slow, because all the needed references are
  ;; unidirectional.  You can get the superclass quickly, but getting
  ;; subclasses is hardly possible.
  (mapcat (fn [c]
            (let [supers (reachables c [p-seq :extends
                                        :classifierReferences
                                        :target])]
              (when (member? t supers)
                [c])))
          classes))

(defn classes-by-number-of-children
  [m]
  (let [jg-classes (*get-classes-fn* m)]
    (apply-metric m #(count (subtypes jg-classes %)))))

(defn classes-by-number-of-children-forkjoin
  [m]
  (let [jg-classes (*get-classes-fn* m)]
    (apply-metric-forkjoin m #(count (subtypes jg-classes %)))))

;;*** Response for a Class

(defn response-set
  "Returns the response set of the given type t."
  [t]
  (let [own-methods (reachables t [p-seq  :members
                                   [p-restr 'members.ClassMethod]])
        called-methods (set (mapcat
                             #(reachables % [p-seq :statements
                                             [p-* <>--]
                                             [p-restr 'references.MethodCall]
                                             :target])
                             own-methods))]
    (into own-methods called-methods)))

(defn classes-by-response-for-a-class
  [g]
  (apply-metric g #(count (response-set %))))

(defn classes-by-response-for-a-class-forkjoin
  [g]
  (apply-metric-forkjoin g #(count (response-set %))))


;;*** Lack of Cohesion in Methods

(defn lack-of-cohesion
  "Returns the lack of cohesion metric value of t."
  [t]
  (let [fields (reachables t [p-seq :members
                              [p-restr 'members.Field]])
        methods (reachables t [p-seq :members
                               [p-restr 'members.ClassMethod]])
        accessed-fields (fn [m]
                          (reachables m [p-seq :statements
                                         [p-* <>--]
                                         [p-restr 'references.IdentifierReference]
                                         :target
                                         [p-restr nil #(member? % fields)]]))
        method-field-map (apply hash-map (mapcat (fn [m] [m (accessed-fields m)])
                                                 methods))
        combinations (loop [ms methods, pairs []]
                       (if (next ms)
                         (recur (rest ms) (concat pairs
                                                  (map (fn [n] [(first ms) n])
                                                       (rest ms))))
                         pairs))
        results (for [[m1 m2] combinations
                      :let [f1 (method-field-map m1)
                            f2 (method-field-map m2)]]
                  (if (seq (clojure.set/intersection f1 f2))
                    :common-fields
                    :disjoint-fields))
        p (count (filter #(= % :disjoint-fields) results))
        q (- (count results) p)]
    (if (> p q)
      (- p q)
      0)))

(defn classes-by-lack-of-cohesion-in-methods
  [g]
  (apply-metric g lack-of-cohesion))

(defn classes-by-lack-of-cohesion-in-methods-forkjoin
  [g]
  (apply-metric-forkjoin g lack-of-cohesion))


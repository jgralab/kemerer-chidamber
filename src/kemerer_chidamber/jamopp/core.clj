(ns kemerer-chidamber.jamopp.core
  (:use funnyqt.generic)
  (:use funnyqt.utils)
  (:use funnyqt.emf.core)
  (:use funnyqt.emf.query))

;;* Convenience

(defn concrete-classifier-by-name
  "Returns the Type with the given (fully qualified) name n."
  [m n]
  (let [nm (name n)
        cun (str nm ".java")]
    (if-let [cu (first (filter #(= (eget % :name) cun)
                               (eallcontents m 'CompilationUnit)))]
      (let [sn (last (clojure.string/split nm #"\."))]
        (the #(= sn (eget % :name))
             (eget cu :classifiers)))
      (if-let [cs (seq (filter #(= (eget % :name) nm)
                               (eallcontents m 'ConcreteClassifier)))]
        (cond
         (next cs)   (error (format "%s is ambiguous." nm))
         :else       (first cs))
        (error (format "No such ConcreteClassifier %s." nm))))))


;;* Metrics

;;** Chidamber & Kemerer

(defn class-qname
  [c]
  (eget (econtainer c) :name))

(def jgralab-classes
  (memoize
   (fn [m]
     (filter (fn [c]
               (re-matches #"de\.uni_koblenz\.jgralab\..*"
                           (class-qname c)))
             (eallcontents m 'Class)))))

(defn apply-metric
  "Applies the given metric to all JGraLab classes."
  [m metric]
  (sort
   (seq-compare (constantly 0) #(- %2 %1) compare)
   (for [c (jgralab-classes m)]
     [c (metric c) (class-qname c)])))

(def ^java.util.concurrent.ForkJoinPool
  fj-pool (java.util.concurrent.ForkJoinPool.))

(defn apply-metric-forkjoin
  "Applies the given metric to all JGraLab classes in parallel using a
  ForkJoinPool."
  [m metric]
  (sort
   (seq-compare (constantly 0) #(- %2 %1) compare)
   (let [res (doall (map (fn [c]
                           (let [^java.util.concurrent.Callable f
                                 (fn []
                                   [c (metric c) (class-qname c)])]
                             (.submit fj-pool f)))
                         (jgralab-classes m)))]
     (map #(.get ^java.util.concurrent.ForkJoinTask %) res))))

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

;; (defn coupled-classes
;;   "Given a Class `c', calculates all coupled classes."
;;   [c]
;;   (reachables c
;;     [p-seq [<>-- 'IsClassBlockOf] [<>-- 'IsMemberOf]
;;            [<-- ['IsBodyOfMethod 'IsFieldCreationOf]]
;;            [p-* [<-- 'IsStatementOf]]
;;            [p-alt
;;              ;; Classes whose methods are called by c
;;              [<-- 'IsDeclarationOfInvokedMethod]
;;              ;; Classes whose Fields are accessed by c
;;              [p-seq [<-- 'IsDeclarationOfAccessedField] [--> 'IsFieldCreationOf]]]
;;            [--<> 'IsMemberOf] [--<> 'IsClassBlockOf]
;;            [p-restr nil #(not (= c %1))]]))

;; (defn classes-by-coupling-between-objects
;;   [g]
;;   (apply-metric g #(count (coupled-classes %))))

;; (defn classes-by-coupling-between-objects-parallel
;;   [g]
;;   (apply-metric-parallel g #(count (coupled-classes %))))

;; (defn classes-by-coupling-between-objects-forkjoin
;;   [g]
;;   (apply-metric-forkjoin g #(count (coupled-classes %))))

;;*** Weighted Methods per Class

;; (defn cyclomatic-complexity
;;   "Returns the cyclomatic complexity of the given method."
;;   [m]
;;   (-> (reachables
;;         m [p-seq [<>-- 'IsBodyOfMethod]
;;                  [p-* [<>-- 'IsStatementOf]]
;;                  [p-restr '[If Case Default For DoWhile While]]])
;;       count
;;       inc))

;; (defn weighted-method-per-class
;;   "Returns the WMC metric for the given class c."
;;   [c]
;;   (reduce + (map cyclomatic-complexity
;;                  (reachables c [p-seq [<>-- 'IsClassBlockOf]
;;                                       [<>-- 'IsMemberOf]
;;                                       [p-restr 'MethodDefinition]]))))

;; (defn classes-by-weighted-methods-per-class
;;   [g]
;;   (apply-metric g weighted-method-per-class))

;; (defn classes-by-weighted-methods-per-class-parallel
;;   [g]
;;   (apply-metric-parallel g weighted-method-per-class))

;; (defn classes-by-weighted-methods-per-class-forkjoin
;;   [g]
;;   (apply-metric-forkjoin g weighted-method-per-class))

;;*** Number of Children

(defn subtypes
  "Returns all direct subtypes of the given type t that are contained in
  classifiers."
  [classes t]
  ;; Well, this is pretty slow, because all the needed references are
  ;; unidirectional.  You can get the superclass quickly, but getting
  ;; subclasses is hardly possible.
  (mapcat (fn [c]
            (let [supers (seq (reachables c [p-seq :extends
                                             :classifierReferences
                                             :target]))]
              (when (member? t supers)
                [c])))
          classes))

(defn classes-by-number-of-children
  [m]
  (let [jg-classes (jgralab-classes m)]
    (apply-metric m #(count (subtypes jg-classes %)))))

(defn classes-by-number-of-children-forkjoin
  [m]
  (let [jg-classes (jgralab-classes m)]
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
    (clojure.set/union own-methods called-methods)))

(defn classes-by-response-for-a-class
  [g]
  (apply-metric g #(count (response-set %))))

(defn classes-by-response-for-a-class-forkjoin
  [g]
  (apply-metric-forkjoin g #(count (response-set %))))


;;*** Lack of Cohesion in Methods

;; (defn lack-of-cohesion
;;   "Returns the lack of cohesion metric value of t."
;;   [t]
;;   (let [fields (reachables t [p-seq [<>-- 'IsClassBlockOf]
;;                                     [<>-- 'IsMemberOf]
;;                                     [p-restr 'Field]])
;;         methods (reachables t [p-seq [<>-- 'IsClassBlockOf]
;;                                      [<>-- 'IsMemberOf]
;;                                      [p-restr 'MethodDefinition]])
;;         accessed-fields (fn [m]
;;                           (reachables m [p-seq [<>-- 'IsBodyOfMethod]
;;                                                [p-* [<-- 'IsStatementOf]]
;;                                                [<-- 'IsDeclarationOfAccessedField]
;;                                                [p-restr nil #(member? % fields)]]))
;;         method-field-map (apply hash-map (mapcat (fn [m] [m (accessed-fields m)])
;;                                                  methods))
;;         combinations (loop [ms methods, pairs []]
;;                        (if (next ms)
;;                          (recur (rest ms) (concat pairs
;;                                                   (map (fn [n] [(first ms) n])
;;                                                        (rest ms))))
;;                          pairs))
;;         results (for [[m1 m2] combinations
;;                       :let [f1 (method-field-map m1)
;;                             f2 (method-field-map m2)]]
;;                   (if (seq (clojure.set/intersection f1 f2))
;;                     :common-fields
;;                     :disjoint-fields))
;;         p (count (filter #(= % :disjoint-fields) results))
;;         q (- (count results) p)]
;;     (if (> p q)
;;       (- p q)
;;       0)))

;; (defn classes-by-lack-of-cohesion-in-methods
;;   [g]
;;   (apply-metric g lack-of-cohesion))

;; (defn classes-by-lack-of-cohesion-in-methods-parallel
;;   [g]
;;   (apply-metric-parallel g lack-of-cohesion))

;; (defn classes-by-lack-of-cohesion-in-methods-forkjoin
;;   [g]
;;   (apply-metric-forkjoin g lack-of-cohesion))


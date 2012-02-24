(ns kemerer-chidamber.grabaja.core
  (:use funnyqt.generic)
  (:require [funnyqt.utils :as u])
  (:use funnyqt.tg.core)
  (:use funnyqt.tg.query))

;;* Convenience

(defn package-by-fqn
  "Returns the JavaPackage with the given fully qualified name."
  [g fqn]
  (the (filter #(= (value % :fullyQualifiedName)
                   (name fqn))
               (vseq g 'JavaPackage))))

(defn type-by-name
  "Returns the Type with the given (fully qualified) name n."
  [g n]
  (the (filter #(or (= (value % :fullyQualifiedName) (name n))
                    (= (value % :name) (name n)))
               (vseq g 'Type))))

(defn type-hierarchy
  "Returns a map of t's type hierarchy."
  [t]
  (when t
    (let [super (first (reachables t [p-seq [<-- 'IsSuperClassOf]
                                      [<-- 'IsTypeDefinitionOf]]))
          ifaces (reachables t [p-seq [<-- 'IsInterfaceOf]
                                [<-- 'IsTypeDefinitionOf]])]
      {(value t :fullyQualifiedName)
       {:super    (type-hierarchy super)
        :ifaces   (map type-hierarchy ifaces)}})))

(defn containment-hierarchy
  "Returns a map of pkg's contents."
  [pkg]
  (let [subs (adjs pkg :subPackages)
        types (reachables pkg
                          [p-seq [<*>-- 'IsPartOf]
                           [<-- 'IsSourceUsageIn]
                           [<-- 'IsExternalDeclarationIn]
                           [p-restr 'Type]])
        classes (p-restr types 'ClassDefinition)
        ifaces  (p-restr types 'InterfaceDefinition)
        enums   (p-restr types 'EnumDefinition)
        annos   (p-restr types 'AnnotationDefinition)]
    {(value pkg :fullyQualifiedName)
     {:classes  (map #(value % :name) classes)
      :ifaces   (map #(value % :name) ifaces)
      :enums    (map #(value % :name) enums)
      :annos    (map #(value % :name) annos)
      :packages (map containment-hierarchy subs)}}))


;;* Metrics

;;** Quantity Metrics

(defn members-of-type
  "Returns the members of type t."
  [t]
  (reachables t [p-seq [<*>-- 'IsBlockOf] :members]))

(defn types-by-size
  [g]
  (sort (seq-compare (constantly 0) #(- %2 %1) #(- %2 %1) compare)
        (for [t (vseq g '[ClassDefinition InterfaceDefinition
                          EnumDefinition AnnotationDefinition])
              :let [members (members-of-type t)]]
          [t
           (count (p-restr members 'Field))
           (count (p-restr members 'MethodDeclaration))
           (value t :fullyQualifiedName)])))

;;** Chidamber & Kemerer

(def ^{:dynamic true
       :doc "A function that should return all classes for which the metrics
  should be calculated.  Defaults to all classes."}
  *get-classes-fn*
  (fn [g]
    (vseq g 'ClassDefinition)))

(defn apply-metric
  "Applies the given metric to all JGraLab classes."
  [g metric]
  (sort
   (seq-compare (constantly 0) #(- %2 %1) compare)
   (for [c (*get-classes-fn* g)]
     [c (metric c) (value c :fullyQualifiedName)])))

(u/compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
              (def ^java.util.concurrent.ForkJoinPool
                fj-pool (java.util.concurrent.ForkJoinPool.))
              nil)

(defn apply-metric-forkjoin
  "Applies the given metric to all JGraLab classes in parallel using a
  ForkJoinPool."
  [g metric]
  (u/compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
                (sort
                 (seq-compare (constantly 0) #(- %2 %1) compare)
                 (let [res (doall (map (fn [c]
                                         (let [^java.util.concurrent.Callable f
                                               (fn []
                                                 [c (metric c) (value c :fullyQualifiedName)])]
                                           (.submit fj-pool f)))
                                       (*get-classes-fn* g)))]
                   (map #(.get ^java.util.concurrent.ForkJoinTask %) res)))
                (do (println "Sorry, ForkJoin application disabled.  Get a JDK7."
                             "Right now, we are simply falling back to sequential application.")
                    (apply-metric g metric))))

;;*** Depth of Inheritance Tree

(defn depth-of-inheritance-tree
  "Returns the depth of the inheritance tree of Type t."
  [t]
  (let [supers (reachables t [p-seq [<-- 'IsSuperClassOf]
                                    [<-- 'IsTypeDefinitionOf]])]
    (cond
     (seq supers) (inc (apply max (map depth-of-inheritance-tree supers)))
     (= (value t :fullyQualifiedName) "java.lang.Object") 0
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
    [p-seq [<>-- 'IsClassBlockOf] [<>-- 'IsMemberOf]
           [<-- ['IsBodyOfMethod 'IsFieldCreationOf]]
           [p-* [<>-- 'IsStatementOf]]
           [p-alt
             ;; Classes whose methods are called by c
             [<-- 'IsDeclarationOfInvokedMethod]
             ;; Classes whose Fields are accessed by c
             [p-seq [<-- 'IsDeclarationOfAccessedField] [--> 'IsFieldCreationOf]]]
           [--<> 'IsMemberOf] [--<> 'IsClassBlockOf]
           [p-restr nil #(not (= c %1))]]))

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
        m [p-seq [<>-- 'IsBodyOfMethod]
                 [p-* [<>-- 'IsStatementOf]]
                 [p-restr '[If Case Default For DoWhile While]]])
      count
      inc))

(defn weighted-method-per-class
  "Returns the WMC metric for the given class c."
  [c]
  (reduce + (map cyclomatic-complexity
                 (reachables c [p-seq [<>-- 'IsClassBlockOf]
                                      [<>-- 'IsMemberOf]
                                      [p-restr 'MethodDefinition]]))))

(defn classes-by-weighted-methods-per-class
  [g]
  (apply-metric g weighted-method-per-class))

(defn classes-by-weighted-methods-per-class-forkjoin
  [g]
  (apply-metric-forkjoin g weighted-method-per-class))

;;*** Number of Children

(defn subtypes
  "Returns all direct subtypes of the given type t."
  [t]
  (reachables t [p-seq [--> 'IsTypeDefinitionOf]
                       [--> 'IsSuperClassOf]]))

(defn classes-by-number-of-children
  [g]
  (apply-metric g #(count (subtypes %))))

(defn classes-by-number-of-children-forkjoin
  [g]
  (apply-metric-forkjoin g #(count (subtypes %))))

;;*** Response for a Class

(defn response-set
  "Returns the response set of the given type t."
  [t]
  (let [own-methods (reachables t [p-seq [<>-- 'IsClassBlockOf]
                                         [<>-- 'IsMemberOf]
                                         [p-restr 'MethodDeclaration]])
        called-methods (set (mapcat
                             #(reachables % [p-seq [<>-- 'IsBodyOfMethod]
                                                   [<>-- 'IsStatementOf]
                                                   [<-- 'IsDeclarationOfInvokedMethod]])
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
  (let [fields (reachables t [p-seq [<>-- 'IsClassBlockOf]
                              [<>-- 'IsMemberOf]
                              [p-restr 'Field]])
        methods (reachables t [p-seq [<>-- 'IsClassBlockOf]
                               [<>-- 'IsMemberOf]
                               [p-restr 'MethodDefinition]])
        accessed-fields (fn [m]
                          (reachables m [p-seq [<>-- 'IsBodyOfMethod]
                                         [p-* [<>-- 'IsStatementOf]]
                                         [<-- 'IsDeclarationOfAccessedField]
                                         [--> 'IsFieldCreationOf]
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


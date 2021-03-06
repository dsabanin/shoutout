(ns shoutout-test
  (:require [clojure.test :refer :all]
            [shoutout :refer :all]))

(deftest parse-feature-test
  (testing "groups"
    (testing "an empty feature has no groups"
      (is (= (:groups (parse-feature "an-feature" "||"))
             #{})))

    (testing "an feature can have an group"
      (is (= (:groups (parse-feature "an-feature" "||admin"))
             #{"admin"})))

    (testing "an feature can have an groups"
      (is (= (:groups (parse-feature "an-feature" "||admin,client"))
             #{"admin" "client"}))))

  (testing "users"
    (testing "an empty feature has no users"
      (is (= (:users (parse-feature "an-feature" "||"))
             #{})))

    (testing "an feature can have an user"
      (is (= (:users (parse-feature "an-feature" "|1|"))
             #{"1"})))

    (testing "an feature can have an users"
      (is (= (:users (parse-feature "an-feature" "|1,2,3|"))
             #{"1" "2" "3"}))))

  (testing "percentage"
    (testing "an empty feature has a percentage of 0"
      (is (= (:percentage (parse-feature "an-feature" "||"))
             0)))

    (testing "an feature can have an percentage"
      (is (= (:percentage (parse-feature "an-feature" "89||"))
             89)))))

(deftest serializing-features-test
  (testing "groups"
    (testing "empty groups gets serialized to empty"
      (is (= (serialize-feature (->Feature "an-feature" [] [] 0))
             "0||")))
    (testing "can serialize a single group"
      (is (= (serialize-feature (->Feature "an-feature" ["admin"] [] 0))
             "0||admin")))

    (testing "can serialize an groups"
      (is (= (serialize-feature (->Feature "an-feature" ["admin" "agent"] [] 0))
             "0||admin,agent"))))

  (testing "users"
    (testing "empty users gets serialized to empty"
      (is (= (serialize-feature (->Feature "an-feature" [] [] 0))
             "0||")))
    (testing "can serialize a single user"
      (is (= (serialize-feature (->Feature "an-feature" [] ["1"] 0))
             "0|1|")))

    (testing "can serialize many users"
      (is (= (serialize-feature (->Feature "an-feature" [] ["1" "2" "3"] 0))
             "0|1,2,3|"))))

  (testing "serializing percentage"
    (is (= (serialize-feature (->Feature "an-feature" [] [] 89))
           "89||"))))

(deftest active-test
  (testing "an individual user is active"
    (is (active-feature? (->Feature "an-feature" #{} #{"1"} 0)
                         {}
                         "1")))

  (testing "an user is active in an active group"
    (is (active-feature? (->Feature "an-feature" #{"admin"} #{} 0)
                         {"admin" (constantly true)}
                         "1")))

  (testing "an user is inactive in an inactive group"
    (is (not (active-feature? (->Feature "an-feature" #{} #{} 0)
                              {"admin" (constantly true)}
                              "1"))))

  (testing "an user is inactive because they are not in an active group"
    (is (not (active-feature? (->Feature "an-feature" #{"admin"} #{} 0)
                              {"admin" (constantly false)}
                              "1"))))

  (testing "without any groups, users or percentage active, a feature is inactive"
    (is (not (active-feature? (->Feature "an-feature" #{} #{} 0)
                              {}
                              "1"))))

  (testing "an user is active when percentage is set to 100"
    (is (active-feature? (->Feature "an-feature" #{} #{} 100)
                         {}
                         "1"))))

(deftest shell-test
  (testing "after activating a group a user is in, the user is active"
    (let [flags (shoutout (in-memory-store) {"admin" #(= % "admin")})]
      (activate-group flags "chat" "admin")
      (is (active? flags "chat" "admin"))))

  (testing "after deactivating a group a user is in, the user is not active"
    (let [flags (shoutout (in-memory-store) {"admin" #(= % "admin")})]
      (activate-group flags "chat" "admin")
      (deactivate-group flags "chat" "admin")
      (is (not (active? flags "chat" "admin")))))

  (testing "after activating a specific user, that user is active"
    (let [flags (shoutout (in-memory-store))]
      (activate-user flags "chat" "5")
      (is (active? flags "chat" "5"))))

  (testing "after deactivating a specific user, that user is active"
    (let [flags (shoutout (in-memory-store))]
      (activate-user flags "chat" "5")
      (deactivate-user flags "chat" "5")
      (is (not (active? flags "chat" "5")))))

  (testing "if a feature hasn't been touched, users are inactive"
    (let [flags (shoutout (in-memory-store) {"admin" #(do (println "lol") (= % "admin"))})]
      (is (not (active? flags "chat" "admin"))))
    ))

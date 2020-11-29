(ns nl.epij.clojournal-recipe.option-vesting
  (:require [clojure.edn :as edn]
            [nl.epij.clojournal-recipe.option-vesting.grant :as grant]
            [nl.epij.clojournal-recipe.option-vesting.period :as period]
            [com.clojournal.alpha :as clojournal]
            [com.clojournal.alpha.api :as c.api]
            [com.clojournal.alpha.virtual :as virtual])
  (:import (java.time Period LocalDate)))

(defn- periods->grants
  [xs]
  (map (fn [[{::grant/keys [id price cliff issued-by underlying-asset]} xs]]
         {::grant/id               id
          ::grant/price            price
          ::grant/size             (apply + (map ::period/options-vested xs))
          ::grant/vesting-start    (.toString (.minus (LocalDate/parse (first (map ::period/date xs))) (Period/parse cliff)))
          ::grant/vesting-end      (last (map ::period/date xs))
          ::grant/issued-by        issued-by
          ::grant/underlying-asset underlying-asset})
       (group-by ::period/grant xs)))

(defn- grants->grant-transactions
  [xs]
  (for [grant xs
        :let [{::grant/keys [id size vesting-start issued-by underlying-asset]} grant]]
    {::clojournal/date           vesting-start
     ::clojournal/transaction-id id
     ::clojournal/payee          issued-by
     ::clojournal/status         ::clojournal/cleared
     ::clojournal/postings       [{::clojournal/account (format "Off-Balance:Unvested Options:%s" id)
                                   ::clojournal/virtual ::virtual/unbalanced
                                   ::clojournal/amount  (format "%s %s" underlying-asset size)}
                                  {::clojournal/account (format "Off-Balance:Unexercised Options:%s" id)
                                   ::clojournal/virtual ::virtual/unbalanced
                                   ::clojournal/amount  (format "%s %s" underlying-asset size)}
                                  ;; FIXME: this is to prevent a bug in ledger CLI where, when using `print`,
                                  ;; it omits the amount of the second virtual transaction above
                                  ;; Reported: https://github.com/ledger/ledger/issues/1974
                                  {::clojournal/account "Bugbuster:One"
                                   ::clojournal/amount  "1"}
                                  {::clojournal/account "Bugbuster:Two"}]}))

(defn- periods->grant-transactions
  [xs]
  (grants->grant-transactions (periods->grants xs)))

(defn- ->status
  [x]
  (get {::period/exercised ::clojournal/cleared
        ::period/vested    ::clojournal/cleared}
       x))

(defn- vest-transactions
  [periods]
  (for [period periods
        :let [{::period/keys [grant date options-vested status]} period
              {::grant/keys [price id issued-by underlying-asset]} grant]]
    {::clojournal/date           date
     ::clojournal/payee          issued-by
     ::clojournal/status         (->status status)
     ::clojournal/postings       (concat
                                   [{::clojournal/account (format "Off-Balance:Unvested Options:%s" id)
                                     ::clojournal/virtual ::virtual/unbalanced
                                     ::clojournal/amount  (format "%s -%s" underlying-asset options-vested)}

                                    {::clojournal/account "Assets:Options"
                                     ::clojournal/amount  (format "%s \"%s\" {%s}" options-vested id price)}
                                    {::clojournal/account "Income"
                                     ::clojournal/amount  (format "-%s \"%s\" {%s}" options-vested id price)}

                                    {::clojournal/account "Unrealized:Equity:Capital Requirement"
                                     ::clojournal/virtual ::virtual/balanced
                                     ::clojournal/amount  (format "%s \"%s\" @ %s" options-vested id price)}
                                    {::clojournal/account "Unrealized:Liabilities:Exercise Provision"
                                     ::clojournal/virtual ::virtual/balanced
                                     ::clojournal/amount  (format "-%s \"%s\" @ %s" options-vested id price)}

                                    {::clojournal/account (str "Unrealized:Assets:" id)
                                     ::clojournal/virtual ::virtual/balanced
                                     ::clojournal/amount  (format "%s %s" underlying-asset options-vested)}
                                    {::clojournal/account (str "Equity:Capital Gains Provision:" id)
                                     ::clojournal/virtual ::virtual/balanced
                                     ::clojournal/amount  (format "%s -%s" underlying-asset options-vested)}])
     ::clojournal/transaction-id (::period/id period)}))

(defn vesting-schedule->journal
  [xs]
  (let [periods xs]
    (->> periods
         vest-transactions
         (concat (periods->grant-transactions periods))
         (sort-by (juxt ::clojournal/date ::clojournal/account)))))

(defn -main
  "Utility main method to make direct invocation through babashka possible. This is useful if you want to pipe into
  ledger CLI directly."
  [in out]
  (spit out (c.api/journal (vesting-schedule->journal (edn/read-string (slurp in))))))

(comment

  )

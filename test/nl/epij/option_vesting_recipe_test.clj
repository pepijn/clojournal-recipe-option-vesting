(ns nl.epij.option-vesting-recipe-test
  (:require [clojure.test :refer :all]
            [nl.epij.option-vesting-recipe :refer [vesting-schedule->journal]]
            [nl.epij.option-vesting-recipe.grant :as grant]
            [nl.epij.option-vesting-recipe.period :as period]
            [com.clojournal.alpha :as clojournal]
            [com.clojournal.alpha.virtual :as virtual]))

(deftest journal-generation
  (is (= (vesting-schedule->journal [{::period/grant          {::grant/id               "PL-1337"
                                                               ::grant/price            "R$ 1.5608"
                                                               ::grant/cliff            "P1Y"
                                                               ::grant/issued-by        "Example Inc."
                                                               ::grant/underlying-asset "EXPL"}
                                      ::period/number         "1"
                                      ::period/status         ::period/exercised
                                      ::period/id             "PL-1337/1"
                                      ::period/date           "2017-12-15"
                                      ::period/options-vested 5000}])
         [{::clojournal/date           "2016-12-15"
           ::clojournal/transaction-id "PL-1337"
           ::clojournal/payee          "Example Inc."
           ::clojournal/status         ::clojournal/cleared
           ::clojournal/postings       [{::clojournal/account "Off-Balance:Unvested Options:PL-1337"
                                         ::clojournal/virtual ::virtual/unbalanced
                                         ::clojournal/amount  "EXPL 5000"}
                                        {::clojournal/account "Off-Balance:Unexercised Options:PL-1337"
                                         ::clojournal/virtual ::virtual/unbalanced
                                         ::clojournal/amount  "EXPL 5000"}]}
          {::clojournal/date           "2017-12-15"
           ::clojournal/payee          "Example Inc."
           ::clojournal/status         ::clojournal/cleared
           ::clojournal/postings       [{::clojournal/account "Off-Balance:Unvested Options:PL-1337"
                                         ::clojournal/virtual ::virtual/unbalanced
                                         ::clojournal/amount  "EXPL -5000"}
                                        {::clojournal/account "Assets:Options"
                                         ::clojournal/amount  "5000 \"PL-1337\" {R$ 1.5608}"}
                                        {::clojournal/account "Income"
                                         ::clojournal/amount  "-5000 \"PL-1337\" {R$ 1.5608}"}
                                        {::clojournal/account "Unrealized:Equity:Capital Requirement"
                                         ::clojournal/virtual ::virtual/balanced
                                         ::clojournal/amount  "5000 \"PL-1337\" @ R$ 1.5608"}
                                        {::clojournal/account "Unrealized:Liabilities:Exercise Provision"
                                         ::clojournal/virtual ::virtual/balanced
                                         ::clojournal/amount  "-5000 \"PL-1337\" @ R$ 1.5608"}
                                        {::clojournal/account "Unrealized:Assets:PL-1337"
                                         ::clojournal/virtual ::virtual/balanced
                                         ::clojournal/amount  "EXPL 5000"}
                                        {::clojournal/account "Equity:Capital Gains Provision:PL-1337"
                                         ::clojournal/virtual ::virtual/balanced
                                         ::clojournal/amount  "EXPL -5000"}]
           ::clojournal/transaction-id "PL-1337/1"}])))

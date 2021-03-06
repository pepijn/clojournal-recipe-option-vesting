(ns simple-example
  (:require [nl.epij.clojournal-recipe.option-vesting :refer [vesting-schedule->journal]]
            [nl.epij.clojournal-recipe.option-vesting.grant :as grant]
            [nl.epij.clojournal-recipe.option-vesting.period :as period]
            [com.clojournal.alpha.api :as c.api]))

(def vesting-schedule
  "Transform your vesting schedule (for example, from Carta) into a data structure like this one"
  [{::period/grant          {::grant/id               "PL-1337"
                             ::grant/price            "R$ 3.42"
                             ::grant/cliff            "P1Y" ;; 1 year
                             ::grant/issued-by        "Example Inc."
                             ::grant/underlying-asset "EXPL"}
    ::period/number         "1"
    ::period/status         ::period/exercised
    ::period/id             "PL-1337/1"
    ::period/date           "2017-12-15"
    ::period/options-vested 5000}

   {::period/grant          {::grant/id               "PL-1337"
                             ::grant/price            "R$ 3.42"
                             ::grant/cliff            "P1Y"
                             ::grant/issued-by        "Example Inc."
                             ::grant/underlying-asset "EXPL"}
    ::period/number         "2"
    ::period/status         ::period/exercised
    ::period/id             "PL-1337/2"
    ::period/date           "2017-12-17"
    ::period/options-vested 25}])

(-> vesting-schedule
    vesting-schedule->journal
    c.api/journal
    println)

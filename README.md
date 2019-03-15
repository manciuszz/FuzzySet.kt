fuzzyset.kt - A fuzzy string set for Kotlin.
=============================================

fuzzyset is a data structure that performs something akin to fulltext search
against data to determine likely mispellings and approximate string matching.
Note that this is a **Kotlin** port of a [javascript library](https://github.com/Glench/fuzzyset.js) (that was ported over from [python library](https://github.com/axiak/fuzzyset))

Usage
-----

The usage is simple. Just add a string to the set, and ask for it later by using ``.get``:
```kotlin
   val a = FuzzySet()
   a.add("michael axiak")
   a.get("micael asiak") // will be [Item(0.8461538461538461, 'michael axiak')]
   // OR
   val b = FuzzySet(arrayOf("michael axiak"))
   b.get("micael asiak") // will be [Item(0.8461538461538461, 'michael axiak')]
```
The result will be an array of ``Item(score, matched_value)`` objects.
The score is between 0 and 1, with 1 being a perfect match.

Construction Arguments
----------------------

 - `array`: An array of strings to initialize the data structure with
 - `useLevenshtein`: Whether or not to use the levenshtein distance to determine the match scoring. Default: True
 - `gramSizeLower`: The lower bound of gram sizes to use, inclusive. Default: 2
 - `gramSizeUpper`: The upper bound of gram sizes to use, inclusive. Default: 3

Methods
-------

 - `get(value, [default], [minScore=.33])`: try to match a string to entries with a score of at least minScore (defaulted to .33), otherwise return `null` or `default` if it is given.
 - `add(value)`: add a value to the set returning `false` if it is already in the set.
 - `length()`: return the number of items in the set.
 - `isEmpty()`: returns true if the set is empty.
 - `values()`: returns an array of the values in the set.

Interactive Representation
-------------------
There is an EXCELLENT documentation already made by the [author](https://github.com/Glench) of the Javascript library [here](http://glench.github.io/fuzzyset.js/ui/).

License
-------

BSD

Python Author
--------

Mike Axiak <mike@axiak.net>


JavaScript Port Author
--------

Glen Chiacchieri (http://glench.com)

Kotlin Port Author
--------
Manciuszz

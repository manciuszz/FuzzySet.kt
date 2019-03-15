class FuzzySet(arr: Array<String> = emptyArray(), val useLevenshtein: Boolean = true, val gramSizeLower: Int = 2, val gramSizeUpper: Int = 3) {

    private val _nonWordRe = Regex("/[^a-zA-Z0-9\\u00C0-\\u00FF, ]+/g")
    private val exactSet = mutableMapOf<String, String>()
    private val matchDict = mutableMapOf<String, ArrayList<Array<Int>>>()
    private val items = mutableMapOf<Int, MutableMap<Int, Item>>()

    public data class Item(
        var score: Double = 0.0,
        var str: String = ""
    )

    private fun levenshtein(s: String, t: String, charScore : (Char, Char) -> Int = { c1, c2 -> if (c1 == c2) 0 else 1}) : Int {
        if (s == t)  return 0
        if (s == "") return t.length
        if (t == "") return s.length

        val initialRow : List<Int> = (0 until t.length + 1).map { it }.toList()
        return (0 until s.length).fold(initialRow) {
            previous, u ->
            (0 until t.length).fold( mutableListOf(u+1)) {
                row, v ->
                row.add(listOf(row.last() + 1,
                        previous[v+1] + 1,
                        previous[v] + charScore(s[u],t[v])).min()!!)
                row
            }
        }.last()
    }

    private fun _distance(str1: String, str2: String): Double {
        val distance = levenshtein(str1, str2).toDouble()
        return if (str1.length > str2.length) {
            1.0 - distance / str1.length
        } else {
            1.0 - distance / str2.length
        }
    }

    private fun _iterateGrams(value: String, gramSize: Int = 2): MutableList<String> {
        var simplified = "-" + value.toLowerCase().replace(_nonWordRe, "") + "-"
        val lenDiff: Int = gramSize - simplified.length
        val results = mutableListOf<String>()

        if (lenDiff > 0) {
            for(i in 0 until lenDiff) {
                simplified += '-'
            }
        }

        for (i in 0 until (simplified.length - gramSize + 1)) {
            results.add(simplified.substring(i, i + gramSize))
        }
        return results;
    }

    private fun _gramCounter(value: String, gramSize: Int = 2): MutableMap<String, Int> {
        val result = mutableMapOf<String, Int>()
        val grams = _iterateGrams(value, gramSize)

        for (i in 0 until grams.size) {
            if (grams[i] in result) {
                result[grams[i]] = result[grams[i]]?.plus(1) ?: 0
            } else {
                result[grams[i]] = 1
            }
        }
        return result
    }

    fun get(value: String, defaultValue: ArrayList<Item> = arrayListOf(), minMatchScore: Double = .33): ArrayList<Item> {
        return this._get(value, minMatchScore) ?: defaultValue
    }

    private fun _get(value: String, minMatchScore: Double): ArrayList<Item>? {
        val normalizedValue = this._normalizeStr(value)
        val result = this.exactSet[normalizedValue]
        if (result != null) {
            return arrayListOf(Item(1.0, result))
        }

        for (gramSize in gramSizeUpper downTo gramSizeLower) {
            val results = this.__get(value, gramSize, minMatchScore);
            if (results.isNotEmpty()) {
                return results
            }
        }
        return null
    }

    private fun __get(value: String, gramSize: Int, minMatchScore: Double): ArrayList<Item> {
        val normalizedValue = this._normalizeStr(value)
        val gramCounts = _gramCounter(normalizedValue, gramSize)

        val matches = mutableMapOf<Int, Int>()

        var sumOfSquareGramCounts: Double = 0.0
        gramCounts.forEach {
            gram, gramCount ->
               sumOfSquareGramCounts += Math.pow(gramCount.toDouble(), 2.0)
               if (gram in this.matchDict) {
                   val match = this.matchDict[gram]!!
                   for (i in 0 until match.size) {
                       val index = match[i][0]
                       val otherGramCount = match[i][1]
                       if (index !in matches) {
                           matches[index] = gramCount * otherGramCount
                       } else {
                           matches[index] = matches[index]!!.plus(gramCount * otherGramCount)
                       }
                   }
               }
        }

        if (matches.isEmpty()) {
            return arrayListOf()
        }

        val vectorNormal = Math.sqrt(sumOfSquareGramCounts)
        var results = arrayListOf<Item>()

        for ((matchIndex, matchScore) in matches) {
            val matchedItem: Item = this.items[gramSizeLower]!![matchIndex]!!
            results.add(Item(matchScore / (vectorNormal * matchedItem.score), matchedItem.str))
        }

        results.sortByDescending {
            it.score
        }

        if (this.useLevenshtein) {
            val newResults = arrayListOf<Item>()
            val endIndex = Math.min(50, results.size)
            for (i in 0 until endIndex) {
                newResults.add(Item(_distance(results[i].str, normalizedValue), results[i].str))
            }
            results = newResults
            results.sortByDescending {
                it.score
            }
        }

        val newResults = arrayListOf<Item>()
        results.forEach {
            scoreWordPair ->
            if (scoreWordPair.score >= minMatchScore) {
                newResults.add(Item(scoreWordPair.score, this.exactSet[scoreWordPair.str]!!))
            }
        }
        return newResults
    }

    fun add(value: String): Boolean {
        val normalizedValue = this._normalizeStr(value)
        if (normalizedValue in this.exactSet) {
            return false
        }

        for (i in this.gramSizeLower .. this.gramSizeUpper + 1)
            this._add(value, i)
        return true
    }

    private fun _add(value: String, gramSize: Int) {
        val normalizedValue = this._normalizeStr(value)
        val items = this.items[gramSize] ?: mutableMapOf()
        val index = items.size

        val gramCounts = _gramCounter(normalizedValue, gramSize)

        var sumOfSquareGramCounts: Double = 0.0
        gramCounts.forEach {
            gram, gramCount ->
            sumOfSquareGramCounts += Math.pow(gramCount.toDouble(), 2.0)
            if (gram in this.matchDict) {
                this.matchDict[gram]!!.add(arrayOf(index, gramCount))
            } else {
                this.matchDict[gram] = arrayListOf(arrayOf(index, gramCount))
            }
        }

        val vectorNormal = Math.sqrt(sumOfSquareGramCounts)
        items[index] = Item(vectorNormal, normalizedValue)
        this.items[gramSize] = items
        this.exactSet[normalizedValue] = value
    }

    private fun _normalizeStr(str: String): String {
        return str.toLowerCase()
    }

    fun length(): Int {
        return this.exactSet.size
    }

    fun isEmpty(): Boolean {
        return this.exactSet.isEmpty()
    }

    fun values(): MutableList<String> {
        val values = mutableListOf<String>()
        for ((normalizedValue, Value) in this.exactSet) {
            values.add(Value)
        }
        return values
    }

    init {
        for (i in gramSizeLower until gramSizeUpper + 1) {
            this.items[i] = mutableMapOf()
        }

        for (obj in arr) {
            this.add(obj)
        }
    }
}
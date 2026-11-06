package com.waypoint.app

/** Validate strict JSON and bound nesting before the platform's lenient recursive parser. */
internal object JsonSafety {
    fun checkDepth(text: String, maxDepth: Int = 64, maxValues: Int = 250_000) {
        require(maxDepth in 1..64)
        require(maxValues > 0)
        Reader(text, maxDepth, maxValues).read()
    }

    private class Reader(val text: String, val maxDepth: Int, val maxValues: Int) {
        var index = 0
        var values = 0
        fun read() { value(0); whitespace(); require(index == text.length) { "Invalid service response." } }
        fun whitespace() { while (index < text.length && text[index] in " \t\r\n") index++ }
        fun take(c: Char): Boolean { whitespace(); return if (index < text.length && text[index] == c) { index++; true } else false }
        fun expect(c: Char) { require(take(c)) { "Invalid service response." } }
        fun value(depth: Int) {
            require(++values <= maxValues) { "The service response is too complex." }
            whitespace()
            require(index < text.length) { "Incomplete service response." }
            when (text[index]) {
                '{', '[' -> {
                    require(depth < maxDepth) { "The service response is too complex." }
                    val objectValue = text[index++] == '{'
                    val closing = if (objectValue) '}' else ']'
                    if (take(closing)) return
                    do {
                        if (objectValue) { whitespace(); string(); expect(':') }
                        value(depth + 1)
                    } while (take(','))
                    expect(closing)
                }
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> {
                    val start = index
                    while (index < text.length && text[index] in "-+0123456789.eE") index++
                    require(number.matches(text.substring(start,index))) { "Invalid service response." }
                }
            }
        }
        fun literal(value: String) { require(text.startsWith(value,index)) { "Invalid service response." }; index += value.length }
        fun string() {
            require(index < text.length && text[index++] == '"') { "Invalid service response." }
            while (index < text.length) {
                val c = text[index++]
                if (c == '"') return
                require(c >= ' ') { "Invalid service response." }
                if (c == '\\') {
                    require(index < text.length) { "Incomplete service response." }
                    val escaped = text[index++]
                    if (escaped == 'u') {
                        repeat(4) { require(index < text.length && text[index++].digitToIntOrNull(16) != null) { "Invalid service response." } }
                    } else require(escaped in "\"\\/bfnrt") { "Invalid service response." }
                }
            }
            error("Incomplete service response.")
        }
    }
    private val number = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
}

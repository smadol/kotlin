fun foo(a: Int, b: Int) = if (a > b) a else b

fun bar(a: Double, b: Double): Double {
    if (a > b) {
        println(a)
        return a
    } else {
        println(b)
        return b
    }
}
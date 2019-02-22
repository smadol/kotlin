fun test() {
    val x = ClassWithExternalAnnotatedMethods()
    x.<warning descr="[DEPRECATION] 'deprecatedMethod(): Unit' is deprecated. Deprecated in Java">deprecatedMethod</warning>()
}
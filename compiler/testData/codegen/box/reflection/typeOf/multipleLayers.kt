// IGNORE_BACKEND: JS, JS_IR, NATIVE
// WITH_REFLECT

// FILE: typeOf.kt
// TODO: remove this in 1.4
package kotlin.reflect
inline fun <reified T> typeOf(): KType = null!!

// FILE: test.kt

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

interface C

inline fun <reified T> get() = typeOf<T>()

inline fun <reified U> get1() = get<U?>()

inline fun <reified V> get2() = get1<Map<in V?, Array<V>>>()

fun box(): String {
    assertEquals("kotlin.collections.Map<in test.C?, kotlin.Array<test.C>>?", get2<C>().toString())
    return "OK"
}

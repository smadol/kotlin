/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package collections

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UArrayJVMTest {
    @Test
    fun fill() {
        fun <A, E> testFailures(array: A, fill: A.(E, Int, Int) -> Unit, element: E, arraySize: Int) {
            assertFailsWith<ArrayIndexOutOfBoundsException> {
                array.fill(element, -1, arraySize)
            }
            assertFailsWith<ArrayIndexOutOfBoundsException> {
                array.fill(element, 0, arraySize + 1)
            }
            assertFailsWith<IllegalArgumentException> {
                array.fill(element, 1, 0)
            }
        }

        testFailures(UByteArray(5) { it.toUByte() }, UByteArray::fill, 0u, 5)
        testFailures(UShortArray(5) { it.toUShort() }, UShortArray::fill, 0u, 5)
        testFailures(UIntArray(5) { it.toUInt() }, UIntArray::fill, 0u, 5)
        testFailures(ULongArray(5) { it.toULong() }, ULongArray::fill, 0u, 5)

        fun <A, E> test(
            array: A,
            fill: A.(E, Int, Int) -> Unit,
            elements: List<E>,
            range: List<Pair<Int, Int>>,
            expectedResults: List<A>,
            contentEquals: A.(A) -> Boolean
        ) {
            for (i in elements.indices) {
                val element = elements[i]
                val (fromIndex, toIndex) = range[i]
                val expectedResult = expectedResults[i]

                array.fill(element, fromIndex, toIndex)
                assertTrue(array.contentEquals(expectedResult))
            }
        }

        val elements = listOf(5u, 1u, 2u, 3u)
        val range = listOf(
            1 to 4,
            0 to 5,
            0 to 3,
            2 to 5
        )
        val originalArray = UIntArray(5) { it.toUInt() }
        val expectedResults = listOf(
            uintArrayOf(0u, 5u, 5u, 5u, 4u),
            uintArrayOf(1u, 1u, 1u, 1u, 1u),
            uintArrayOf(2u, 2u, 2u, 1u, 1u),
            uintArrayOf(2u, 2u, 3u, 3u, 3u)
        )

        test(
            originalArray.toUByteArray(),
            UByteArray::fill,
            elements.map(UInt::toUByte),
            range,
            expectedResults.map(UIntArray::toUByteArray),
            UByteArray::contentEquals
        )

        test(
            originalArray.toUShortArray(),
            UShortArray::fill,
            elements.map(UInt::toUShort),
            range,
            expectedResults.map(UIntArray::toUShortArray),
            UShortArray::contentEquals
        )

        test(
            originalArray.copyOf(),
            UIntArray::fill,
            elements,
            range,
            expectedResults,
            UIntArray::contentEquals
        )

        test(
            originalArray.toULongArray(),
            ULongArray::fill,
            elements.map(UInt::toULong),
            range,
            expectedResults.map(UIntArray::toULongArray),
            ULongArray::contentEquals
        )
    }
}


private fun UIntArray.toUByteArray(): UByteArray {
    return UByteArray(size) { get(it).toUByte() }
}

private fun UIntArray.toUShortArray(): UShortArray {
    return UShortArray(size) { get(it).toUShort() }
}

private fun UIntArray.toULongArray(): ULongArray {
    return ULongArray(size) { get(it).toULong() }
}

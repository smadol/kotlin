/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package collections

import java.lang.IllegalArgumentException
import kotlin.test.*

class UArrayJVMTest {
    @Test
    fun fill() {
        val array = UIntArray(5) { it.toUInt() }

        assertFailsWith<ArrayIndexOutOfBoundsException> {
            array.fill(0u, -1, array.size)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            array.fill(0u, 0, array.size + 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.fill(0u, 2, 0)
        }

        array.fill(5u, 1, array.size - 1)
        assertEquals(array, uintArrayOf(0u, 5u, 5u, 5u, 4u))

        array.fill(1u)
        assertEquals(array, UIntArray(5) { 1u })
    }
}
/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.complex

import space.kscience.kmath.structures.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexBufferSpecTest {
    @Test
    fun testComplexBuffer() {
        val buffer = Buffer.complex(20) { Complex(it.toDouble(), -it.toDouble()) }
        assertEquals(Complex(5.0, -5.0), buffer[5])
    }
}
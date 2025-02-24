/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.tensors.core.internal

import space.kscience.kmath.nd.Strides
import kotlin.math.max


internal fun stridesFromShape(shape: IntArray): IntArray {
    val nDim = shape.size
    val res = IntArray(nDim)
    if (nDim == 0)
        return res

    var current = nDim - 1
    res[current] = 1

    while (current > 0) {
        res[current - 1] = max(1, shape[current]) * res[current]
        current--
    }
    return res
}

internal fun indexFromOffset(offset: Int, strides: IntArray, nDim: Int): IntArray {
    val res = IntArray(nDim)
    var current = offset
    var strideIndex = 0

    while (strideIndex < nDim) {
        res[strideIndex] = (current / strides[strideIndex])
        current %= strides[strideIndex]
        strideIndex++
    }
    return res
}

/**
 * This [Strides] implementation follows the last dimension first convention
 * For more information: https://numpy.org/doc/stable/reference/generated/numpy.ndarray.strides.html
 *
 * @param shape the shape of the tensor.
 */
internal class TensorLinearStructure(override val shape: IntArray) : Strides() {
    override val strides: IntArray
        get() = stridesFromShape(shape)

    override fun index(offset: Int): IntArray =
        indexFromOffset(offset, strides, shape.size)

    override val linearSize: Int
        get() = shape.reduce(Int::times)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TensorLinearStructure

        if (!shape.contentEquals(other.shape)) return false

        return true
    }

    override fun hashCode(): Int {
        return shape.contentHashCode()
    }
}

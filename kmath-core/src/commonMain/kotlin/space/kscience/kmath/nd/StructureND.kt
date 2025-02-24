/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.nd

import space.kscience.kmath.linear.LinearSpace
import space.kscience.kmath.misc.Feature
import space.kscience.kmath.misc.Featured
import space.kscience.kmath.misc.PerformancePitfall
import space.kscience.kmath.operations.Ring
import space.kscience.kmath.operations.invoke
import space.kscience.kmath.structures.Buffer
import space.kscience.kmath.structures.BufferFactory
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.reflect.KClass

public interface StructureFeature : Feature<StructureFeature>

/**
 * Represents n-dimensional structure i.e., multidimensional container of items of the same type and size. The number
 * of dimensions and items in an array is defined by its shape, which is a sequence of non-negative integers that
 * specify the sizes of each dimension.
 *
 * StructureND is in general identity-free. [StructureND.contentEquals] should be used in tests to compare contents.
 *
 * @param T the type of items.
 */
public interface StructureND<out T> : Featured<StructureFeature> {
    /**
     * The shape of structure i.e., non-empty sequence of non-negative integers that specify sizes of dimensions of
     * this structure.
     */
    public val shape: IntArray

    /**
     * The count of dimensions in this structure. It should be equal to size of [shape].
     */
    public val dimension: Int get() = shape.size

    /**
     * Returns the value at the specified indices.
     *
     * @param index the indices.
     * @return the value.
     */
    public operator fun get(index: IntArray): T

    /**
     * Returns the sequence of all the elements associated by their indices.
     *
     * @return the lazy sequence of pairs of indices to values.
     */
    @PerformancePitfall
    public fun elements(): Sequence<Pair<IntArray, T>>

    /**
     * Feature is some additional structure information that allows to access it special properties or hints.
     * If the feature is not present, `null` is returned.
     */
    override fun <F : StructureFeature> getFeature(type: KClass<out F>): F? = null

    public companion object {
        /**
         * Indicates whether some [StructureND] is equal to another one.
         */
        @PerformancePitfall
        public fun <T : Any> contentEquals(st1: StructureND<T>, st2: StructureND<T>): Boolean {
            if (st1 === st2) return true

            // fast comparison of buffers if possible
            if (st1 is BufferND && st2 is BufferND && st1.indexes == st2.indexes)
                return Buffer.contentEquals(st1.buffer, st2.buffer)

            //element by element comparison if it could not be avoided
            return st1.elements().all { (index, value) -> value == st2[index] }
        }

        @PerformancePitfall
        public fun contentEquals(
            st1: StructureND<Double>,
            st2: StructureND<Double>,
            tolerance: Double = 1e-11
        ): Boolean {
            if (st1 === st2) return true

            // fast comparison of buffers if possible
            if (st1 is BufferND && st2 is BufferND && st1.indexes == st2.indexes)
                return Buffer.contentEquals(st1.buffer, st2.buffer)

            //element by element comparison if it could not be avoided
            return st1.elements().all { (index, value) -> abs(value - st2[index]) < tolerance }
        }

        /**
         * Debug output to string
         */
        public fun toString(structure: StructureND<*>): String {
            val bufferRepr: String = when (structure.shape.size) {
                1 -> (0 until structure.shape[0]).map { structure[it] }
                    .joinToString(prefix = "[", postfix = "]", separator = ", ")
                2 -> (0 until structure.shape[0]).joinToString(prefix = "[", postfix = "]", separator = ", ") { i ->
                    (0 until structure.shape[1]).joinToString(prefix = "[", postfix = "]", separator = ", ") { j ->
                        structure[i, j].toString()
                    }
                }
                else -> "..."
            }
            val className = structure::class.simpleName ?: "StructureND"

            return "$className(shape=${structure.shape.contentToString()}, buffer=$bufferRepr)"
        }

        /**
         * Creates a NDStructure with explicit buffer factory.
         *
         * Strides should be reused if possible.
         */
        public fun <T> buffered(
            strides: Strides,
            bufferFactory: BufferFactory<T> = Buffer.Companion::boxing,
            initializer: (IntArray) -> T,
        ): BufferND<T> = BufferND(strides, bufferFactory(strides.linearSize) { i -> initializer(strides.index(i)) })

        /**
         * Inline create NDStructure with non-boxing buffer implementation if it is possible
         */
        public inline fun <reified T : Any> auto(
            strides: Strides,
            crossinline initializer: (IntArray) -> T,
        ): BufferND<T> = BufferND(strides, Buffer.auto(strides.linearSize) { i -> initializer(strides.index(i)) })

        public inline fun <T : Any> auto(
            type: KClass<T>,
            strides: Strides,
            crossinline initializer: (IntArray) -> T,
        ): BufferND<T> = BufferND(strides, Buffer.auto(type, strides.linearSize) { i -> initializer(strides.index(i)) })

        public fun <T> buffered(
            shape: IntArray,
            bufferFactory: BufferFactory<T> = Buffer.Companion::boxing,
            initializer: (IntArray) -> T,
        ): BufferND<T> = buffered(DefaultStrides(shape), bufferFactory, initializer)

        public inline fun <reified T : Any> auto(
            shape: IntArray,
            crossinline initializer: (IntArray) -> T,
        ): BufferND<T> = auto(DefaultStrides(shape), initializer)

        @JvmName("autoVarArg")
        public inline fun <reified T : Any> auto(
            vararg shape: Int,
            crossinline initializer: (IntArray) -> T,
        ): BufferND<T> =
            auto(DefaultStrides(shape), initializer)

        public inline fun <T : Any> auto(
            type: KClass<T>,
            vararg shape: Int,
            crossinline initializer: (IntArray) -> T,
        ): BufferND<T> = auto(type, DefaultStrides(shape), initializer)
    }
}

/**
 * Indicates whether some [StructureND] is equal to another one.
 */
@PerformancePitfall
public fun <T : Comparable<T>> AlgebraND<T, Ring<T>>.contentEquals(
    st1: StructureND<T>,
    st2: StructureND<T>,
): Boolean = StructureND.contentEquals(st1, st2)

/**
 * Indicates whether some [StructureND] is equal to another one.
 */
@PerformancePitfall
public fun <T : Comparable<T>> LinearSpace<T, Ring<T>>.contentEquals(
    st1: StructureND<T>,
    st2: StructureND<T>,
): Boolean = StructureND.contentEquals(st1, st2)

/**
 * Indicates whether some [StructureND] is equal to another one with [absoluteTolerance].
 */
@PerformancePitfall
public fun <T : Comparable<T>> GroupOpsND<T, Ring<T>>.contentEquals(
    st1: StructureND<T>,
    st2: StructureND<T>,
    absoluteTolerance: T,
): Boolean = st1.elements().all { (index, value) -> elementAlgebra { (value - st2[index]) } < absoluteTolerance }

/**
 * Indicates whether some [StructureND] is equal to another one with [absoluteTolerance].
 */
@PerformancePitfall
public fun <T : Comparable<T>> LinearSpace<T, Ring<T>>.contentEquals(
    st1: StructureND<T>,
    st2: StructureND<T>,
    absoluteTolerance: T,
): Boolean = st1.elements().all { (index, value) -> elementAlgebra { (value - st2[index]) } < absoluteTolerance }

/**
 * Returns the value at the specified indices.
 *
 * @param index the indices.
 * @return the value.
 */
public operator fun <T> StructureND<T>.get(vararg index: Int): T = get(index)

//@UnstableKMathAPI
//public inline fun <reified T : StructureFeature> StructureND<*>.getFeature(): T? = getFeature(T::class)

/**
 * Represents mutable [StructureND].
 */
public interface MutableStructureND<T> : StructureND<T> {
    /**
     * Inserts an item at the specified indices.
     *
     * @param index the indices.
     * @param value the value.
     */
    public operator fun set(index: IntArray, value: T)
}

/**
 * Transform a structure element-by element in place.
 */
@OptIn(PerformancePitfall::class)
public inline fun <T> MutableStructureND<T>.mapInPlace(action: (index: IntArray, t: T) -> T): Unit =
    elements().forEach { (index, oldValue) -> this[index] = action(index, oldValue) }

public inline fun <reified T : Any> StructureND<T>.zip(
    struct: StructureND<T>,
    crossinline block: (T, T) -> T,
): StructureND<T> {
    require(shape.contentEquals(struct.shape)) { "Shape mismatch in structure combination" }
    return StructureND.auto(shape) { block(this[it], struct[it]) }
}

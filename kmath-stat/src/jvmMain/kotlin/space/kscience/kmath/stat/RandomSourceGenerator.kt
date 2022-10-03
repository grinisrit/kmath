/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.stat

import org.apache.commons.rng.UniformRandomProvider
import org.apache.commons.rng.simple.RandomSource

/**
 * Implements [RandomGenerator] by delegating all operations to [RandomSource].
 *
 * @property source the underlying [RandomSource] object.
 */
public class RandomSourceGenerator internal constructor(public val source: RandomSource, seed: Long?) : RandomGenerator {
    internal val random: UniformRandomProvider = seed?.let { RandomSource.create(source, seed) }
        ?: RandomSource.create(source)

    override fun nextBoolean(): Boolean = random.nextBoolean()
    override fun nextDouble(): Double = random.nextDouble()
    override fun nextInt(): Int = random.nextInt()
    override fun nextInt(until: Int): Int = random.nextInt(until)
    override fun nextLong(): Long = random.nextLong()
    override fun nextLong(until: Long): Long = random.nextLong(until)

    override fun fillBytes(array: ByteArray, fromIndex: Int, toIndex: Int) {
        require(toIndex > fromIndex)
        random.nextBytes(array, fromIndex, toIndex - fromIndex)
    }

    override fun fork(): RandomGenerator = RandomSourceGenerator(source, nextLong())
}

/**
 * Implements [UniformRandomProvider] by delegating all operations to [RandomGenerator].
 *
 * @property generator the underlying [RandomGenerator] object.
 */
public class RandomGeneratorProvider(public val generator: RandomGenerator) : UniformRandomProvider {
    /**
     * Generates a [Boolean] value.
     *
     * @return the next random value.
     */
    override fun nextBoolean(): Boolean = generator.nextBoolean()

    /**
     * Generates a [Float] value between 0 and 1.
     *
     * @return the next random value between 0 and 1.
     */
    override fun nextFloat(): Float = generator.nextDouble().toFloat()

    /**
     * Generates [Byte] values and places them into a user-supplied array.
     *
     * The number of random bytes produced is equal to the length of the byte array.
     *
     * @param bytes byte array in which to put the random bytes.
     */
    override fun nextBytes(bytes: ByteArray): Unit = generator.fillBytes(bytes)

    /**
     * Generates [Byte] values and places them into a user-supplied array.
     *
     * The array is filled with bytes extracted from random integers. This implies that the number of random bytes
     * generated may be larger than the length of the byte array.
     *
     * @param bytes the array in which to put the generated bytes.
     * @param start the index at which to start inserting the generated bytes.
     * @param len the number of bytes to insert.
     */
    override fun nextBytes(bytes: ByteArray, start: Int, len: Int) {
        generator.fillBytes(bytes, start, start + len)
    }

    /**
     * Generates an [Int] value.
     *
     * @return the next random value.
     */
    override fun nextInt(): Int = generator.nextInt()

    /**
     * Generates an [Int] value between 0 (inclusive) and the specified value (exclusive).
     *
     * @param n the bound on the random number to be returned. Must be positive.
     * @return a random integer between 0 (inclusive) and [n] (exclusive).
     */
    override fun nextInt(n: Int): Int = generator.nextInt(n)

    /**
     * Generates a [Double] value between 0 and 1.
     *
     * @return the next random value between 0 and 1.
     */
    override fun nextDouble(): Double = generator.nextDouble()

    /**
     * Generates a [Long] value.
     *
     * @return the next random value.
     */
    override fun nextLong(): Long = generator.nextLong()

    /**
     * Generates a [Long] value between 0 (inclusive) and the specified value (exclusive).
     *
     * @param n Bound on the random number to be returned.  Must be positive.
     * @return a random long value between 0 (inclusive) and [n] (exclusive).
     */
    override fun nextLong(n: Long): Long = generator.nextLong(n)
}

/**
 * Represent this [RandomGenerator] as commons-rng [UniformRandomProvider] preserving and mirroring its current state.
 * Getting new value from one of those changes the state of another.
 */
public fun RandomGenerator.asUniformRandomProvider(): UniformRandomProvider = if (this is RandomSourceGenerator)
    random
else
    RandomGeneratorProvider(this)

/**
 * Returns [RandomSourceGenerator] with given [RandomSource] and [seed].
 */
public fun RandomGenerator.Companion.fromSource(source: RandomSource, seed: Long? = null): RandomSourceGenerator =
    RandomSourceGenerator(source, seed)

/**
 * Returns [RandomSourceGenerator] with [RandomSource.MT] algorithm and given [seed].
 */
public fun RandomGenerator.Companion.mersenneTwister(seed: Long? = null): RandomSourceGenerator =
    fromSource(RandomSource.MT, seed)

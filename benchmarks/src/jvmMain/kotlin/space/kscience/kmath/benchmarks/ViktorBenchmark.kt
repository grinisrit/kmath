/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.benchmarks

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.jetbrains.bio.viktor.F64Array
import space.kscience.kmath.nd.*
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.structures.Buffer
import space.kscience.kmath.viktor.ViktorFieldND

@State(Scope.Benchmark)
internal class ViktorBenchmark {
    @Benchmark
    fun automaticFieldAddition(blackhole: Blackhole) {
        with(autoField) {
            var res: StructureND<Double> = one(shape)
            repeat(n) { res += 1.0 }
            blackhole.consume(res)
        }
    }

    @Benchmark
    fun realFieldAddition(blackhole: Blackhole) {
        with(realField) {
            var res: StructureND<Double> = one(shape)
            repeat(n) { res += 1.0 }
            blackhole.consume(res)
        }
    }

    @Benchmark
    fun viktorFieldAddition(blackhole: Blackhole) {
        with(viktorField) {
            var res = one(shape)
            repeat(n) { res += 1.0 }
            blackhole.consume(res)
        }
    }

    @Benchmark
    fun rawViktor(blackhole: Blackhole) {
        val one = F64Array.full(init = 1.0, shape = intArrayOf(dim, dim))
        var res = one
        repeat(n) { res = res + one }
        blackhole.consume(res)
    }

    private companion object {
        private const val dim = 1000
        private const val n = 100
        private val shape = Shape(dim, dim)

        // automatically build context most suited for given type.
        private val autoField = BufferedFieldOpsND(DoubleField, Buffer.Companion::auto)
        private val realField = DoubleField.ndAlgebra
        private val viktorField = ViktorFieldND(dim, dim)
    }
}

/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package space.kscience.kmath.nd

import space.kscience.kmath.operations.Algebra
import space.kscience.kmath.operations.Group
import space.kscience.kmath.operations.Ring
import kotlin.jvm.JvmName


public fun <T, A : Algebra<T>> AlgebraND<T, A>.structureND(
    shapeFirst: Int,
    vararg shapeRest: Int,
    initializer: A.(IntArray) -> T
): StructureND<T> = structureND(Shape(shapeFirst, *shapeRest), initializer)

public fun <T, A : Group<T>> AlgebraND<T, A>.zero(shape: Shape): StructureND<T> = structureND(shape) { zero }

@JvmName("zeroVarArg")
public fun <T, A : Group<T>> AlgebraND<T, A>.zero(
    shapeFirst: Int,
    vararg shapeRest: Int,
): StructureND<T> = structureND(shapeFirst, *shapeRest) { zero }

public fun <T, A : Ring<T>> AlgebraND<T, A>.one(shape: Shape): StructureND<T> = structureND(shape) { one }

@JvmName("oneVarArg")
public fun <T, A : Ring<T>> AlgebraND<T, A>.one(
    shapeFirst: Int,
    vararg shapeRest: Int,
): StructureND<T> = structureND(shapeFirst, *shapeRest) { one }
package space.kscience.kmath.tensors.core

import space.kscience.kmath.nd.MutableStructure1D
import space.kscience.kmath.nd.MutableStructure2D
import space.kscience.kmath.nd.as1D
import space.kscience.kmath.nd.as2D
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt


internal inline fun <T> BufferedTensor<T>.vectorSequence(): Sequence<BufferedTensor<T>> = sequence {
    val n = shape.size
    val vectorOffset = shape[n - 1]
    val vectorShape = intArrayOf(shape.last())
    for (offset in 0 until numel step vectorOffset) {
        val vector = BufferedTensor(vectorShape, buffer, offset)
        yield(vector)
    }
}

internal inline fun <T> BufferedTensor<T>.matrixSequence(): Sequence<BufferedTensor<T>> = sequence {
    check(shape.size >= 2) { "todo" }
    val n = shape.size
    val matrixOffset = shape[n - 1] * shape[n - 2]
    val matrixShape = intArrayOf(shape[n - 2], shape[n - 1])
    for (offset in 0 until numel step matrixOffset) {
        val matrix = BufferedTensor(matrixShape, buffer, offset)
        yield(matrix)
    }
}

internal inline fun <T> BufferedTensor<T>.forEachVector(vectorAction: (BufferedTensor<T>) -> Unit): Unit {
    for (vector in vectorSequence()) {
        vectorAction(vector)
    }
}

internal inline fun <T> BufferedTensor<T>.forEachMatrix(matrixAction: (BufferedTensor<T>) -> Unit): Unit {
    for (matrix in matrixSequence()) {
        matrixAction(matrix)
    }
}


internal inline fun dotHelper(
    a: MutableStructure2D<Double>,
    b: MutableStructure2D<Double>,
    res: MutableStructure2D<Double>,
    l: Int, m: Int, n: Int
) {
    for (i in 0 until l) {
        for (j in 0 until n) {
            var curr = 0.0
            for (k in 0 until m) {
                curr += a[i, k] * b[k, j]
            }
            res[i, j] = curr
        }
    }
}

internal inline fun luHelper(lu: MutableStructure2D<Double>, pivots: MutableStructure1D<Int>, m: Int) {
    for (row in 0 until m) pivots[row] = row

    for (i in 0 until m) {
        var maxVal = -1.0
        var maxInd = i

        for (k in i until m) {
            val absA = kotlin.math.abs(lu[k, i])
            if (absA > maxVal) {
                maxVal = absA
                maxInd = k
            }
        }

        //todo check singularity

        if (maxInd != i) {

            val j = pivots[i]
            pivots[i] = pivots[maxInd]
            pivots[maxInd] = j

            for (k in 0 until m) {
                val tmp = lu[i, k]
                lu[i, k] = lu[maxInd, k]
                lu[maxInd, k] = tmp
            }

            pivots[m] += 1

        }

        for (j in i + 1 until m) {
            lu[j, i] /= lu[i, i]
            for (k in i + 1 until m) {
                lu[j, k] -= lu[j, i] * lu[i, k]
            }
        }
    }
}

internal inline fun pivInit(
    p: MutableStructure2D<Double>,
    pivot: MutableStructure1D<Int>,
    n: Int
) {
    for (i in 0 until n) {
        p[i, pivot[i]] = 1.0
    }
}

internal inline fun luPivotHelper(
    l: MutableStructure2D<Double>,
    u: MutableStructure2D<Double>,
    lu: MutableStructure2D<Double>,
    n: Int
) {
    for (i in 0 until n) {
        for (j in 0 until n) {
            if (i == j) {
                l[i, j] = 1.0
            }
            if (j < i) {
                l[i, j] = lu[i, j]
            }
            if (j >= i) {
                u[i, j] = lu[i, j]
            }
        }
    }
}

internal inline fun choleskyHelper(
    a: MutableStructure2D<Double>,
    l: MutableStructure2D<Double>,
    n: Int
) {
    for (i in 0 until n) {
        for (j in 0 until i) {
            var h = a[i, j]
            for (k in 0 until j) {
                h -= l[i, k] * l[j, k]
            }
            l[i, j] = h / l[j, j]
        }
        var h = a[i, i]
        for (j in 0 until i) {
            h -= l[i, j] * l[i, j]
        }
        l[i, i] = sqrt(h)
    }
}

internal inline fun luMatrixDet(luTensor: MutableStructure2D<Double>, pivotsTensor: MutableStructure1D<Int>): Double {
    val lu = luTensor.as2D()
    val pivots = pivotsTensor.as1D()
    val m = lu.shape[0]
    val sign = if ((pivots[m] - m) % 2 == 0) 1.0 else -1.0
    return (0 until m).asSequence().map { lu[it, it] }.fold(sign) { left, right -> left * right }
}

internal inline fun luMatrixInv(
    lu: MutableStructure2D<Double>,
    pivots: MutableStructure1D<Int>,
    invMatrix: MutableStructure2D<Double>
) {
    val m = lu.shape[0]

    for (j in 0 until m) {
        for (i in 0 until m) {
            if (pivots[i] == j) {
                invMatrix[i, j] = 1.0
            }

            for (k in 0 until i) {
                invMatrix[i, j] -= lu[i, k] * invMatrix[k, j]
            }
        }

        for (i in m - 1 downTo 0) {
            for (k in i + 1 until m) {
                invMatrix[i, j] -= lu[i, k] * invMatrix[k, j]
            }
            invMatrix[i, j] /= lu[i, i]
        }
    }
}

internal inline fun DoubleLinearOpsTensorAlgebra.qrHelper(
    matrix: DoubleTensor,
    q: DoubleTensor,
    r: MutableStructure2D<Double>
) {
    checkSquareMatrix(matrix.shape)
    val n = matrix.shape[0]
    val qM = q.as2D()
    val matrixT = matrix.transpose(0, 1)
    val qT = q.transpose(0, 1)

    for (j in 0 until n) {
        val v = matrixT[j]
        val vv = v.as1D()
        if (j > 0) {
            for (i in 0 until j) {
                r[i, j] = (qT[i] dot matrixT[j]).value()
                for (k in 0 until n) {
                    val qTi = qT[i].as1D()
                    vv[k] = vv[k] - r[i, j] * qTi[k]
                }
            }
        }
        r[j, j] = DoubleAnalyticTensorAlgebra { (v dot v).sqrt().value() }
        for (i in 0 until n) {
            qM[i, j] = vv[i] / r[j, j]
        }
    }
}

internal inline fun DoubleLinearOpsTensorAlgebra.svd1d(a: DoubleTensor, epsilon: Double = 1e-10): DoubleTensor {
    val (n, m) = a.shape
    var v: DoubleTensor
    val b: DoubleTensor
    if (n > m) {
        b = a.transpose(0, 1).dot(a)
        v = DoubleTensor(intArrayOf(m), getRandomNormals(m, 0))
    } else {
        b = a.dot(a.transpose(0, 1))
        v = DoubleTensor(intArrayOf(n), getRandomNormals(n, 0))
    }

    var lastV: DoubleTensor
    while (true) {
        lastV = v
        v = b.dot(lastV)
        val norm = DoubleAnalyticTensorAlgebra { (v dot v).sqrt().value() }
        v = v.times(1.0 / norm)
        if (abs(v.dot(lastV).value()) > 1 - epsilon) {
            return v
        }
    }
}

internal inline fun DoubleLinearOpsTensorAlgebra.svdHelper(
    matrix: DoubleTensor,
    USV: Pair<BufferedTensor<Double>, Pair<BufferedTensor<Double>, BufferedTensor<Double>>>,
    m: Int, n: Int
): Unit {
    val res = ArrayList<Triple<Double, DoubleTensor, DoubleTensor>>(0)
    val (matrixU, SV) = USV
    val (matrixS, matrixV) = SV

    for (k in 0 until min(n, m)) {
        var a = matrix.copy()
        for ((singularValue, u, v) in res.slice(0 until k)) {
            val outerProduct = DoubleArray(u.shape[0] * v.shape[0])
            for (i in 0 until u.shape[0]) {
                for (j in 0 until v.shape[0]) {
                    outerProduct[i * v.shape[0] + j] = u[i].value() * v[j].value()
                }
            }
            a = a - singularValue.times(DoubleTensor(intArrayOf(u.shape[0], v.shape[0]), outerProduct))
        }
        var v: DoubleTensor
        var u: DoubleTensor
        var norm: Double
        if (n > m) {
            v = svd1d(a)
            u = matrix.dot(v)
            norm = DoubleAnalyticTensorAlgebra { (u dot u).sqrt().value() }
            u = u.times(1.0 / norm)
        } else {
            u = svd1d(a)
            v = matrix.transpose(0, 1).dot(u)
            norm = DoubleAnalyticTensorAlgebra { (v dot v).sqrt().value() }
            v = v.times(1.0 / norm)
        }

        res.add(Triple(norm, u, v))
    }

    val s = res.map { it.first }.toDoubleArray()
    val uBuffer = res.map { it.second }.flatMap { it.buffer.array().toList() }.toDoubleArray()
    val vBuffer = res.map { it.third }.flatMap { it.buffer.array().toList() }.toDoubleArray()
    uBuffer.copyInto(matrixU.buffer.array())
    s.copyInto(matrixS.buffer.array())
    vBuffer.copyInto(matrixV.buffer.array())
}

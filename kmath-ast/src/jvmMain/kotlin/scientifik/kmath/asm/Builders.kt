package scientifik.kmath.asm

import scientifik.kmath.ast.MST
import scientifik.kmath.ast.evaluate
import scientifik.kmath.expressions.Expression
import scientifik.kmath.operations.*

@PublishedApi
internal fun buildName(expression: AsmExpression<*>, collision: Int = 0): String {
    val name = "scientifik.kmath.expressions.generated.AsmCompiledExpression_${expression.hashCode()}_$collision"

    try {
        Class.forName(name)
    } catch (ignored: ClassNotFoundException) {
        return name
    }

    return buildName(expression, collision + 1)
}

inline fun <reified T> AsmExpression<T>.compile(algebra: Algebra<T>): Expression<T> {
    val ctx = AsmGenerationContext(T::class.java, algebra, buildName(this))
    invoke(ctx)
    return ctx.generate()
}

inline fun <reified T, A : NumericAlgebra<T>, E : AsmExpressionAlgebra<T, A>> A.asm(
    expressionAlgebra: E,
    block: E.() -> AsmExpression<T>
): Expression<T> = expressionAlgebra.block().compile(expressionAlgebra.algebra)

inline fun <reified T, A : NumericAlgebra<T>, E : AsmExpressionAlgebra<T, A>> A.asm(
    expressionAlgebra: E,
    ast: MST
): Expression<T> = asm(expressionAlgebra) { evaluate(ast) }

inline fun <reified T, A> A.asmSpace(block: AsmExpressionSpace<T, A>.() -> AsmExpression<T>): Expression<T> where A : NumericAlgebra<T>, A : Space<T> =
    AsmExpressionSpace(this).let { it.block().compile(it.algebra) }

inline fun <reified T, A> A.asmSpace(ast: MST): Expression<T> where A : NumericAlgebra<T>, A : Space<T> =
    asmSpace { evaluate(ast) }

inline fun <reified T, A> A.asmRing(block: AsmExpressionRing<T, A>.() -> AsmExpression<T>): Expression<T> where A : NumericAlgebra<T>, A : Ring<T> =
    AsmExpressionRing(this).let { it.block().compile(it.algebra) }

inline fun <reified T, A> A.asmRing(ast: MST): Expression<T> where A : NumericAlgebra<T>, A : Ring<T> =
    asmRing { evaluate(ast) }

inline fun <reified T, A> A.asmField(block: AsmExpressionField<T, A>.() -> AsmExpression<T>): Expression<T> where A : NumericAlgebra<T>, A : Field<T> =
    AsmExpressionField(this).let { it.block().compile(it.algebra) }

inline fun <reified T, A> A.asmField(ast: MST): Expression<T> where A : NumericAlgebra<T>, A : Field<T> =
    asmRing { evaluate(ast) }

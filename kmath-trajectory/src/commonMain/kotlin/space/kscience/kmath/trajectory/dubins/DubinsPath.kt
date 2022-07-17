/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.trajectory.dubins

import space.kscience.kmath.geometry.Euclidean2DSpace.distanceTo
import space.kscience.kmath.geometry.Vector2D
import space.kscience.kmath.trajectory.segments.Arc
import space.kscience.kmath.trajectory.segments.Segment
import space.kscience.kmath.trajectory.segments.Straight
import space.kscience.kmath.trajectory.segments.components.Circle
import space.kscience.kmath.trajectory.segments.components.Pose2D
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

public class DubinsPath(
    public val a: Arc,
    public val b: Segment,
    public val c: Arc,
) {

    public val type: TYPE = TYPE.valueOf(
        arrayOf(
            a.direction.name[0],
            if (b is Arc) b.direction.name[0] else 'S',
            c.direction.name[0]
        ).toCharArray().concatToString()
    )

    public val length: Double = a.length + b.length + c.length

    public enum class TYPE {
        RLR, LRL, RSR, LSL, RSL, LSR
    }

    public companion object {
        public fun all(start: Pose2D, end: Pose2D, turningRadius: Double): List<DubinsPath> =
            listOfNotNull(
                rlr(start, end, turningRadius),
                lrl(start, end, turningRadius),
                rsr(start, end, turningRadius),
                lsl(start, end, turningRadius),
                rsl(start, end, turningRadius),
                lsr(start, end, turningRadius)
            )
        public fun shortest(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath =
            all(start, end, turningRadius).minByOrNull { it.length }!!

        public fun rlr(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath? {
            val c1 = start.getRightCircle(turningRadius)
            val c2 = end.getRightCircle(turningRadius)
            val centers = Straight(c1.center, c2.center)
            if (centers.length > turningRadius * 4) return null

            var theta = theta(centers.theta - acos(centers.length / (turningRadius * 4)))
            var dX = turningRadius * sin(theta)
            var dY = turningRadius * cos(theta)
            val p = Vector2D(c1.center.x + dX * 2, c1.center.y + dY * 2)
            val e = Circle(p, turningRadius)
            val p1 = Vector2D(c1.center.x + dX, c1.center.y + dY)
            theta = theta(centers.theta + acos(centers.length / (turningRadius * 4)))
            dX = turningRadius * sin(theta)
            dY = turningRadius * cos(theta)
            val p2 = Vector2D(e.center.x + dX, e.center.y + dY)
            val a1 = Arc(c1.center, start, p1, Arc.Direction.RIGHT)
            val a2 = Arc(e.center, p1, p2, Arc.Direction.LEFT)
            val a3 = Arc(c2.center, p2, end, Arc.Direction.RIGHT)
            return DubinsPath(a1, a2, a3)
        }

        public fun lrl(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath? {
            val c1 = start.getLeftCircle(turningRadius)
            val c2 = end.getLeftCircle(turningRadius)
            val centers = Straight(c1.center, c2.center)
            if (centers.length > turningRadius * 4) return null

            var theta = theta(centers.theta + acos(centers.length / (turningRadius * 4)))
            var dX = turningRadius * sin(theta)
            var dY = turningRadius * cos(theta)
            val p = Vector2D(c1.center.x + dX * 2, c1.center.y + dY * 2)
            val e = Circle(p, turningRadius)
            val p1 = Vector2D(c1.center.x + dX, c1.center.y + dY)
            theta = theta(centers.theta - acos(centers.length / (turningRadius * 4)))
            dX = turningRadius * sin(theta)
            dY = turningRadius * cos(theta)
            val p2 = Vector2D(e.center.x + dX, e.center.y + dY)
            val a1 = Arc(c1.center, start, p1, Arc.Direction.LEFT)
            val a2 = Arc(e.center, p1, p2, Arc.Direction.RIGHT)
            val a3 = Arc(c2.center, p2, end, Arc.Direction.LEFT)
            return DubinsPath(a1, a2, a3)
        }

        public fun rsr(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath {
            val c1 = start.getRightCircle(turningRadius)
            val c2 = end.getRightCircle(turningRadius)
            val s = leftOuterTangent(c1, c2)
            val a1 = Arc(c1.center, start, s.start, Arc.Direction.RIGHT)
            val a3 = Arc(c2.center, s.end, end, Arc.Direction.RIGHT)
            return DubinsPath(a1, s, a3)
        }

        public fun lsl(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath {
            val c1 = start.getLeftCircle(turningRadius)
            val c2 = end.getLeftCircle(turningRadius)
            val s = rightOuterTangent(c1, c2)
            val a1 = Arc(c1.center, start, s.start, Arc.Direction.LEFT)
            val a3 = Arc(c2.center, s.end, end, Arc.Direction.LEFT)
            return DubinsPath(a1, s, a3)
        }

        public fun rsl(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath? {
            val c1 = start.getRightCircle(turningRadius)
            val c2 = end.getLeftCircle(turningRadius)
            val s = rightInnerTangent(c1, c2)
            if (c1.center.distanceTo(c2.center) < turningRadius * 2 || s == null) return null

            val a1 = Arc(c1.center, start, s.start, Arc.Direction.RIGHT)
            val a3 = Arc(c2.center, s.end, end, Arc.Direction.LEFT)
            return DubinsPath(a1, s, a3)
        }

        public fun lsr(start: Pose2D, end: Pose2D, turningRadius: Double): DubinsPath? {
            val c1 = start.getLeftCircle(turningRadius)
            val c2 = end.getRightCircle(turningRadius)
            val s = leftInnerTangent(c1, c2)
            if (c1.center.distanceTo(c2.center) < turningRadius * 2 || s == null) return null

            val a1 = Arc(c1.center, start, s.start, Arc.Direction.LEFT)
            val a3 = Arc(c2.center, s.end, end, Arc.Direction.RIGHT)
            return DubinsPath(a1, s, a3)
        }
    }
}

import jaci.pathfinder.Pathfinder
import jaci.pathfinder.Trajectory
import jaci.pathfinder.Waypoint
import jaci.pathfinder.modifiers.TankModifier

import static jaci.pathfinder.Pathfinder.d2r
import static jaci.pathfinder.Pathfinder.r2d


class PathDesc {
    Waypoint[] waypoints
    String name
    double maxVel = 50.0
    double maxAccel = 0.8 * maxVel
    double maxJerk = maxAccel
}
/*

max vel
 ~ 1100 ticks / 100 ms
 ~ 144 in/sec
 ~ 12 ft/sec
 ~ 8 mph
 */

@Newify([Waypoint, PathDesc])
class PathfinderTest {


    static def pathDescs = [
            PathDesc(name: "DriveStraight_10ft",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(0, 120, 90)
                    ]),
            /*
           [x] x needs to be 6"-12"
           [x] test
             */
            PathDesc(name: "CRSwitch",
//                    maxVel: 100.0,
//                    maxAccel: 60.0,
//                    maxJerk: 50,
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(40, 108, 90)
                    ]),
            /*
            cuts off pyramid of cubes
             */
            PathDesc(name: "CLSwitch",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(-90, 110, 90)
                    ]),
            /*
              [x]  y needs 8" - +12"
              [] test
             */
            PathDesc(name: "RRSwitch",
//                    maxVel: 100.0,
//                    maxAccel: 40.0,
//                    maxJerk: 50,
                    waypoints: [
                            Waypoint(0, 0, 90),
                            Waypoint(10, 100, 90),
                            Waypoint(-44, 140, 180),
//                                Waypoint(0, 0, 90),
//                                Waypoint(10, 100, 90),
//                                Waypoint(10, 120, 90),
//                                Waypoint(-30, 145, 180),
//                                Waypoint(-44, 145, 180)
                    ]),
            /*
          [x] 8" ->
          [x] 12" \/
          [] test
             */
            PathDesc(name: "LLSwitch",
                    waypoints: [
                            Waypoint(0, 0, 90),
                            Waypoint(-10, 100, 90),
                            Waypoint(44, 140, 0),
//                            Waypoint(0, 0, 90),
//                            Waypoint(-20, 100, 90),
//                            Waypoint(-20,120,90),
//                            Waypoint(10,137,0),
//                            Waypoint(34, 150, 0),
                    ]),
            /*ok*/
            PathDesc(name: "RFwd",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(-10, 200, 90)
                    ]),
            /*ok
             */
            PathDesc(name: "LFwd",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(0, 220, 90)
                    ]),
            /*
           [x] y needs to be ~ +6"
           [] x needs to be  -> 12"
           [] test
             */
            PathDesc(name: "RRScale",
//                    maxVel: 80.0,
//                    maxAccel: 40.0,
//                    maxJerk: 20,
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(10, 100, 90),
                                Waypoint(10, 230, 90),
                                Waypoint(14, 260, 90),
                                Waypoint(14, 272, 90),
                                Waypoint(-5, 296, 180),
                                Waypoint(-32,296,180)
                    ]),
//            /*
//           [] y is too far by 12"
//           [] x is too far by 24"
//             */
//            PathDesc(name: "LLScale",
//                    waypoints: [Waypoint(0, 0, 90),
//                                Waypoint(-26, 100, 90),
//                                Waypoint(-26, 300, 90),
//                                Waypoint(-10, 310, 180)
//                    ]),

                        PathDesc(name: "LLScale",
                                waypoints: [Waypoint(0, 0, 90),
                                            Waypoint(-10, 100, 90),
                                            Waypoint(-10, 240, 90),
                                            Waypoint(-14, 270, 90),
                                            Waypoint(-14, 282, 90),
                                            Waypoint(5, 306, 180),
                                ]),

            PathDesc(name: "RLScale",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(0,12,90),
                                Waypoint(10,120,90),
                                Waypoint(10, 160, 90),
                                Waypoint(-60, 230, 180),
                                Waypoint(-130, 230, 180),
                                Waypoint(-180, 300, 90),
                                Waypoint(-180, 320, 90),
                    ]),
            PathDesc(name: "LRScale",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(0,12,90),
                                Waypoint(-10,120,90),
                                Waypoint(-10, 146, 90),
                                Waypoint(60, 230, 180),
                                Waypoint(130, 230, 180),
                                Waypoint(180, 300, 90),
                                Waypoint(180, 320, 90),
                    ]),

    ]



    static final double cm_per_inch = 2.54
    static final double m_per_inch = cm_per_inch / 100.0 //0.0254
    static final double encoder_ticks_per_inch = 76.0
    static final double encoder_ticks_per_m = encoder_ticks_per_inch * (1.0 / m_per_inch) //~5118

    static def gen(PathDesc desc) {

        desc.waypoints.each { w ->
            w.x *= m_per_inch
            w.y *= m_per_inch
            w.angle = d2r(w.angle)
        }

        double timeStep = 0.01
        double maxVel = desc.maxVel * m_per_inch //(50.0 * m_per_inch)
        double maxAccel =desc.maxAccel * m_per_inch  //0.25 * maxVel
        double maxJerk = desc.maxJerk * m_per_inch


        def config = new Trajectory.Config(
                Trajectory.FitMethod.HERMITE_CUBIC,
                Trajectory.Config.SAMPLES_HIGH,
                timeStep, maxVel, maxAccel, maxJerk)


        def path = Pathfinder.generate(desc.waypoints, config)


        double wheelBase = 24.0 * m_per_inch

        def tank = new TankModifier(path)
        tank.modify(wheelBase)

        def leftPath = tank.leftTrajectory
        def rightPath = tank.rightTrajectory



        new File('gensrc').mkdirs() //windows fails ungracefully when this doesn't exist.
        new File('gencsv').mkdirs()

        Pathfinder.writeToCSV(new File("gencsv/${desc.name}_left.csv"), leftPath)
        Pathfinder.writeToCSV(new File("gencsv/${desc.name}_right.csv"), rightPath)
        Pathfinder.writeToCSV(new File("gencsv/${desc.name}_path.csv"), path)

        new File("gensrc/${desc.name}_profile.h").withPrintWriter { pw ->
            pw << """
#include <vector>
#include "profile.h"
namespace mp {
/*
(${desc.maxVel*m_per_inch}, ${desc.maxAccel*m_per_inch}, ${desc.maxJerk*m_per_inch})
-------------
\t${desc.waypoints.collect{"($it.x,$it.y,${r2d(it.angle)}"}.join(',\n\t')}
*/
extern std::vector<mp::Prof> ${desc.name};
}

"""
//constexpr double TIME_STEP = $timeStep ;
//extern std::array<mp::Prof,${path.length()}>
        }

        new File("gensrc/${desc.name}_profile.cpp").withPrintWriter{pw->
            pw<<"""
#include "${desc.name}_profile.h"


std::vector<mp::Prof> mp::${desc.name} =  {{
"""
            [leftPath, rightPath].collect { it.segments }.transpose().each { l, r ->
                def lp = l.position * encoder_ticks_per_m /// 10.0
                def lv = l.velocity * encoder_ticks_per_m / 10.0
                def rp = r.position * encoder_ticks_per_m /// 10.0
                def rv = r.velocity * encoder_ticks_per_m / 10.0
                pw << "\t{$lp,$lv,$rp,$rv},\n"
            }
            pw << '}};\n'
        }

    }

    static void main(args) {
       pathDescs.each{gen(it)}
    }
}

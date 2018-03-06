import jaci.pathfinder.Pathfinder
import jaci.pathfinder.Trajectory
import jaci.pathfinder.Waypoint
import jaci.pathfinder.modifiers.TankModifier

import static jaci.pathfinder.Pathfinder.d2r

class PathDesc {
    Waypoint[] waypoints
    String name
}


@Newify([Waypoint, PathDesc])
class PathfinderTest {


    static def pathDescs = [
            PathDesc(name: "DriveStraight_10ft",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(0, 120, 90)
                    ]),
            PathDesc(name: "RFwd",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(-10, 200, 90)
                    ]),
            PathDesc(name: "LFwd",
                    waypoints: [Waypoint(0.0d, 0.0d, 90.0d),
                                Waypoint(0, 220, 90)
                    ]),
            PathDesc(name: "RRSwitch",
                    waypoints: [
                            Waypoint(0, 0, 90),
                            Waypoint(10, 100, 90),
                            Waypoint(-45, 145, 180),
                    ]),
            PathDesc(name: "LLSwitch",
                    waypoints: [
                            Waypoint(0, 0, 90),
                            Waypoint(-20, 100, 90),
                            Waypoint(20, 157, 180),
                    ]),
            PathDesc(name: "RRScale",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(10, 100, 90),
                                Waypoint(10, 280, 90),
                                Waypoint(-15, 300, 180)
                    ]),
            PathDesc(name: "LLScale",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(-26, 100, 90),
                                Waypoint(-26, 300, 90),
                                Waypoint(-10, 310, 180)
                    ]),
            PathDesc(name: "CRSwitch",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(37, 110, 90)
                    ]),
            PathDesc(name: "CLSwitch",
                    waypoints: [Waypoint(0, 0, 90),
                                Waypoint(-90, 110, 90)
                    ]),

//            PathDesc(name: "CRScale",
//                    waypoints: [Waypoint(0, 0, 90),
//                                Waypoint(0,10,90),
//                                Waypoint(60, 100, 90),
//                                Waypoint(60, 180, 90),
//                                Waypoint(50, 300, 90),
//
//                    ]),
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

    ]

//    static final double inches_per_encoder_tick = 0.00872;
//    static final double encoder_ticks_per_inch = 114.7;

    static final double cm_per_inch = 2.54
    static final double m_per_inch = cm_per_inch / 100.0 //0.0254
    //static final double encoder_ticks_per_rev = 1410.0
    //static final double inches_per_rev = 6.0 * Math.PI //18.85
    //static final double m_per_rev = inches_per_rev * m_per_inch //0.48
    static final double encoder_ticks_per_inch = 76.0 //encoder_ticks_per_rev / inches_per_rev;
    static final double encoder_ticks_per_m = encoder_ticks_per_inch * (1.0 / m_per_inch); //~5118

/*
IMPORTANT:
ALL DISTANCE UNITS ARE IN METERS!!!
ALL ANGLES ARE MEASURED IN RADIANS!!!!!
 */

    static def gen(PathDesc desc) {

        desc.waypoints.each { w ->
            w.x *= m_per_inch
            w.y *= m_per_inch
            w.angle = d2r(w.angle)
        }

        double timeStep = 0.01
        double maxVel = (50.0 * m_per_inch)
        double maxAccel = 0.25 * maxVel
        double maxJerk = maxAccel * 10


        def config = new Trajectory.Config(
                Trajectory.FitMethod.HERMITE_QUINTIC,
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


    //SCRATCH:
    {

//        Waypoint[] waypoints = [
//                Waypoint(0, 0, 90),
//                Waypoint(10,100,90),
//                Waypoint(-40, 160, 180),
//        ]

//        Waypoint[] waypoints = [
//            Waypoint(0,0,90),
//            Waypoint(0,120,90)
//        ]

//        Waypoint[] waypoints = [
//                Waypoint(0, 0, 90),
//                Waypoint(10, 100, 90),
//                Waypoint(10,280,90),
//                Waypoint(-15,300,180),
//              //  Waypoint(-10, 324, 180)
//        ]
        //center to swtich
        Waypoint[] waypoints = [
                Waypoint(0, 0, 90),
                Waypoint(70, 140, 90)
        ]

        waypoints.each { w ->
            w.x *= m_per_inch
            w.y *= m_per_inch
            w.angle = d2r(w.angle)
        }

        double timeStep = 0.01
        double maxVel = (50.0 * m_per_inch)
        double maxAccel = 0.25 * maxVel
        double maxJerk = maxAccel * 10


        def config = new Trajectory.Config(
                Trajectory.FitMethod.HERMITE_QUINTIC,
                Trajectory.Config.SAMPLES_HIGH,
                timeStep, maxVel, maxAccel, maxJerk)


        def path = Pathfinder.generate(waypoints, config)


        double wheelBase = 34.0 * m_per_inch

        def tank = new TankModifier(path)
        tank.modify(wheelBase)

        def leftPath = tank.leftTrajectory
        def rightPath = tank.rightTrajectory



        Pathfinder.writeToCSV(new File('left.csv'), leftPath)
        Pathfinder.writeToCSV(new File('right.csv'), rightPath)
        Pathfinder.writeToCSV(new File("path.csv"), path)

        new File('profile.h').withPrintWriter { pw ->
//            pw << """
//#include <array>
//
//namespace mp {
//
//constexpr double TIME_STEP = $timeStep ;
//
//
//
////positions are in encoder ticks.
////velocities are in encoder ticks per 100 ms.
//struct Prof {
//    double leftPosition;
//    double leftVelocity;
//    double rightPosition;
//    double rightVelocity;
//};
//"""
            pw << """
#include "profile.h"
 std::array<mp::Prof,${path.length()}> mp::PROFS = {{
"""


            [leftPath, rightPath].collect { it.segments }.transpose().each { l, r ->
                def lp = l.position * encoder_ticks_per_m /// 10.0
                def lv = l.velocity * encoder_ticks_per_m / 10.0
                def rp = r.position * encoder_ticks_per_m /// 10.0
                def rv = r.velocity * encoder_ticks_per_m / 10.0
                pw << "\t{$lp,$lv,$rp,$rv},\n"
            }
            pw << '}};\n'

        } //withPrintWriter


    }


    static void main(args) {
       pathDescs.each{gen(it)}
        //new PathfinderTest()
    }
}

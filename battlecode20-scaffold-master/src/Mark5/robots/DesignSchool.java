package Mark5.robots;

import Mark5.sensors.DesignSchoolSensor;
import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import java.util.List;

import static Mark5.RobotPlayer.*;

public class DesignSchool {

    public static int numLandscapers = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();

        if (DesignSchoolSensor.droneNearby && !DesignSchoolSensor.netGunNearby) return;

        if (DesignSchoolSensor.numThreats > DesignSchoolSensor.numLandscapers || (
                rc.getRoundNum() > 1200 && rc.getRoundNum() % 3 == 0 && rc.getTeamSoup() > 400)) {
            if (Strategium.enemyHQLocation != null) {
                Direction dirToEnemyHQ = rc.getLocation().directionTo(Strategium.enemyHQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemyHQ.opposite()));
                for (Direction dir : towards)
                    if (tryBuild(RobotType.LANDSCAPER, dir)) {
                        ++numLandscapers;
                        return;
                    }
            }
            for (Direction buildDirection : DesignSchoolSensor.priorityBuildDirections)
                if (tryBuild(RobotType.LANDSCAPER, buildDirection)) {
                    ++numLandscapers;
                    return;
                }
            if (Strategium.HQLocation != null) {
                Direction dirToHQ = rc.getLocation().directionTo(Strategium.HQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToHQ.opposite()));
                for (Direction dir : towards)
                    if (tryBuild(RobotType.LANDSCAPER, dir)) {
                        ++numLandscapers;
                        return;
                    }
            }
            for (Direction dir : dir8) {
                if (tryBuild(RobotType.LANDSCAPER, dir)) {
                    ++numLandscapers;
                    return;
                }
            }

        } else if (RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost <= rc.getTeamSoup() ||

                ((numLandscapers < rc.getRoundNum() / 150 || numLandscapers < 5) &&

                        rc.getTeamSoup() > 2 * RobotType.LANDSCAPER.cost && rc.getRoundNum() > 150)) {
            if (Strategium.enemyHQLocation != null) {
                Direction dirToEnemyHQ = rc.getLocation().directionTo(Strategium.enemyHQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemyHQ.opposite()));
                for (Direction dir : towards)
                    if (!Lattice.isPit(rc.adjacentLocation(dir)))
                        if (tryBuild(RobotType.LANDSCAPER, dir)) {
                            ++numLandscapers;
                            return;
                        }
            }
            if (Strategium.HQLocation != null) {
                Direction dirToHQ = rc.getLocation().directionTo(Strategium.HQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToHQ.opposite()));
                for (Direction dir : towards)
                    if (!Lattice.isPit(rc.adjacentLocation(dir)))
                        if (tryBuild(RobotType.LANDSCAPER, dir)) {
                            ++numLandscapers;
                            return;
                        }
            }
            for (Direction dir : dir8) {
                if (!Lattice.isPit(rc.adjacentLocation(dir)))
                    if (tryBuild(RobotType.LANDSCAPER, dir)) {
                        ++numLandscapers;
                        return;
                    }
            }
        }

    }
}




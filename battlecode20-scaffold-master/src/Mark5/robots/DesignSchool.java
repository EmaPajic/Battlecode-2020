package Mark5.robots;

import Mark5.sensors.DesignSchoolSensor;
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
        
        if (DesignSchoolSensor.numThreats > DesignSchoolSensor.numLandscapers + numLandscapers) {
            if(Strategium.enemyHQLocation != null) {
                Direction dirToEnemyHQ = rc.getLocation().directionTo(Strategium.enemyHQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemyHQ.opposite()));
                for(Direction dir : towards)
                    if(tryBuild(RobotType.LANDSCAPER, dir)) {
                        ++numLandscapers;
                        return;
                    }
            }
            for (Direction buildDirection : DesignSchoolSensor.priorityBuildDirections)
                if (tryBuild(RobotType.LANDSCAPER, buildDirection)) {
                    ++numLandscapers;
                    return;
                }
            for (Direction dir : dir8) {
                if (tryBuild(RobotType.LANDSCAPER, dir)) {
                    ++numLandscapers;
                    return;
                }
            }

        } else if (RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost <= rc.getTeamSoup() ||
                ((numLandscapers < 5 || rc.getRoundNum() % 50 == 0) &&
                        rc.getTeamSoup() > 2 * RobotType.LANDSCAPER.cost)) {
            if(Strategium.enemyHQLocation != null) {
                Direction dirToEnemyHQ = rc.getLocation().directionTo(Strategium.enemyHQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemyHQ.opposite()));
                for(Direction dir : towards)
                    if(tryBuild(RobotType.LANDSCAPER, dir)) {
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
        }

    }
}




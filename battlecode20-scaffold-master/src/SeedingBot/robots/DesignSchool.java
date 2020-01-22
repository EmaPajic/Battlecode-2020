package SeedingBot.robots;

import SeedingBot.sensors.DesignSchoolSensor;
import SeedingBot.utils.Navigation;
import SeedingBot.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import java.util.List;

import static SeedingBot.RobotPlayer.*;

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
                (numLandscapers < 5 && rc.getTeamSoup() > 2* RobotType.LANDSCAPER.cost)) {
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




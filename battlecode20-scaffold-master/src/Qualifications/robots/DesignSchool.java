package Qualifications.robots;

import Qualifications.sensors.DesignSchoolSensor;
import Qualifications.utils.Navigation;
import Qualifications.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import java.util.List;

import static Qualifications.RobotPlayer.*;

public class DesignSchool {

    public static int numLandscapers = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();

        if(DesignSchoolSensor.droneNearby && !DesignSchoolSensor.netGunNearby) return;
        
        if (DesignSchoolSensor.numThreats > DesignSchoolSensor.numLandscapers) {
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
            if(Strategium.HQLocation != null) {
                Direction dirToHQ = rc.getLocation().directionTo(Strategium.HQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToHQ.opposite()));
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
            if(Strategium.HQLocation != null) {
                Direction dirToHQ = rc.getLocation().directionTo(Strategium.HQLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToHQ.opposite()));
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




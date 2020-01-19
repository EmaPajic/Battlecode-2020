package Mark5.robots;

import Mark5.sensors.DesignSchoolSensor;
import Mark5.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

import static Mark5.RobotPlayer.*;

public class DesignSchool {

    public static int numLandscapers = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        
        if (DesignSchoolSensor.numThreats > DesignSchoolSensor.numLandscapers + numLandscapers) {
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
                (numLandscapers < 3 && rc.getTeamSoup() > 2* RobotType.LANDSCAPER.cost)) {
            for (Direction dir : dir8) {
                if (tryBuild(RobotType.LANDSCAPER, dir)) {
                    ++numLandscapers;
                    return;
                }
            }
        }

    }
}




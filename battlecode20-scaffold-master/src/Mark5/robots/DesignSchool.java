package Mark5.robots;

import Mark5.sensors.DesignSchoolSensor;
import Mark5.utils.Strategium;
import Mark5.RobotPlayer;
import battlecode.common.*;

import static Mark5.RobotPlayer.*;

public class DesignSchool {

    public static int numLandscapers = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();

        if(DesignSchoolSensor.numThreats > DesignSchoolSensor.numLandscapers){
        for(Direction buildDirection : DesignSchoolSensor.priorityBuildDirections)
            if (tryBuild(RobotType.LANDSCAPER, buildDirection)) {
                ++numLandscapers;
            } else {
                for (Direction dir : dir8) {
                    if (tryBuild(RobotType.LANDSCAPER, dir)) {
                        ++numLandscapers;
                        return;
                    }
                }
            }
        } else if (RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost > rc.getTeamSoup()){
            for (Direction dir : dir8) {
                if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                    ++numLandscapers;
                    return;
                }
            }
        }

    }
}




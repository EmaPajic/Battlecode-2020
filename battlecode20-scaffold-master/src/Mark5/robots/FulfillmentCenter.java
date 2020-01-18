package Mark5.robots;

import Mark5.utils.Strategium;
import Mark5.RobotPlayer;
import battlecode.common.*;

import static Mark5.RobotPlayer.*;

public class FulfillmentCenter {

    public enum DroneBuildingImportance {
        TAXI_NEEDED,
        PERIODIC_BUILDING
    }

    public static DroneBuildingImportance droneBuildingImportance = DroneBuildingImportance.PERIODIC_BUILDING;

    public static int numDrones = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        switch(droneBuildingImportance) {
            case TAXI_NEEDED:
                for (Direction dir : dir8) {
                    if(tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                        ++numDrones;
                        droneBuildingImportance = DroneBuildingImportance.PERIODIC_BUILDING;
                        return;
                    }
                }
                break;
            case PERIODIC_BUILDING:
                if (numDrones < 5 && rc.getTeamSoup() > 650)
                    for (Direction dir : dir8) {
                        if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                            ++numDrones;
                            return;
                        }
                    }
                break;
        }
    }
}

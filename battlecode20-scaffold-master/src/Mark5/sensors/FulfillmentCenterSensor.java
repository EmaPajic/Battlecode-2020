package Mark5.sensors;

import Mark5.RobotPlayer;
import Mark5.robots.FulfillmentCenter;
import Mark5.utils.Wall;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import javax.naming.directory.DirContext;

import java.awt.*;

import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;

public class FulfillmentCenterSensor {

    public static int[] adjacentRobotTurnCount = {0, 0, 0, 0, 0, 0, 0 ,0};
    public static int[] adjacentRobotTurnID = {-1, -1, -1, -1, -1, -1, -1, -1};

    public static void sense() throws GameActionException {

        // Sensing if someone needs a taxi
        if(rc.getType() == RobotType.FULFILLMENT_CENTER) {
            for (int i = 0; i < 8; ++i) {
                if(rc.canSenseLocation(rc.getLocation().add(RobotPlayer.dir8[i]))) {
                    RobotInfo robot = rc.senseRobotAtLocation(
                            rc.getLocation().add(RobotPlayer.dir8[i]));
                    if (robot != null)
                        if (!robot.type.isBuilding() && robot.getType() == RobotType.MINER && robot.getSoupCarrying() == 0) {
                            if (adjacentRobotTurnID[i] == robot.getID()) {
                                ++adjacentRobotTurnCount[i];
                            } else {
                                adjacentRobotTurnID[i] = robot.getID();
                                adjacentRobotTurnCount[i] = 1;
                            }
                        }
                }
            }
            for(int i = 0; i < 8; ++i) {
                if(adjacentRobotTurnCount[i] == 15) {
                    FulfillmentCenter.droneBuildingImportance =
                            FulfillmentCenter.DroneBuildingImportance.TAXI_NEEDED;
                }
            }
        }

    }

}

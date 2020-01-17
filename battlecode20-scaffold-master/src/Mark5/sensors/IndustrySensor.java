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

public class IndustrySensor {

    public static int[] adjacentRobotTurnCount = {0, 0, 0, 0, 0, 0, 0 ,0};
    public static int[] adjacentRobotTurnID = {-1, -1, -1, -1, -1, -1, -1, -1};

    public static void sense() throws GameActionException {

        if (HQLocation == null) {
            for (RobotInfo robot : rc.senseNearbyRobots()) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    HQLocation = robot.location;
                    Wall.init();
                    break;
                }

            }
        }

        if (HQLocation != null) {
            int landscaperCnt = 0;
            for(int i = 16; i-- >0;) if(RobotPlayer.rc.canSenseLocation(Wall.wall[i]))
                if(robotAt(Wall.wall[i]) == RobotType.LANDSCAPER) landscaperCnt++;

            shouldBuildLandscaper = landscaperCnt < 15;
            System.out.println("LANCPED:" + Wall.launchPad + " " + robotAt(Wall.launchPad));
            if(robotAt(Wall.launchPad) != null) shouldBuildLandscaper = false;

        } else {
            shouldBuildLandscaper = rc.getType() == RobotType.DESIGN_SCHOOL;
        }

        // Sensing if someone needs a taxi
        if(rc.getType() == RobotType.FULFILLMENT_CENTER) {
            for (int i = 0; i < 8; ++i) {
                RobotInfo robot = rc.senseRobotAtLocation(
                        rc.getLocation().add(RobotPlayer.dir8[i]));
                if (robot != null) if (!robot.type.isBuilding()) {
                    if(adjacentRobotTurnID[i] == robot.getID()) {
                        ++adjacentRobotTurnCount[i];
                    }
                    else {
                        adjacentRobotTurnID[i] = robot.getID();
                        adjacentRobotTurnCount[i] = 1;
                    }
                }
            }

            for(int i = 0; i < 8; ++i) {
                if(adjacentRobotTurnCount[i] == 10) {
                    FulfillmentCenter.droneBuildingImportance =
                            FulfillmentCenter.DroneBuildingImportance.TAXI_NEEDED;
                }
            }
        }

    }

}

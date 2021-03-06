package SeedingBot.sensors;

import SeedingBot.RobotPlayer;
import SeedingBot.robots.FulfillmentCenter;
import SeedingBot.utils.Strategium;
import SeedingBot.utils.Wall;
import battlecode.common.*;

import javax.naming.directory.DirContext;

import java.awt.*;

import static SeedingBot.RobotPlayer.rc;

import static SeedingBot.utils.Strategium.*;

public class FulfillmentCenterSensor {

    public static int[] adjacentRobotTurnCount = {0, 0, 0, 0, 0, 0, 0 ,0};
    public static int[] adjacentRobotTurnID = {-1, -1, -1, -1, -1, -1, -1, -1};

    public static boolean enemyBuildingsNearby = false;
    public static boolean friendlyDesignSchoolNearby = false;
    public static boolean friendlyFulfilmentCenterNearby = false;
    public static boolean friendlyBuriedBuildingNearby = false;
    public static boolean enemyNetGunsNearby = false;
    public static boolean enemyLandscapersNearby = false;
    public static boolean enemySoftNearby = false;
    public static boolean enemyDronesNearby = false;
    public static boolean friendlyDronesNearby = false;
    public static boolean enemyFulfillmentCenterNearby = false;
    public static boolean friendlyNetGunsNearby = false;
    public static MapLocation nearestEnemyLandscaperLocation = null;
    public static MapLocation nearestEnemyMinerLocation = null;
    public static MapLocation nearestRushMinerLocation = null;

    public static void senseNearbyUnits() {
        RobotInfo[] robots = rc.senseNearbyRobots();
        //System.println(robots.length);
        for (RobotInfo robot : robots) {
            if (robot.team == Strategium.myTeam) {

                switch (robot.type) {
                    case DESIGN_SCHOOL:
                        friendlyDesignSchoolNearby = true;
                        break;

                    case FULFILLMENT_CENTER:
                        friendlyFulfilmentCenterNearby = true;

                    case HQ:
                    case VAPORATOR:
                    case REFINERY:
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case NET_GUN:
                        if (rc.getLocation().distanceSquaredTo(robot.location) <= 15) friendlyNetGunsNearby = true;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case DELIVERY_DRONE:
                        friendlyDronesNearby = true;
                        break;

                }
            } else {

                switch (robot.type) {
                    case NET_GUN:
                        if (rc.getLocation().distanceSquaredTo(robot.location) <= 35) enemyNetGunsNearby = true;
                    case HQ:
                    case VAPORATOR:
                    case REFINERY:
                    case DESIGN_SCHOOL:
                        enemyBuildingsNearby = true;
                        break;
                    case FULFILLMENT_CENTER:
                        enemyFulfillmentCenterNearby = true;
                        break;
                    case DELIVERY_DRONE:
                        enemyDronesNearby = true;
                        break;
                    case LANDSCAPER:
                        enemyLandscapersNearby = true;
                        nearestEnemyLandscaperLocation = robot.location;
                        break;
                    case MINER:
                        nearestEnemyMinerLocation = robot.location;
                    case COW:
                        enemySoftNearby = true;
                        break;
                }

            }
        }
    }
    public static void sense() throws GameActionException {
        senseNearbyUnits();
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
                if(adjacentRobotTurnCount[i] == 5) {
                    nearestRushMinerLocation = rc.getLocation().add(RobotPlayer.dir8[i]);
                    FulfillmentCenter.droneBuildingImportance =
                            FulfillmentCenter.DroneBuildingImportance.TAXI_NEEDED;
                }
            }
        }

    }

}

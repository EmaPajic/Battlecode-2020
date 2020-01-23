package Mark5.sensors;

import Mark5.robots.TwoMinerController;
import Mark5.utils.Blockchain;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import Mark5.utils.Wall;
import battlecode.common.*;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.*;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class DroneSensor {

    public static DroneSensor strategium = null;
    public static RobotInfo potentialTaxiPayload = null;

    public static void init() {

        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        robotsMet = new boolean[GameConstants.MAX_ROBOT_ID];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];

    }

    public static void sense() throws GameActionException {

        enemyUnits.clear();
        enemyDrones.clear();
        alliedDrones.clear();
        nearestEnemyDrone = null;
        if(nearestEnemyUnit != null)
            if(rc.canSenseLocation(nearestEnemyUnit.location)) nearestEnemyUnit = null;
        blockedUnit = null;
        blockingUnit = null;
        nearestLandscaper = null;
        nearestMiner = null;
        nearestPayload = null;

        int xMin = max(0, rc.getLocation().x - 4);
        int yMin = max(0, rc.getLocation().y - 4);
        int xMax = min(rc.getLocation().x + 4, rc.getMapWidth() - 1);
        int yMax = min(rc.getLocation().y + 4, rc.getMapHeight() - 1);
        for (int i = xMin; i <= xMax; i++)
            for (int j = yMin; j <= yMax; j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    explored[i][j] = true;
                    elevation[i][j] = rc.senseElevation(location);
                    occupied[i][j] = false;
                    if (rc.senseFlooding(location)) {
                        if (!water[i][j]) {
                            water[i][j] = true;
                            foundWater = true;
                        }
                    } else {

                        water[i][j] = false;
                        if (location.equals(nearestWater))
                            nearestWater = null;

                    }
                }
            }


        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {

            occupied[robot.location.x][robot.location.y] = true;

            if (robot.team == myTeam) {
                switch (robot.type) {
                    case HQ:
                        if (HQLocation == null) {
                            HQLocation = robot.location;
                            //TwoMinerController.searchRoute.add(0, HQLocation);
                            Wall.init();
                            Strategium.updatePotentialEnemyHQLocations();
                        }
                        break;
                    case MINER:
                        /*
                        boolean NearFulfillmentCenter = false;
                        for (Direction dir : dir8) {
                            if (rc.canSenseLocation(robot.location.add(dir))) {
                                RobotInfo maybeFulfillment = rc.senseRobotAtLocation(robot.location.add(dir));
                                if (maybeFulfillment != null)
                                    if (maybeFulfillment.type == RobotType.FULFILLMENT_CENTER &&
                                            maybeFulfillment.getTeam() == myTeam) {
                                        NearFulfillmentCenter = true;
                                        break;
                                    }
                            }
                        }
                        boolean isRushMiner = true;
                        if (robot.getSoupCarrying() > 0)
                            isRushMiner = false;
                        if (NearFulfillmentCenter && isRushMiner)
                            potentialTaxiPayload = robot;
                        */
                        if(enemyHQLocation != null) {
                            if(Navigation.aerialDistance(robot.location, enemyHQLocation) <= 2) {
                                break;
                            }
                        }
                        if(nearestMiner == null)
                            nearestMiner = robot;
                        else
                        if(Navigation.aerialDistance(nearestMiner) > Navigation.aerialDistance(robot)) {
                            nearestMiner = robot;
                        }

                        // test for blockage
                        int cnt = 0;
                        for(Direction dir : dir8) {
                            MapLocation adjacentLoc = robot.location.add(dir);
                            if(rc.canSenseLocation(adjacentLoc)) {
                                if(rc.senseFlooding(adjacentLoc) ||
                                        Math.abs(rc.senseElevation(robot.location) - rc.senseElevation(adjacentLoc)) > 3)
                                    ++cnt;
                            }
                        }
                        if(cnt >= 7)
                            blockedUnit = robot;
                        break;
                    case LANDSCAPER:
                        if(enemyHQLocation != null) {
                            if(Navigation.aerialDistance(robot.location, enemyHQLocation) <= 2) {
                                break;
                            }
                        }
                        if(Navigation.aerialDistance(robot.location, HQLocation) > 1) {
                            if(nearestLandscaper == null)
                                nearestLandscaper = robot;
                            else
                                if(Navigation.aerialDistance(nearestLandscaper) > Navigation.aerialDistance(robot)) {
                                    nearestLandscaper = robot;
                                }
                        }

                        // test for blockage
                        int cntL = 0;
                        if(blockedUnit != null || Navigation.aerialDistance(robot.location, HQLocation) == 1)
                            break;
                        for(Direction dir : dir8) {
                            MapLocation adjacentLoc = robot.location.add(dir);
                            if(rc.canSenseLocation(adjacentLoc)) {
                                if(rc.senseFlooding(adjacentLoc) ||
                                        Math.abs(rc.senseElevation(robot.location) - rc.senseElevation(adjacentLoc)) > 3)
                                    ++cntL;
                            }
                        }
                        if(cntL >= 7)
                            blockedUnit = robot;
                        break;
                }
            } else {

                switch (robot.type) {

                    case HQ:
                        if (enemyHQLocation == null) {
                            enemyHQLocation = robot.location;
                            potentialEnemyHQLocations.clear();
                            enemyNetGuns.add(robot.location);
                            enemyBuildings.add(robot.location);
                            Blockchain.reportEnemyHQLocation(2);
                        }
                    case NET_GUN:

                        if (!enemyNetGuns.contains(robot.location)) enemyNetGuns.add(robot.location);

                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:

                        if (!enemyBuildings.contains(robot.location)) enemyBuildings.add(robot.location);
                        break;

                    case DELIVERY_DRONE:

                        enemyDrones.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                            nearestEnemyDrone = robot;
                        break;

                    default:
                        enemyUnits.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyUnit))
                            nearestEnemyUnit = robot;

                }

            }

        }

        if(nearestMiner != null)
            nearestPayload = nearestMiner;
        if(nearestLandscaper != null && nearestPayload == null)
            nearestPayload = nearestLandscaper;


        if (!rc.isReady() && (nearestWater == null || Navigation.frustration >= 100)) findWater();

    }

    public static void findWater() {
        if (foundWater)
            for (int i = rc.getMapWidth(); i-- > 0; )
                for (int j = rc.getMapHeight(); j-- > 0; ) {
                    if (water[i][j]) if (Navigation.aerialDistance(i, j) < Navigation.aerialDistance(nearestWater))
                        nearestWater = new MapLocation(i, j);
                }

        if (nearestWater != null) return;

        if (!potentialEnemyHQLocations.isEmpty())
            nearestWater = potentialEnemyHQLocations.get(
                    rand.nextInt(potentialEnemyHQLocations.size()));
        else nearestWater = new MapLocation(rand.nextInt(rc.getMapWidth()), rand.nextInt(rc.getMapHeight()));

    }

}

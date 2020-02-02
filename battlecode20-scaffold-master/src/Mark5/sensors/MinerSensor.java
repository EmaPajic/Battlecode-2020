package Mark5.sensors;

import Mark5.RobotPlayer;
import Mark5.robots.Miner;
import Mark5.utils.Blockchain;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import static Mark5.RobotPlayer.*;
import static Mark5.sensors.MinerSensor.*;
import static Mark5.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class MinerSensor {
    public static int visibleSoup;
    public static boolean seenWater = false;
    public static MapLocation vacantBuildSpot;
    public static boolean enemyBuildingsNearby = false;
    public static boolean friendlyBuriedBuildingNearby = false;
    public static boolean enemyNetGunsNearby = false;
    public static boolean enemyLandscapersNearby = false;
    public static boolean enemyDronesNearby = false;
    public static boolean friendlyDronesNearby = false;
    public static boolean enemyFulfillmentCenterNearby = false;
    public static boolean friendlyNetGunsNearby = false;
    public static boolean aroundEnemyHQ = false;
    public static boolean refineryNearby = false;
    public static MapLocation nearestNetGun = null;
    public static MapLocation nearestDesignSchool = null;
    public static MapLocation nearestFulfillmentCenter = null;
    public static MapLocation nearestVaporator = null;
    public static boolean designSchoolNearby = false;

    public static void init() {
        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];
    }

    public static void sense() throws GameActionException {

        enemyDrones.clear();
        nearestEnemyDrone = null;
        visibleSoup = 0;
        vacantBuildSpot = null;

        System.out.println("SENSING");


        int xMin = max(0, rc.getLocation().x - 5);
        int yMin = max(0, rc.getLocation().y - 5);
        int xMax = min(rc.getLocation().x + 5, rc.getMapWidth() - 1);
        int yMax = min(rc.getLocation().y + 5, rc.getMapHeight() - 1);

        for (int i = xMin; i <= xMax; ++i)
            for (int j = yMin; j <= yMax; ++j) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    occupied[i][j] = false;
                    // next line is only used for rush miner
                    //if (rc.senseFlooding(location)) seenWater = true;
                    if(rc.getRoundNum() <= 600) {
                        visibleSoup += rc.senseSoup(location);
                        if (rc.senseSoup(location) > 0) {
                            if (!soup[i][j]) {
                                ++knownSoup;
                                soup[i][j] = true;
                                if (Navigation.aerialDistance(rc.getLocation(), location) <
                                        Navigation.aerialDistance(rc.getLocation(), nearestSoup))
                                    nearestSoup = location;
                            }

                        } else if (soup[i][j]) {
                            soup[i][j] = false;
                            knownSoup--;
                            if (nearestSoup != null)
                                if (nearestSoup.x == i && nearestSoup.y == j){
                                    nearestSoup = null;
                                    for(Direction dir : dir8) {
                                        if(rc.senseSoup(location.add(dir))>0){
                                            nearestSoup = location.add(dir);
                                            break;
                                        }
                                    }
                                }
                        }
                    }
                }

            }

        System.out.println(Clock.getBytecodeNum());


        enemyBuildingsNearby = false;
        friendlyBuriedBuildingNearby = false;
        enemyNetGunsNearby = false;
        enemyLandscapersNearby = false;
        enemyDronesNearby = false;
        friendlyDronesNearby = false;
        enemyFulfillmentCenterNearby = false;
        friendlyNetGunsNearby = false;
        aroundEnemyHQ = false;
        refineryNearby = false;

        RobotInfo[] robots = rc.senseNearbyRobots();

        if(nearestDesignSchool != null)
            if(rc.canSenseLocation(nearestDesignSchool)) nearestDesignSchool = null;

        if(nearestFulfillmentCenter != null)
            if(rc.canSenseLocation(nearestFulfillmentCenter) || (nearestFulfillmentCenter.isAdjacentTo(HQLocation)) &&
            rc.getRoundNum() > 300)
                nearestFulfillmentCenter = null;

        if(nearestNetGun != null)
            if(rc.canSenseLocation(nearestNetGun)) nearestNetGun = Strategium.HQLocation;

        for (RobotInfo robot : robots) {
            occupied[robot.location.x][robot.location.y] = true;

            if (robot.team == myTeam) {
                switch (robot.type) {
                    case DESIGN_SCHOOL:
                        designSchoolNearby = true;
                        if(rc.senseElevation(robot.location) >= 5 || rc.getRoundNum() <= 600)
                            if(Navigation.aerialDistance(nearestDesignSchool) > Navigation.aerialDistance(robot))
                                nearestDesignSchool = robot.location;
                        break;

                    case FULFILLMENT_CENTER:
                        if(rc.senseElevation(robot.location) >= 5 || rc.getRoundNum() <= 600)
                            if(Navigation.aerialDistance(nearestFulfillmentCenter) > Navigation.aerialDistance(robot))
                                nearestFulfillmentCenter = robot.location;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case VAPORATOR:
                        if (Navigation.aerialDistance(nearestVaporator) > Navigation.aerialDistance(robot))
                            nearestVaporator = robot.location;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case REFINERY:
                        refineryNearby = true;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        if (!refineries.contains(robot.location)) refineries.add(robot.location);
                        break;

                    case HQ:
                        refineryNearby = true;
                        if (!refineries.contains(robot.location)) refineries.add(robot.location);
                        if (HQLocation == null) {
                            HQLocation = robot.location;
                            Strategium.updatePotentialEnemyHQLocations();
                        }
                    case NET_GUN:
                        if (rc.getLocation().distanceSquaredTo(robot.location) <= 15) friendlyNetGunsNearby = true;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        if (Navigation.aerialDistance(nearestNetGun) > Navigation.aerialDistance(robot))
                            nearestNetGun = robot.location;
                        break;
                    case DELIVERY_DRONE:
                        friendlyDronesNearby = true;
                        break;
                }
            } else {
                switch (robot.type) {
                    case FULFILLMENT_CENTER:
                        enemyFulfillmentCenterNearby = true;
                        enemyBuildingsNearby = true;
                        break;

                    case DELIVERY_DRONE:
                        enemyDronesNearby = true;
                        enemyDrones.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                            nearestEnemyDrone = robot;
                        break;

                    case HQ:
                        if (enemyHQLocation == null) {
                            potentialEnemyHQLocations.clear();
                            enemyHQLocation = robot.location;
                            currentEnemyHQTarget = enemyHQLocation;
                            Blockchain.reportEnemyHQLocation(2);
                        }
                        aroundEnemyHQ = true;
                    case NET_GUN:
                        if (rc.getLocation().distanceSquaredTo(robot.location) <= 35) enemyNetGunsNearby = true;
                    case VAPORATOR:
                    case REFINERY:
                    case DESIGN_SCHOOL:
                        enemyBuildingsNearby = true;
                        break;

                    case LANDSCAPER:
                        enemyLandscapersNearby = true;
                        break;
                }
            }
        }

        System.out.println(Clock.getBytecodeNum());

        if(minerType == Miner.MinerType.SEARCH && rc.getRoundNum() <= 600) {
            nearestRefinery = null;
            for (MapLocation refinery : refineries) {
                if (refinery.equals(HQLocation) && rc.getRoundNum() > 600)
                    continue;
                if (Navigation.aerialDistance(nearestRefinery, rc.getLocation()) >
                        Navigation.aerialDistance(refinery, rc.getLocation()))
                    nearestRefinery = refinery;
            }
            if (knownSoup > 0 && nearestSoup == null) scanAllSoup();
        } else {
            if(xMin % 2 == HQLocation.x % 2) xMin++;
            if(yMin % 2 == HQLocation.y % 2) yMin++;
            for (int i = xMin; i <= xMax; i+=2)
                for (int j = yMin; j <= yMax; j+=2)
                    if(!occupied[i][j] && (i!=0 || j!=0)) {
                        MapLocation location = new MapLocation(i, j);
                        if(!rc.canSenseLocation(location)) continue;
                        if(rc.senseFlooding(location)) continue;
                        if(rc.senseElevation(location) < 8) continue;
                        if(Math.abs(rc.senseElevation(location) - rc.senseElevation(rc.getLocation())) <= 3)
                            if(Navigation.aerialDistance(vacantBuildSpot) > Navigation.aerialDistance(location))
                                vacantBuildSpot = location;
                    }
        }

        System.out.println(Clock.getBytecodeNum());

    }

    private static void scanAllSoup() {
        if (knownSoup <= 0) {
            nearestSoup = null;
            return;
        }
        for (int i = rc.getMapWidth(); i-- > 0; )
            for (int j = rc.getMapHeight(); j-- > 0; )
                if (soup[i][j])
                    if(Navigation.aerialDistance(rc.getLocation(), i, j) <
                        Navigation.aerialDistance(rc.getLocation(), nearestSoup)) nearestSoup = new MapLocation(i, j);
    }
}

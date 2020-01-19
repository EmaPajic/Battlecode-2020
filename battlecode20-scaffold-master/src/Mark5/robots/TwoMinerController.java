package Mark5.robots;


import Mark5.sensors.MinerSensor;
import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static Mark5.RobotPlayer.*;

public class TwoMinerController {

    //    static class RobotsComparator implements Array<RobotInfo> {
//        @Override
//        public int compare(RobotInfo robA, RobotInfo robB){
//            if(staticRobots.contains(robA.type) && robA.team != Strategium.myTeam)
//                if(staticRobots.contains(robA.type) && robA.team != Strategium.myTeam)
//        }
//    }
    static class LocationComparator implements Comparator<MapLocation> {
        @Override
        public int compare(MapLocation locA, MapLocation locB) {
            return Integer.compare(
                    Navigation.aerialDistance(hqLocation, locA), Navigation.aerialDistance(hqLocation, locB));
        }
    }

    static ArrayList<MapLocation> searchRoute;
    static ArrayList<MapLocation> searchRouteVisited;

    static MapLocation currentTarget;
    static public ArrayList<RobotType> staticRobots;
    static public RobotType lastMadeRobotType;
    static public boolean triedToBuildRefinery;

    static boolean currentlyRefining;
    static int[] adjacencyCount = {0, 0, 0, 0, 0, 0, 0, 0};
    static int[] adjacencyID = {-1, -1, -1, -1, -1, -1, -1, -1};


    static void findRoute() {
        for (int currX = 4; currX <= rc.getMapWidth() - 6; currX++) {
            if (currX % 5 == 0) {
                for (int currY = 4; currY <= rc.getMapHeight() - 6; currY++) {
                    if (currY % 5 == 0) {
                        searchRoute.add(new MapLocation(currX, currY));

                    }
                }
            }
        }
    }


    public static void init() {
        staticRobots = new ArrayList<>(Arrays.asList(RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
                RobotType.FULFILLMENT_CENTER, RobotType.HQ, RobotType.NET_GUN, RobotType.REFINERY));
        searchRoute = new ArrayList<>();
        searchRouteVisited = new ArrayList<>();
        currentlyRefining = false;
        lastMadeRobotType = RobotType.DESIGN_SCHOOL;
        triedToBuildRefinery = false;
        findRoute();
        if (searchRoute.contains(hqLocation)) {
            searchRoute.remove(hqLocation);
            searchRouteVisited.add(hqLocation);
        }
        searchRoute.sort(new LocationComparator());
        currentTarget = searchRoute.get(0); // get the first location from sorted locations which are closest to HQ
        //System.println("Init je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

    }


    public static void updateTarget() {
        if (!searchRoute.isEmpty()) {
            Navigation.frustration = 0;
            searchRoute.remove(currentTarget);
            searchRouteVisited.add(currentTarget);
            currentTarget = searchRoute.get(0); // get the first elem
        }
    }

    @SuppressWarnings("unused")
    public static void checkRobotCollision() throws GameActionException {
        for (int dirIndex = 0; dirIndex < 8; ++dirIndex) {
            Direction dir = dir8[dirIndex];
            if (rc.canSenseLocation(rc.getLocation().add(dir))) {
                RobotInfo info = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                if (info != null) {
                    if (info.getID() == adjacencyID[dirIndex]) {
                        ++adjacencyCount[dirIndex];
                    } else {
                        adjacencyCount[dirIndex] = 1;
                        adjacencyID[dirIndex] = info.getID();
                    }
                } else {
                    adjacencyCount[dirIndex] = 0;
                    adjacencyID[dirIndex] = -1;
                }
            }
        }
        for (int ind = 0; ind < 8; ++ind) {
            if (adjacencyCount[ind] > 50) {
                for (Direction awayDir : dir8) {
                    if (tryMove(awayDir)) {
                        adjacencyCount[ind] = 0;
                        return;
                    }
                }
            }
        }
    }


    public static void updateEnemyHQTarget() {
        if (!Strategium.potentialEnemyHQLocations.isEmpty()) {
            Strategium.potentialEnemyHQLocations.remove(Strategium.currentEnemyHQTarget);
            Strategium.currentEnemyHQTarget = Strategium.potentialEnemyHQLocations.get(0);
        }
    }

    public static boolean buildDesignCenterNearEnemy() throws GameActionException {
        if (Navigation.aerialDistance(Strategium.enemyHQLocation) <= 3) {
            for (Direction dir : dir8) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) &&
                        Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.enemyHQLocation) <= 2) {
                    if (tryBuild(RobotType.DESIGN_SCHOOL, dir)) return true;
                }
            }
        }
        Navigation.bugPath(Strategium.enemyHQLocation);
        return false;

    }


    public static boolean refineryRentability() throws GameActionException {
        if (Navigation.aerialDistance(rc.getLocation(), Strategium.nearestRefinery) > 7 &&
                Navigation.aerialDistance(hqLocation, rc.getLocation()) > 4) {

            if (MinerSensor.visibleSoup > RobotType.REFINERY.cost && rc.getTeamSoup() > RobotType.REFINERY.cost) {
                for (Direction dir : dir8) {
                    if (rc.canBuildRobot(RobotType.REFINERY, dir))
                        if (Lattice.isBuildingSite(rc.adjacentLocation(dir))) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                            return true;
                        }

                }
                //System.println("RefineryRentabilityje potrosio: " + (Clock.getBytecodeNum() - startByteCodes));
            }
        }
        //System.println("refineryRentability je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

        return false;


    }

    public static boolean mineAndRefine() throws GameActionException {

        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            for (Direction dir : Direction.allDirections())
                if (rc.canMineSoup(dir)) {
                    rc.mineSoup(dir);
                    return true;
                }
        } else {
            if (Navigation.aerialDistance(Strategium.nearestSoup)
                    < Navigation.aerialDistance(Strategium.nearestRefinery)
                    && Navigation.aerialDistance(Strategium.nearestRefinery) > 6) return refineryRentability();
            if (Navigation.aerialDistance(Strategium.nearestRefinery) == 1) {
                for (Direction dir : Direction.allDirections())
                    if (rc.canDepositSoup(dir)) {
                        rc.depositSoup(dir, rc.getSoupCarrying());
                        return true;
                    }
            }
        }
        return false;
    }

    public static void control() throws GameActionException {
        Strategium.gatherInfo();
        //System.println("Current target : " + currentTarget);
        if (rc.canSenseLocation(currentTarget) || Navigation.frustration >= 50) {
            //System.println("Update target");
            updateTarget();
        }

        if (mineAndRefine()) return;
        //System.println("Ima dovoljno supe");

        boolean enemyBuildingsNearby = false;
        boolean friendlyDesignSchoolNearby = false;
        boolean friendlyFulfilmentCenterNearby = false;
        boolean friendlyBuriedBuildingNearby = false;
        boolean enemyNetGunsNearby = false;
        boolean enemyLandscapersNearby = false;
        boolean enemySoftNearby = false;
        boolean enemyDronesNearby = false;
        boolean friendlyDronesNearby = false;
        boolean enemyFulfillmentCenterNearby = false;
        boolean friendlyNetGunsNearby = false;

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
                        break;
                    case COW:
                    case MINER:
                        enemySoftNearby = true;
                        break;
                }

            }
        }


        RobotType makeRobotType = null;

        if (!friendlyDesignSchoolNearby) {
            if ((enemyBuildingsNearby || enemyLandscapersNearby || friendlyBuriedBuildingNearby ||
                    Navigation.aerialDistance(Strategium.HQLocation) <= 3) && rc.getTeamSoup() >= 300)
                makeRobotType = RobotType.DESIGN_SCHOOL;
        }

        if (rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.DESIGN_SCHOOL.cost) {

            if (makeRobotType == null && !friendlyNetGunsNearby &&
                    (enemyDronesNearby || enemyFulfillmentCenterNearby)) {
                makeRobotType = RobotType.NET_GUN;
            }

            if (!friendlyFulfilmentCenterNearby && !enemyNetGunsNearby && !friendlyDronesNearby) {
                if (makeRobotType == null && (enemyLandscapersNearby)) {
                    makeRobotType = RobotType.FULFILLMENT_CENTER;
                }
            }


            if (makeRobotType == null) {

                // stavi da pravi design school u ovom else-u dole ako ima mogucnost a nema ga u okolini baze
                // situacija kada ima okolnih nasih robota a ima mogucnost da pravi nije pokrivena
                // if soup < 1000 make vaporator
                if (rc.getTeamSoup() < 1000) {
                    makeRobotType = RobotType.VAPORATOR;
                    //System.println("Imamo dovoljno novca za Vaporator");
                } else {
                    if (lastMadeRobotType == RobotType.DESIGN_SCHOOL) {
                        lastMadeRobotType = RobotType.FULFILLMENT_CENTER;
                        makeRobotType = RobotType.FULFILLMENT_CENTER;
                        // if its not in visible radius make altern design school and fulfilment center if soup > 1000
                    } else {
                        lastMadeRobotType = RobotType.DESIGN_SCHOOL;
                        makeRobotType = RobotType.DESIGN_SCHOOL;
                    }
                }

            }
            System.out.println(makeRobotType);
            if (rc.getTeamSoup() >= makeRobotType.cost)
                for (Direction dir : dir8)
                    if (rc.canBuildRobot(makeRobotType, dir))
                        if (Lattice.isBuildingSite(rc.adjacentLocation(dir))) {
                            rc.buildRobot(makeRobotType, dir);
                            return;
                        }
        }

        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            if (Strategium.nearestSoup != null) Navigation.bugPath(Strategium.nearestSoup);
            else Navigation.bugPath(currentTarget);
        } else {
            Navigation.bugPath(Strategium.nearestRefinery);
        }
    }


//


}


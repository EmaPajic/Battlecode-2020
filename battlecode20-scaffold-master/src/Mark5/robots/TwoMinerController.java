package Mark5.robots;


import Mark5.sensors.MinerSensor;
import Mark5.utils.Blockchain;
import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static Mark5.RobotPlayer.*;
import static java.lang.Integer.min;
import static java.util.Collections.swap;

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
    public static int currentTargetIndex;
    static public ArrayList<RobotType> staticRobots;
    static public RobotType lastMadeRobotType;
    static public boolean triedToBuildRefinery;

    static boolean currentlyRefining;
    static int[] adjacencyCount = {0, 0, 0, 0, 0, 0, 0, 0};
    static int[] adjacencyID = {-1, -1, -1, -1, -1, -1, -1, -1};


    static void findRoute() {
        int stepX = (rc.getMapWidth() - 10) / 3;
        int stepY = (rc.getMapWidth() - 10) / 3;
        for (int currX = 4; currX <= rc.getMapWidth() - 1; currX += stepX) {
            for (int currY = 4; currY <= rc.getMapHeight() - 1; currY += stepY) {
                searchRoute.add(new MapLocation(currX, currY));
            }
        }

    }

    public static void randomizeBit() {
        for(int i = 0; i < min(searchRoute.size() - 2, 2); ++i) {
            for(int j = i + 1; j < min(searchRoute.size(), i + 3); ++j) {
                if(Strategium.rand.nextInt() % 3 < 2)
                    swap(searchRoute, i, j);
            }
        }
    }

    public static void nearestNeighbor() {
        for(int i = 1; i < searchRoute.size() - 1; ++i) {
            int bestIndex = -1;
            int bestDistance = 1000;
            for(int j = i + 1; j < searchRoute.size(); ++j) {
                if(Navigation.aerialDistance(searchRoute.get(i), searchRoute.get(j)) < bestDistance) {
                    bestIndex = j;
                    bestDistance = Navigation.aerialDistance(searchRoute.get(i), searchRoute.get(j));
                }
            }
            swap(searchRoute, i + 1, bestIndex);
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
        randomizeBit();
        nearestNeighbor();
        currentTarget = searchRoute.get(0); // get the first location from sorted locations which are closest to HQ
        //System.println("Init je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

    }


    public static void updateTarget() {
        if (!searchRoute.isEmpty()) {
            Navigation.frustration = 0;
            currentTargetIndex = (currentTargetIndex + 1) % searchRoute.size();
            currentTarget = searchRoute.get(currentTargetIndex); // get the first elem
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
//        if( Strategium.turnsAlive <= 10){
////            int[] msgs = new int();
////            Transation msgs = new Transaction(42, )
//            if(Strategium.nearestRefinery != Strategium.HQLocation){
//                currentTarget = Strategium.nearestRefinery;
//            }
//
//        }

        if (rc.canSenseLocation(currentTarget) || Navigation.frustration >= 50) {
            // trosi 60 bajtkoda
            updateTarget();

        }
//        int byteCodeUsed = Clock.getBytecodeNum();
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
                    Navigation.aerialDistance(Strategium.HQLocation) <= 3) && rc.getTeamSoup() >= 250)
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
                if (rc.getTeamSoup() < 700) {
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
                        if(makeRobotType != RobotType.VAPORATOR ||
                                Strategium.elevation[rc.adjacentLocation(dir).x][rc.adjacentLocation(dir).y] > 5)
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


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
import java.util.List;

import static Mark5.RobotPlayer.*;
import static java.lang.Integer.min;
import static java.lang.Math.max;
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

    public static ArrayList<MapLocation> searchRoute;
    static ArrayList<MapLocation> searchRouteVisited;

    static MapLocation currentTarget;
    public static int currentTargetIndex = 0;
    static public ArrayList<RobotType> staticRobots;
    static public RobotType lastMadeRobotType;
    static public boolean triedToBuildRefinery;

    static boolean currentlyRefining;
    static int[] adjacencyCount = {0, 0, 0, 0, 0, 0, 0, 0};
    static int[] adjacencyID = {-1, -1, -1, -1, -1, -1, -1, -1};

    static int buildRadius = 0;
    static MapLocation buildWaypoint;

    static MapLocation nearestNetGun = null;
    static MapLocation nearestDesignSchool = null;
    static MapLocation nearestFulfillmentCenter = null;
    static boolean haveVaporator = false;

    static int factoriesBuilt;

    public static MapLocation pastLocation = null;
    public static boolean tryToReturn = false;



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
        for (int i = 0; i < min(searchRoute.size() - 2, 2); ++i) {
            for (int j = i + 1; j < min(searchRoute.size(), i + 3); ++j) {
                if (Strategium.rand.nextInt() % 3 < 2)
                    swap(searchRoute, i, j);
            }
        }
    }

    public static void nearestNeighbor() {
        for (int i = 1; i < searchRoute.size() - 1; ++i) {
            int bestIndex = -1;
            int bestDistance = 1000;
            for (int j = i + 1; j < searchRoute.size(); ++j) {
                if (Navigation.aerialDistance(searchRoute.get(i), searchRoute.get(j)) < bestDistance) {
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

    public static boolean buildNetGunNearEnemy() throws GameActionException {


            for (Direction dir : dir8) {
                if (rc.canBuildRobot(RobotType.NET_GUN, dir) &&
                        Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3)
                    if (tryBuild(RobotType.NET_GUN, dir)) {
                        return true;
                    }
            }
        return false;
    }

    public static void control() throws GameActionException {
        Strategium.gatherInfo();

        if(rc.getRoundNum() <= 600) {
            if (rc.canSenseLocation(currentTarget) || Navigation.frustration >= 50) {

                updateTarget();

            }

            if (mineAndRefine()) return;
        }

        System.out.println("Nisam crko");


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
        boolean aroundEnemyHQ = false;

        RobotInfo[] robots = rc.senseNearbyRobots();
        //System.println(robots.length);

        if(nearestDesignSchool != null)
            if(rc.canSenseLocation(nearestDesignSchool)) nearestDesignSchool = null;

        if(nearestFulfillmentCenter != null)
            if(rc.canSenseLocation(nearestFulfillmentCenter)) nearestFulfillmentCenter = null;

        if(nearestNetGun != null)
            if(rc.canSenseLocation(nearestNetGun)) nearestNetGun = Strategium.HQLocation;

        for (RobotInfo robot : robots) {
            if (robot.team == Strategium.myTeam) {

                switch (robot.type) {
                    case DESIGN_SCHOOL:
                        friendlyDesignSchoolNearby = true;
                        if(rc.senseElevation(robot.location) >= 5 || rc.getRoundNum() <= 600)
                        if(Navigation.aerialDistance(nearestDesignSchool) > Navigation.aerialDistance(robot))
                            nearestDesignSchool = robot.location;
                        break;

                    case FULFILLMENT_CENTER:
                        friendlyFulfilmentCenterNearby = true;
                        if(rc.senseElevation(robot.location) >= 5 || rc.getRoundNum() <= 600)
                        if(Navigation.aerialDistance(nearestFulfillmentCenter) > Navigation.aerialDistance(robot))
                            nearestFulfillmentCenter = robot.location;
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case VAPORATOR:
                        haveVaporator = true;
                    case REFINERY:
                        if (robot.dirtCarrying > 0) friendlyBuriedBuildingNearby = true;
                        break;

                    case HQ:
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
                    case HQ:
                        aroundEnemyHQ = true;
                    case NET_GUN:
                        if (rc.getLocation().distanceSquaredTo(robot.location) <= 35) enemyNetGunsNearby = true;

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

        System.out.println(rc.getTeamSoup());

        if (aroundEnemyHQ && rc.getRoundNum() > 1500 && enemyDronesNearby && !friendlyNetGunsNearby) {
            buildNetGunNearEnemy();
        }

        if (rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.LANDSCAPER.cost ||
                Navigation.aerialDistance(Strategium.HQLocation) <= 3) {

            System.out.println("DOVOLJNO ZA GRADNJU");

        if (Navigation.aerialDistance(nearestDesignSchool) > 8) {
            if (enemyBuildingsNearby || enemyLandscapersNearby || friendlyBuriedBuildingNearby ||
                    Navigation.aerialDistance(Strategium.HQLocation) <= 3 ||
                    (Navigation.aerialDistance(nearestDesignSchool) > 20 && haveVaporator))
                makeRobotType = RobotType.DESIGN_SCHOOL;
        }

            if (makeRobotType == null && !friendlyNetGunsNearby &&
                    (enemyDronesNearby || enemyFulfillmentCenterNearby)
            ) {
                makeRobotType = RobotType.NET_GUN;
            }

            if (Navigation.aerialDistance(nearestFulfillmentCenter) > 8 &&
                    !enemyNetGunsNearby && !friendlyDronesNearby) {
                if (makeRobotType == null && (Navigation.aerialDistance(Strategium.HQLocation) <= 3 ||
                        (Navigation.aerialDistance(nearestFulfillmentCenter) > 20 && haveVaporator))) {
                    makeRobotType = RobotType.FULFILLMENT_CENTER;
                }
            }


            if (makeRobotType == null) {

                // stavi da pravi design school u ovom else-u dole ako ima mogucnost a nema ga u okolini baze
                // situacija kada ima okolnih nasih robota a ima mogucnost da pravi nije pokrivena
                // if soup < 1000 make vaporator
                makeRobotType = RobotType.VAPORATOR;
                /*if (rc.getTeamSoup() < 700) {
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
                }*/

            }
            System.out.println(makeRobotType);
            if (rc.getTeamSoup() >= makeRobotType.cost && rc.getRoundNum() < 1200)
                for (Direction dir : dir8)
                    if (rc.canBuildRobot(makeRobotType, dir))
                        if (makeRobotType != RobotType.VAPORATOR ||
                                rc.senseElevation(rc.adjacentLocation(dir)) >= 5)
                            if (Lattice.isBuildingSite(rc.adjacentLocation(dir))) {
                                if(rc.getRoundNum() <= 600 || rc.senseElevation(rc.adjacentLocation(dir)) >= 5)
                                rc.buildRobot(makeRobotType, dir);
                                return;
                            }
        }

        if(Strategium.nearestEnemyDrone != null) rc.setIndicatorLine(rc.getLocation(),
                Strategium.nearestEnemyDrone.location,
                255, 0, 0);

        if(nearestNetGun != null) rc.setIndicatorLine(rc.getLocation(), nearestNetGun, 0, 255, 0);

        if(Strategium.nearestEnemyDrone != null && rc.getLocation().distanceSquaredTo(nearestNetGun) >= 8)
            if(Navigation.fuzzyNav(nearestNetGun)) return;

        if (rc.getRoundNum() <= 600) {
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                tryToReturn = false;
                pastLocation = rc.getLocation();

                if (Strategium.nearestSoup != null) Navigation.bugPath(Strategium.nearestSoup);
                else Navigation.bugPath(currentTarget);
            } else {
                if (Strategium.nearestRefinery != null){
                    if(pastLocation != null && !tryToReturn && hqLocation != null) {
                        List<Direction> dir = Navigation.moveAwayFrom(rc.getLocation().add(
                                rc.getLocation().directionTo(Strategium.nearestRefinery).opposite()));

                        System.out.println(dir);

                        Direction pastDir = pastLocation.directionTo((rc.getLocation()));
                        System.out.println(pastDir);
                        System.out.println("Prethodna lok " + pastLocation + "Trenu lokacija" + rc.getLocation());
                        if(!dir.contains(pastDir) && pastDir != Direction.CENTER) {

                            System.out.println("Hallelujah, country roadds");
//                            rc.move(pastDir);
                            tryToReturn = true;
                            System.out.println(tryToReturn + "pokusao sam ka " + pastDir);
                            return;
                        }

                    }


                    if(tryToReturn){
                        pastLocation = rc.getLocation();
                        Navigation.rbugPath(Strategium.nearestRefinery);
                    }
                    else {
                        pastLocation = rc.getLocation();
                        Navigation.bugPath(Strategium.nearestRefinery);
                    }

                }
                else {
                    pastLocation = rc.getLocation();
                    Navigation.bugPath(currentTarget);
                }
//                pastLocation = rc.getLocation();

            }
        } else {
            if(rc.getRoundNum() % 100 == 1) {
                buildRadius += 5;
                int xMin = max(0, Strategium.HQLocation.x - buildRadius);
                int yMin = max(0, Strategium.HQLocation.y - buildRadius);
                int xMax = Math.min(Strategium.HQLocation.x + buildRadius, rc.getMapWidth() - 1);
                int yMax = Math.min(Strategium.HQLocation.y + buildRadius, rc.getMapHeight() - 1);
                buildWaypoint =
                        new MapLocation(Strategium.rand.nextInt(xMax - xMin) + xMin,
                                Strategium.rand.nextInt(yMax - yMin) + yMin);
            }
            if(MinerSensor.vacantBuildSpot != null) {
                if (!rc.getLocation().isAdjacentTo(MinerSensor.vacantBuildSpot))
                    Navigation.bugPath(MinerSensor.vacantBuildSpot);
            } else Navigation.bugPath(buildWaypoint);

        }

    }


//


}


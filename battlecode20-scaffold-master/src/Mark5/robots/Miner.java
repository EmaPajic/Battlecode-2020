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
import static Mark5.sensors.MinerSensor.*;

public class Miner {

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
                    Navigation.aerialDistance(Strategium.HQLocation, locA),
                    Navigation.aerialDistance(Strategium.HQLocation, locB));
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


    static boolean haveVaporator = false;

    static int factoriesBuilt;

    public static MapLocation pastLocation = null;
    public static boolean tryToReturn = false;

    public enum MinerType {
        RUSH,
        SEARCH
    }

    static void findRoute() {
        int stepX = (rc.getMapWidth() - 10) / 3;
        int stepY = (rc.getMapHeight() - 10) / 3;
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
        if (searchRoute.contains(Strategium.HQLocation)) {
            searchRoute.remove(Strategium.HQLocation);
            searchRouteVisited.add(Strategium.HQLocation);
        }
        searchRoute.sort(new LocationComparator());
        randomizeBit();
        nearestNeighbor();
        currentTarget = searchRoute.get(0); // get the first location from sorted locations which are closest to HQ
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
                if ( Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.enemyHQLocation) <= 2 ) {
                    if (tryBuild(RobotType.DESIGN_SCHOOL, dir)) return true;
                }
            }
        }
        Navigation.bugPath(Strategium.enemyHQLocation);
        return false;

    }


    public static boolean refineryRentability() throws GameActionException {
        if (Navigation.aerialDistance(Strategium.nearestRefinery) > 7 &&
                Navigation.aerialDistance(Strategium.HQLocation) > 4) {

            if (MinerSensor.visibleSoup > RobotType.REFINERY.cost && rc.getTeamSoup() > RobotType.REFINERY.cost) {
                for (Direction dir : dir8) {
                        if (Lattice.isBuildingSite(rc.adjacentLocation(dir)))
                            if(tryBuild(RobotType.REFINERY, dir)) return true;
                }
            }
        }
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
                if ( Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3 )
                    if (tryBuild(RobotType.NET_GUN, dir)) {
                        return true;
                    }
            }
        return false;
    }

    public static boolean buildDesignSchoolNearEnemy() throws GameActionException {
        for (Direction dir : dir8) {
            if ( Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3 )
                if (tryBuild(RobotType.DESIGN_SCHOOL, dir)) {
                    return true;
                }
        }
        return false;
    }
    public static void control() throws GameActionException {
        Strategium.gatherInfo();

        if(buildWaypoint == null && Strategium.HQLocation != null)
            buildWaypoint = Strategium.HQLocation;

        if(rc.getRoundNum() <= 600) {
            if (rc.canSenseLocation(currentTarget) || Navigation.frustration >= 50) {

                updateTarget();

            }
            if(Strategium.nearestEnemyDrone != null && rc.getLocation().distanceSquaredTo(nearestNetGun) >= 8)
                if(Navigation.fleeToSafety(Strategium.nearestEnemyDrone.location, nearestNetGun)) return;
            if (mineAndRefine()) return;
        }

        RobotType makeRobotType = null;

        if (aroundEnemyHQ && rc.getRoundNum() > 1300 && enemyDronesNearby && !friendlyNetGunsNearby) {
            buildNetGunNearEnemy();
        }
        if (aroundEnemyHQ && rc.getRoundNum() > 1300 && !designSchoolNearby) {
            buildDesignSchoolNearEnemy();
        }

        /*if(Strategium.nearestEnemyDrone != null && rc.getLocation().distanceSquaredTo(nearestNetGun) >= 8) {
            if (!Strategium.nearestEnemyDrone.currentlyHoldingUnit)
            if (Navigation.fuzzyNav(nearestNetGun)) return;
        }*/

        if (rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.LANDSCAPER.cost ||
                Navigation.aerialDistance(Strategium.HQLocation) <= 3) {


        if (Navigation.aerialDistance(nearestDesignSchool) > 10)
        {
            if (enemyBuildingsNearby || enemyLandscapersNearby || friendlyBuriedBuildingNearby ||
                    Navigation.aerialDistance(Strategium.HQLocation) < 3 ||
                    /*(Navigation.aerialDistance(Strategium.HQLocation) <= 3  && rc.getRoundNum() > 150)*/ //||
                    (Navigation.aerialDistance(nearestDesignSchool) > 20 && Navigation.aerialDistance(nearestVaporator)
                    < 8))
                makeRobotType = RobotType.DESIGN_SCHOOL;
        }

            if (!friendlyNetGunsNearby &&
                    (enemyDronesNearby || enemyFulfillmentCenterNearby) &&
                    (rc.senseElevation(rc.getLocation()) >= 5 || refineryNearby || MinerSensor.visibleSoup > 250)) {
                makeRobotType = RobotType.NET_GUN;
            }

            if (Navigation.aerialDistance(nearestFulfillmentCenter) > 8 &&
                    !enemyNetGunsNearby && !friendlyDronesNearby) {
                if (makeRobotType == null && (Navigation.aerialDistance(Strategium.HQLocation) < 2 ||
                        (Navigation.aerialDistance(nearestFulfillmentCenter) > 20 &&
                                Navigation.aerialDistance(nearestVaporator)
                                < 8))) {
                    makeRobotType = RobotType.FULFILLMENT_CENTER;
                }
            }

            if (makeRobotType == null) {

                makeRobotType = RobotType.VAPORATOR;

            }
            //System.out.println(makeRobotType);
            if (rc.getTeamSoup() >= makeRobotType.cost && rc.getRoundNum() < 1200)
                for (Direction dir : dir8)
                    if (rc.canBuildRobot(makeRobotType, dir))
                        if (makeRobotType != RobotType.VAPORATOR ||
                                rc.senseElevation(rc.adjacentLocation(dir)) >= 5 || rc.getRoundNum()<150)
                            if (Lattice.isBuildingSite(rc.adjacentLocation(dir)) ||
                                    (rc.adjacentLocation(dir).isAdjacentTo(Strategium.HQLocation) &&
                                            rc.getRoundNum() < 150) && makeRobotType == RobotType.FULFILLMENT_CENTER) {
                                if(rc.getRoundNum() <= 600 || rc.senseElevation(rc.adjacentLocation(dir)) >= 5)
                                rc.buildRobot(makeRobotType, dir);
                                return;
                            }
        }

        if(Strategium.nearestEnemyDrone != null) rc.setIndicatorLine(rc.getLocation(),
                Strategium.nearestEnemyDrone.location,
                255, 0, 0);

        if(nearestNetGun != null) rc.setIndicatorLine(rc.getLocation(), nearestNetGun, 0, 255, 0);

        if(!(makeRobotType == RobotType.NET_GUN && rc.getTeamSoup() >= 250) &&
                Strategium.nearestEnemyDrone != null && rc.getLocation().distanceSquaredTo(nearestNetGun) >= 8) {

            if (Navigation.fleeToSafety(Strategium.nearestEnemyDrone.location, nearestNetGun)) return;
        }

        if (rc.getRoundNum() <= 600 && (rc.getRoundNum() < 200 ||
                GameConstants.getWaterLevel(rc.getRoundNum()) < rc.senseElevation(rc.getLocation()) ||
                Strategium.nearestSoup != null)) {
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                tryToReturn = false;
                pastLocation = rc.getLocation();

                if (Strategium.nearestSoup != null) Navigation.bugPath(Strategium.nearestSoup);
                else Navigation.bugPath(currentTarget);
            } else {
                if (Strategium.nearestRefinery != null){
                    if(pastLocation != null && !tryToReturn ) {
                        List<Direction> dir = Navigation.moveAwayFrom(rc.getLocation().add(
                                rc.getLocation().directionTo(Strategium.nearestRefinery).opposite()));

                        System.out.println(dir);

                        Direction pastDir = pastLocation.directionTo((rc.getLocation()));
                        if(!dir.contains(pastDir) && pastDir != Direction.CENTER) {
                            tryToReturn = true;
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

}


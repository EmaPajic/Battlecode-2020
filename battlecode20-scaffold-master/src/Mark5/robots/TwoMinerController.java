package Mark5.robots;

import Mark5.sensors.MinerSensor;
import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.awt.*;
import java.util.*;


import static Mark5.RobotPlayer.*;
import static Mark5.RobotPlayer.tryMove;
import static java.lang.Math.*;

public class TwoMinerController {


    static class LocationComparator implements Comparator<MapLocation> {
        @Override
        public int compare(MapLocation locA, MapLocation locB) {
            //noinspection ComparatorMethodParameterNotUsed
            if (Navigation.aerialDistance(hqLocation, locA) > Navigation.aerialDistance(hqLocation, locB))
                return 1;
            else if (Navigation.aerialDistance(hqLocation, locA) < Navigation.aerialDistance(hqLocation, locB))
                return -1;
            else
                return 0;
        }
    }

    static ArrayList<MapLocation> searchRoute;
    static ArrayList<MapLocation> searchRouteVisited;

    static MapLocation currentTarget;
    static public ArrayList<RobotType> staticRobots;

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
        staticRobots = new ArrayList<>(Arrays.asList(RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,  RobotType.FULFILLMENT_CENTER, RobotType.HQ, RobotType.NET_GUN, RobotType.REFINERY));
        searchRoute = new ArrayList<>();
        searchRouteVisited = new ArrayList<>();
        currentlyRefining = false;
        //currentTarget = rc.getLocation();
        findRoute();
        if (searchRoute.contains(hqLocation)) {
            searchRoute.remove(hqLocation);
            searchRouteVisited.add(hqLocation);
        }
        Collections.sort(searchRoute, new LocationComparator());
        currentTarget = searchRoute.get(0); // get the first location from sorted locations which are closest to HQ


    }


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

    public static void updateTarget() throws GameActionException {
        if (searchRoute.isEmpty()) {

        } else {
            Navigation.frustration = 0;
            searchRoute.remove(currentTarget);
            searchRouteVisited.add(currentTarget);
            currentTarget = searchRoute.get(0); // get the first elem
        }
    }

    public static void updateEnemyHQTarget() throws GameActionException {
        if (!Strategium.potentialEnemyHQLocations.isEmpty()) {
            Strategium.potentialEnemyHQLocations.remove(Strategium.currentEnemyHQTarget);
            Strategium.currentEnemyHQTarget = Strategium.potentialEnemyHQLocations.get(0);
        }
    }

    public static boolean buildDesignCenterNearEnemy() throws GameActionException {
        System.out.println("BILDING");
        // Build DesignCenter near enemy
        //System.out.println("Pot" + Strategium.potentialEnemyHQLocations);

        if (Navigation.aerialDistance(Strategium.enemyHQLocation) > 3) {
            System.out.println("BAGPAT");
            Navigation.bugPath(Strategium.enemyHQLocation);
            return false;
        } else {
            System.out.println("BILD");
            for (Direction dir : dir8) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) &&
                        Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.enemyHQLocation) <= 2) {
                    if(tryBuild(RobotType.DESIGN_SCHOOL, dir)) return true;
                }
            }
            Navigation.bugPath(Strategium.enemyHQLocation);
            return false;
        }

    }

    public static void refineryRentability() throws GameActionException {
        if (Navigation.aerialDistance(rc.getLocation(), Strategium.nearestRefinery) > 7 &&
                Navigation.aerialDistance(hqLocation, rc.getLocation()) > 4) {

//            System.out.println("Daleko si");
            int xMin = rc.getLocation().x - 3;
            int yMin = rc.getLocation().y - 3;
            int xMax = rc.getLocation().x + 3;
            int yMax = rc.getLocation().y + 3;
            int totalSoup = 0;
            for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++) {
                for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {
                    MapLocation loc = new MapLocation(i, j);
                    if (rc.canSenseLocation(loc))
                        totalSoup += rc.senseSoup(loc);
                }
            }

            if (totalSoup > 200) {
                for (Direction dir : dir8) {
                    boolean refineryBuilt = tryBuild(RobotType.REFINERY, dir);
//                    System.out.println("Moguce napraviti rafineriju "+ refineryBuilt);
                    if (refineryBuilt) {
                        //System.out.println("napravio rafineriju");
                        return;
                    }
                }
            }
        }


    }

    public static boolean mineAndRefine() throws GameActionException {
        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            if (Strategium.nearestSoup != null) {
                //System.out.println("Supa nadjena");
                if (Navigation.aerialDistance(Strategium.nearestSoup, rc.getLocation()) > 1) {
                    Navigation.bugPath(Strategium.nearestSoup);
                }
                refineryRentability();
                tryMine(rc.getLocation().directionTo(Strategium.nearestSoup));

                return true;
            }
        } else {
            if (Navigation.aerialDistance(Strategium.nearestRefinery, rc.getLocation()) > 1) {
                Navigation.bugPath(Strategium.nearestRefinery);
            }


            tryRefine(rc.getLocation().directionTo(Strategium.nearestRefinery));


            return true;
        }
        return false;
    }

    public static void control() throws GameActionException {
        //System.out.println("Adj count: " + adjacencyCount);
        // Avoid collisions
        Strategium.gatherInfo();
//        rc.setIndicatorLine(rc.getLocation(), currentTarget, 1, 0, 0);
//        System.out.println("Current target " + currentTarget);
        if (!rc.canSenseLocation(currentTarget) && Navigation.frustration < 50) {

            if (mineAndRefine()) return;
            else {
                RobotInfo[] robots = rc.senseNearbyRobots();
                RobotType makeRobotType = null;
                for(RobotInfo robot : robots) {
                    if (robot.team == Strategium.myTeam) {
                        if (staticRobots.contains(robot.type) && robot.getDirtCarrying() > 0) {
                            makeRobotType = RobotType.DESIGN_SCHOOL;
                        }

                        // if its not in visible radius make altern design school and fulfilment center if soup > 1000
                        // if soup < 1000 make vaporator
                        // if enemy drone spoted or enemy fulfilment center make net gun
                        // if you spot endangered building make design school - OK
                        // if you see miner or landscaper make fulfilment center - 0K
                        // if enemy building and is not fulfilment center make design school - OK


                    } else {
                        if (robot.type != RobotType.FULFILLMENT_CENTER) {
                            makeRobotType = RobotType.DESIGN_SCHOOL;
                        } else if (robot.type == RobotType.FULFILLMENT_CENTER || robot.type == RobotType.DELIVERY_DRONE) {
                            makeRobotType = RobotType.NET_GUN;
                        } else if (robot.type == RobotType.LANDSCAPER || robot.type == RobotType.MINER) {
                            makeRobotType = RobotType.FULFILLMENT_CENTER;
                        } else if (rc.getTeamSoup() < 1000) {
                            makeRobotType = RobotType.VAPORATOR;
                        } else if (rc.getTeamSoup() >= 1000) {
                            makeRobotType = RobotType.DESIGN_SCHOOL;

                        }

                    }
                    if (makeRobotType != null) {
                        if (staticRobots.contains(makeRobotType)) {
                            boolean noSameTwoBuildings = true;
                            for (RobotInfo robot2 : robots) {
                                if (robot2.team == Strategium.myTeam && robot2.type == makeRobotType) {
                                    noSameTwoBuildings = false;
                                }
                            }
                            if (noSameTwoBuildings) {
                                for (Direction dir : dir8) {
                                    if (Lattice.isBuildingSite(rc.getLocation().add(dir))) {
                                        tryBuild(makeRobotType, dir);
                                        return;
                                    }
                                }
                            }
                        } else {
                            for (Direction dir : dir8) {
                                if (Lattice.isBuildingSite(rc.getLocation().add(dir))) {
                                    tryBuild(makeRobotType, dir);
                                    return;
                                }
                            }
                        }
                    }
                }
                Navigation.bugPath(currentTarget);
                return;
            }
//              }
//              else if(rc.getSoupCarrying() > RobotType.MINER.soupLimit || currentlyRefining){
//                  if(Navigation.aerialDistance(Strategium.nearestRefinery, rc.getLocation()) > 1){
//                      Navigation.bugPath(Strategium.nearestRefinery);
//                      return;
//                  } else{
//                      currentlyRefining = true;
//                      tryRefine(rc.getLocation().directionTo(Strategium.nearestRefinery));
//                      return;
//                  }
//              } else if(rc.getSoupCarrying() == 0 && currentlyRefining == true){
//                  currentlyRefining = false;
//              }

//            checkRobotCollision();
//            buildDesignCenterNearEnemy();
//
//            if (rc.canSenseLocation(hqLocation)) {
//                if (rc.canSenseLocation(vaporatorLocation2)) {
//                    RobotInfo maybeVaporator = rc.senseRobotAtLocation(vaporatorLocation2);
//                    if (maybeVaporator != null) {
//                        if (maybeVaporator.getType() == RobotType.VAPORATOR) {
//                            Strategium.vaporatorBuilt = true;
//                            Strategium.nearestRefinery = null;
//                        }
//                    }
//                }
//            }
//            // sta ovaj deo koda radi?
////            if (Strategium.vaporatorBuilt && Navigation.aerialDistance(hqLocation) < 4) {
////                Navigation.bugPath(currentTarget);
////                if (rc.getLocation().distanceSquaredTo(currentTarget) < 5) {
////                    currentTargetIndex = (currentTargetIndex + 1) % searchRoute.size();
////                    currentTarget = searchRoute.get(currentTargetIndex);
////                }
////            }
//            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
//
//                            }
//                        }
//                        if (tryMine(dir)) {
//                            return;
//                        }
//                    }
//                if (Strategium.nearestSoup != null)
//                    if (Navigation.bugPath(Strategium.nearestSoup)) return;

//                if (rc.getLocation().distanceSquaredTo(currentTarget) < 5) {
//                    currentTargetIndex = (currentTargetIndex + 1) % searchRoute.size();
//                    currentTarget = searchRoute.get(currentTargetIndex);
//                }

//            } else {
//                // when finished with refining go back to pos where you were mining
//                Navigation.bugPath(Strategium.nearestRefinery);
//                if (Navigation.aerialDistance(Strategium.nearestRefinery, rc.getLocation()) == 1) {
//                    tryRefine(rc.getLocation().directionTo(Strategium.nearestRefinery));
//                    currentlyRefining = true;
//                    return;
//
//                }
//            }


        } else {
            updateTarget();
            return;
        }
    }
}


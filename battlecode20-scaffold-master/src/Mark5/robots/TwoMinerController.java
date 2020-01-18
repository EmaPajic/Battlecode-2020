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
        int startByteCodes = Clock.getBytecodeNum();
        staticRobots = new ArrayList<>(Arrays.asList(RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,  RobotType.FULFILLMENT_CENTER, RobotType.HQ, RobotType.NET_GUN, RobotType.REFINERY));
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
        Collections.sort(searchRoute, new LocationComparator());
        currentTarget = searchRoute.get(0); // get the first location from sorted locations which are closest to HQ
        System.out.println("Init je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

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
            // do nothing
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

    public static boolean refineryRentability() throws GameActionException {
        int startByteCodes = Clock.getBytecodeNum();
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
                boolean tryToBuildRefinery = false;
                for (Direction dir : dir8) {
                     tryToBuildRefinery = tryBuild(RobotType.REFINERY, dir);
                     if(tryToBuildRefinery) break;
                }
                System.out.println("RefineryRentabilityje potrosio: " + (Clock.getBytecodeNum() - startByteCodes));
                return tryToBuildRefinery;
            }
        }
        System.out.println("refineryRentability je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

        return false;


    }

    public static boolean mineAndRefine() throws GameActionException {
        int startByteCodes = Clock.getBytecodeNum();

        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            if (Strategium.nearestSoup != null) {
                //System.out.println("Supa nadjena");
                if (Navigation.aerialDistance(Strategium.nearestSoup, rc.getLocation()) > 1) {
                    //Navigation.bugPath(Strategium.nearestSoup);
                    triedToBuildRefinery = false;
                    System.out.println("mineNRefine(krecem se) je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));
                    return false;
                } else if(!triedToBuildRefinery  && Navigation.aerialDistance(Strategium.nearestSoup, rc.getLocation()) == 1){
                    triedToBuildRefinery = true;
                    System.out.println("Rafinerije ce pokusati da se izgradi");
                    return refineryRentability();
                } else{
                    tryMine(rc.getLocation().directionTo(Strategium.nearestSoup));
                    System.out.println("mineNRefine(kopam supu) je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

                    return true;
                }




            }
        } else {
            if (Navigation.aerialDistance(Strategium.nearestRefinery, rc.getLocation()) == 1) {
                tryRefine(rc.getLocation().directionTo(Strategium.nearestRefinery));
                return true;
            }






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

            if (mineAndRefine() && rc.getTeamSoup() < RobotType.DESIGN_SCHOOL.cost){

                System.out.println("Kopam");
                return;
            }
            else {
                System.out.println("Ima dovoljno supe");
                RobotInfo[] robots = rc.senseNearbyRobots();
                RobotType makeRobotType = null;
                System.out.println(robots.length);
                if(robots.length == 0) {
                    for (RobotInfo robot : robots) {
                        // try to defend
                        if (robot.team == Strategium.myTeam) {
                            if (staticRobots.contains(robot.type) && robot.getDirtCarrying() > 0) {
                                makeRobotType = RobotType.DESIGN_SCHOOL;
                                System.out.println("Neprijatelj pokusava da nas zakopa");
                            }

                            // if its not in visible radius make altern design school and fulfilment center if soup > 1000
                            // if soup < 1000 make vaporator
                            // if enemy drone spoted or enemy fulfilment center make net gun
                            // if you spot endangered building make design school - OK
                            // if you see miner or landscaper make fulfilment center - 0K
                            // if enemy building and is not fulfilment center make design school - OK

                            // then if possible attack
                        } else if (robot.team != Strategium.myTeam) {
                            if (staticRobots.contains(robot) && robot.type != RobotType.FULFILLMENT_CENTER) {
                                makeRobotType = RobotType.DESIGN_SCHOOL;
                                System.out.println("Neprijateljeske zgrade, nije fulfillment centar");

                            } else if (robot.type == RobotType.FULFILLMENT_CENTER || robot.type == RobotType.DELIVERY_DRONE) {
                                makeRobotType = RobotType.NET_GUN;
                                System.out.println("Neprijateljski fulfullment centar ili dron, napravi netgun");

                            } else if (robot.type == RobotType.LANDSCAPER || robot.type == RobotType.MINER) {
                                makeRobotType = RobotType.FULFILLMENT_CENTER;
                                System.out.println("Vidjen je lendskejper ili miner gradi fulfillment");

                            }
                            // then try to expand
                        }

//                        makeRobotType = RobotType.FULFILLMENT_CENTER;
                        System.out.println("Imamo dovoljno novca za ds ili fc");

                    }
                } else { // situacija kada ima okolnih nasih robota a ima mogucnost da pravi nije pokrivena
                    if (rc.getTeamSoup() >= 500 && rc.getTeamSoup() < 1000) {
                        makeRobotType = RobotType.VAPORATOR;
                        System.out.println("Imamo dovoljno novca za Vaporator");
                    } else if (rc.getTeamSoup() >= 1000) {
                        if (lastMadeRobotType == RobotType.DESIGN_SCHOOL) {
                            lastMadeRobotType = RobotType.FULFILLMENT_CENTER;
                            makeRobotType = RobotType.FULFILLMENT_CENTER;
                        } else {
                            lastMadeRobotType = RobotType.DESIGN_SCHOOL;
                            makeRobotType = RobotType.DESIGN_SCHOOL;
                        }
                    }
                }
                System.out.println(makeRobotType);
                if (makeRobotType != null) {
                    // try to make building
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
                        // try to make unit
                    } else {
                        for (Direction dir : dir8) {
                            if (Lattice.isBuildingSite(rc.getLocation().add(dir))) {
                                tryBuild(makeRobotType, dir);
                                return;
                            }
                        }
                    }
                }

                if (Strategium.nearestSoup == null) {
                    Navigation.bugPath(currentTarget);
                    return;
                }
                if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                    Navigation.bugPath(Strategium.nearestSoup);
                    return;
                } else {
                    Navigation.bugPath(Strategium.nearestRefinery);
                    return;
                }
            }


//
        } else {
            System.out.println("Update target");
            updateTarget();
            return;
        }
    }
}


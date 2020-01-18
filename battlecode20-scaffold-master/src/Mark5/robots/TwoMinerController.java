package Mark5.robots;


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
        //System.println("Init je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

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



    public static void updateEnemyHQTarget() throws GameActionException {
        if (!Strategium.potentialEnemyHQLocations.isEmpty()) {
            Strategium.potentialEnemyHQLocations.remove(Strategium.currentEnemyHQTarget);
            Strategium.currentEnemyHQTarget = Strategium.potentialEnemyHQLocations.get(0);
        }
    }

    public static boolean buildDesignCenterNearEnemy() throws GameActionException {
        //System.println("BILDING");
        // Build DesignCenter near enemy
        ////System.println("Pot" + Strategium.potentialEnemyHQLocations);

        if (Navigation.aerialDistance(Strategium.enemyHQLocation) > 3) {
            //System.println("BAGPAT");
            Navigation.bugPath(Strategium.enemyHQLocation);
            return false;
        } else {
            //System.println("BILD");
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

//            //System.println("Daleko si");
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
                //System.println("RefineryRentabilityje potrosio: " + (Clock.getBytecodeNum() - startByteCodes));
                return tryToBuildRefinery;
            }
        }
        //System.println("refineryRentability je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

        return false;


    }

    public static boolean mineAndRefine() throws GameActionException {
        int startByteCodes = Clock.getBytecodeNum();

        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            if (Strategium.nearestSoup != null) {
                ////System.println("Supa nadjena");
                if (Navigation.aerialDistance(Strategium.nearestSoup, rc.getLocation()) > 1) {
                    triedToBuildRefinery = false;
                    //System.println("mineNRefine(krecem se) je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));
                    return false;
                } else if(!triedToBuildRefinery  && Navigation.aerialDistance(Strategium.nearestSoup, rc.getLocation()) == 1){
                    triedToBuildRefinery = true;
                    //System.println("Rafinerije ce pokusati da se izgradi");
                    return refineryRentability();
                } else{
                    tryMine(rc.getLocation().directionTo(Strategium.nearestSoup));
                    //System.println("mineNRefine(kopam supu) je potrosio: " + (Clock.getBytecodeNum() - startByteCodes));

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

        Strategium.gatherInfo();
        //System.println("Current target : " + currentTarget);
        if (!rc.canSenseLocation(currentTarget) && Navigation.frustration < 50) {

            if (mineAndRefine() && rc.getTeamSoup() < RobotType.DESIGN_SCHOOL.cost){

                //System.println("Kopam");
                return;
            }
            else {
                //System.println("Ima dovoljno supe");

                RobotType makeRobotType = null;
                RobotInfo[] robots = rc.senseNearbyRobots();
                //System.println(robots.length);

                if(robots.length != 0) {
                    for (RobotInfo robot : robots) {
                        // try to defend
                        if (robot.team == Strategium.myTeam) {
                            // if you spot endangered building make design school - OK
                            if (staticRobots.contains(robot.type) && robot.getDirtCarrying() > 0) {
                                makeRobotType = RobotType.DESIGN_SCHOOL;
                                //System.println("Neprijatelj pokusava da nas zakopa");
                                break;
                            }

                            // then if possible attack
                        } else if (robot.team != Strategium.myTeam) {
                            // if enemy building and is not fulfilment center make design school - OK
                            if (staticRobots.contains(robot) && robot.type != RobotType.FULFILLMENT_CENTER) {
                                makeRobotType = RobotType.DESIGN_SCHOOL;
                                //System.println("Neprijateljeske zgrade, nije fulfillment centar");
                                break;
                                // if enemy drone spoted or enemy fulfilment center make net gun
                            } else if (robot.type == RobotType.FULFILLMENT_CENTER || robot.type == RobotType.DELIVERY_DRONE) {
                                makeRobotType = RobotType.NET_GUN;
                                //System.println("Neprijateljski fulfullment centar ili dron, napravi netgun");
                                break;
                                // if you see miner or landscaper make fulfilment center - 0K
                            } else if (robot.type == RobotType.LANDSCAPER || robot.type == RobotType.MINER) {
                                makeRobotType = RobotType.FULFILLMENT_CENTER;
                                //System.println("Vidjen je lendskejper ili miner gradi fulfillment");
                                break;

                            }
                            // then try to expand
                        }


                    }
                    // stavi da pravi design school u ovom else-u dole ako ima mogucnost a nema ga u okolini baze
                } else { // situacija kada ima okolnih nasih robota a ima mogucnost da pravi nije pokrivena
                    // if soup < 1000 make vaporator
                    if (rc.getTeamSoup() < 1000) {
                        makeRobotType = RobotType.VAPORATOR;
                        //System.println("Imamo dovoljno novca za Vaporator");
                    } else if (rc.getTeamSoup() >= 1000) {
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
                //System.println(makeRobotType);

                if (makeRobotType != null && rc.getTeamSoup() >= makeRobotType.cost) {
                    // try to make building
                    //System.println("Ovo je robot " + makeRobotType);
                    if (staticRobots.contains(makeRobotType)) {
                        boolean noSameTwoBuildings = true;
                        // decide to make design school first if there is none in HQ surrounding less or equal to 4 squares
                        if(Navigation.aerialDistance(rc.getLocation(), hqLocation)  <=  16 && makeRobotType != RobotType.DESIGN_SCHOOL){
                            for (RobotInfo robot2 : robots) {
                                if (robot2.team == Strategium.myTeam && robot2.type == RobotType.DESIGN_SCHOOL) {
                                    noSameTwoBuildings = false;
                                }
                            }
                            if(noSameTwoBuildings == true) makeRobotType = RobotType.DESIGN_SCHOOL;
                        } else {
                            for (RobotInfo robot2 : robots) {
                                if (robot2.team == Strategium.myTeam && robot2.type == makeRobotType) {
                                    noSameTwoBuildings = false;
                                }
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

                if (Strategium.nearestSoup == null || makeRobotType == null) {
                    Navigation.bugPath(currentTarget);
                    return;
                }
                if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                    Navigation.bugPath(Strategium.nearestSoup);
                } else {
                    Navigation.bugPath(Strategium.nearestRefinery);
                }
                return;
            }


//
        } else {
            //System.println("Update target");
            updateTarget();
            return;
        }
    }
}


package Mark5.utils;

import Mark5.robots.DesignSchool;
import Mark5.robots.Drone;
import Mark5.sensors.*;
import battlecode.common.*;

import java.awt.*;
import java.util.*;
import java.util.*;
import java.util.List;

import static Mark5.RobotPlayer.rc;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Strategium {

    static boolean upToDate = false;
    static LinkedList<Transaction> transactions = new LinkedList<>();

    public static MapLocation HQLocation = null;
    public static MapLocation enemyHQLocation = null;
    public static List<MapLocation> potentialEnemyHQLocations = new ArrayList<>();
    public static MapLocation currentEnemyHQTarget = null;
    public static boolean onWallAndBlocking = false;

    public static Team myTeam;
    public static Team opponentTeam;

    public static List<MapLocation> enemyBuildings = new LinkedList<>();
    public static List<MapLocation> enemyNetGuns = new LinkedList<>();
    public static List<MapLocation> refineries = new LinkedList<>();
    public static List<RobotInfo> enemyDrones = new LinkedList<>();
    public static List<RobotInfo> enemyUnits = new LinkedList<>();
    public static List<RobotInfo> alliedDrones = new LinkedList<>();
    public static List<RobotInfo> buriedFriendlyBuildings = new LinkedList<>();

    public static boolean[] robotsMet;
    public static int numDronesMet = 0;
    public static int dronesMetWithLowerID = 0;
    public static RobotInfo nearestLandscaper = null;

    public static MapLocation nearestRefinery = null;
    public static MapLocation nearestSoup = null;

    public static MapLocation nearestWater = null;

    public static RobotInfo nearestEnemyDrone;
    public static RobotInfo nearestEnemyUnit;
    public static RobotInfo blockedUnit;
    public static RobotInfo blockingUnit;

    public static boolean[][] soup = null;
    public static int[][] elevation = null;
    public static boolean[][] water = null;
    public static boolean[][] explored = null;
    public static boolean[][] occupied = null;
    public static int knownSoup = 0;
    public static boolean foundWater = false;
    public static boolean vaporatorBuilt = false;

    public static boolean shouldBuildLandscaper = false;
    public static boolean shouldCircle = true;

    public static MapLocation nearestBuriedFriendlyBuilding = null;
    public static MapLocation nearestEnemyBuilding = null;
    public static List<MapLocation> overlapLocations = new LinkedList<>();

    public static int[] dirSafetyCacheValid;
    public static boolean[] dirSafetyCache;


    public static Random rand;
    // iskreno nisam znao kom bloku polja da ovo polje pridruzim, sometimes i feel like coravi boromir
    public static int leastAmountOfSoup = 0;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;

        switch (rc.getType()) {
            case HQ:
                HQSensor.init();
                break;
            case MINER:
                MinerSensor.init();
                break;
            case DELIVERY_DRONE:
                DroneSensor.init();
                break;
            case LANDSCAPER:
                LandscaperSensor.init();
                break;
            case DESIGN_SCHOOL:
                DesignSchoolSensor.init();
                break;
            case FULFILLMENT_CENTER:
                //industrySense();
                break;
        }

        rand = new Random();
    }

    public static RobotType robotAt(MapLocation location) throws GameActionException {
        if (!rc.canSenseLocation(location)) return null;
        RobotInfo robot = rc.senseRobotAtLocation(location);
        if (robot == null) return null;
        return robot.type;
    }

    public static void gatherInfo() throws GameActionException {
        gatherInfo(0);
    }


    public static boolean canSafelyMove(Direction dir) throws GameActionException {
        int index = Navigation.index(dir);
        if(dirSafetyCacheValid[index] == rc.getRoundNum()) return dirSafetyCache[index];
        dirSafetyCache[index] = updateSafetyCache(dir);
        dirSafetyCacheValid[index] = rc.getRoundNum();
        return dirSafetyCache[index];
    }

    public static boolean updateSafetyCache(Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        MapLocation target = rc.adjacentLocation(dir);
        if(rc.sensePollution(target) >= 9600) return false;
        switch (rc.getType()) {
            case MINER:
            case LANDSCAPER:
                for (RobotInfo drone : enemyDrones) if (target.isAdjacentTo(drone.location)) return false;
                if (rc.senseFlooding(target)) return false;
                break;
            case DELIVERY_DRONE:
                if(Navigation.aerialDistance(target, HQLocation) < 2 && Navigation.aerialDistance(HQLocation) >= 2)
                    return false;
                switch (Drone.state) {
                    case SWARMER:
                        return true;
                    case TAXI:
                        if (!water[target.x][target.y]) return true;
                        for (MapLocation gun : enemyNetGuns)
                            if (target.isWithinDistanceSquared(
                                    gun, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
                                if (enemyHQLocation == null) return false;
                                int range = target.distanceSquaredTo(gun);
                                for (RobotInfo drone : alliedDrones)
                                    if (drone.location.distanceSquaredTo(gun) <= range) return true;
                                return false;
                            }
                        return true;
                    default:
                        for (MapLocation gun : enemyNetGuns){
                            if (target.isWithinDistanceSquared(
                                    gun, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) return false;
                        }
                        return true;
                }
        }

        return true;
    }

    static private void sense() throws GameActionException {


        enemyBuildings.removeIf(building -> {
            if (rc.canSenseLocation(building)) {
                try {
                    RobotInfo info = rc.senseRobotAtLocation(building);
                    if(info == null) return true;
                    return (info.type != RobotType.VAPORATOR &&
                            info.type != RobotType.REFINERY &&
                            info.type != RobotType.DESIGN_SCHOOL &&
                            info.type != RobotType.NET_GUN &&
                            info.type != RobotType.FULFILLMENT_CENTER) ||
                            info.team != opponentTeam;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        enemyNetGuns.removeIf(gun -> {
            if (rc.canSenseLocation(gun)) {
                try {
                    RobotInfo info = rc.senseRobotAtLocation(gun);
                    if (info == null) return true;
                    return info.type != RobotType.NET_GUN || info.team != opponentTeam;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        if(rc.getType() != RobotType.HQ)
            refineries.removeIf(refinery -> {
                if (rc.canSenseLocation(refinery)) {
                    try {
                        RobotInfo info = rc.senseRobotAtLocation(refinery);
                        if(info == null) return true;
                        return info.type != RobotType.REFINERY || info.team != myTeam;
                    } catch (GameActionException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });

        switch (rc.getType()) {
            case HQ:
                HQSensor.sense();
                break;
            case MINER:
                MinerSensor.sense();
                break;
            case DELIVERY_DRONE:
                DroneSensor.sense();
                break;
            case LANDSCAPER:
                LandscaperSensor.sense();
                break;
            case DESIGN_SCHOOL:
                DesignSchoolSensor.sense();
                break;
            case FULFILLMENT_CENTER:
                FulfillmentCenterSensor.sense();
                break;
        }

        if(enemyHQLocation != null) currentEnemyHQTarget = enemyHQLocation;
        else if(!potentialEnemyHQLocations.isEmpty()) currentEnemyHQTarget = potentialEnemyHQLocations.get(0);
        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

    }

    public static void gatherInfo(int bytecodesReq) throws GameActionException {
        upToDate = false;

        sense();

        switch(rc.getType()){
            case HQ:
                if(rc.getRoundNum() == 1)
                    Blockchain.reportHQLocation( 1);
                Blockchain.parseBlockchain(transactions);
                parseTransactions();
                break;
            case LANDSCAPER:
            case DELIVERY_DRONE:
                while (HQLocation == null){
                    Blockchain.parseBlockchain(transactions);
                    parseTransactions();
//                    System.out.println("Baza je " + HQLocation);
                }
//                System.out.println("Baza je " + HQLocation);
                break;
            case MINER:
                while (!upToDate){
                    Blockchain.parseBlockchain(transactions);
                    parseTransactions();

                    if(rc.getRoundNum() == Blockchain.parsingProgress){
                        upToDate = true;

                    }
                }
                break;
        }


    }

    public static void parseTransactions() throws GameActionException {
//        if (transactions == null) {
//            upToDate = true;
//            return;
//        }

        while (!transactions.isEmpty()) {

            int[] message = transactions.get(0).getMessage();

            switch (Blockchain.getType(message)) {
                case 73:
                    Blockchain.parsingProgress = max(Blockchain.parsingProgress, rc.getRoundNum() - 50);
                    if (HQLocation != null) break;
                    HQLocation = new MapLocation(message[0], message[1]);
                    updatePotentialEnemyHQLocations();
                    Wall.init();
                    break;
                case 42:
                    leastAmountOfSoup  += message[2];
                    refineries.add(new MapLocation(message[5], message[6]));
                    break;
                default:
                    break;
            }

            transactions.remove(0);
        }

    }

    static public void updatePotentialEnemyHQLocations() {
        if (HQLocation == null)
            return;
        if (Math.abs(rc.getMapWidth() / 2 - HQLocation.x) >
                Math.abs(rc.getMapHeight() / 2 - HQLocation.y)) {
            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(rc.getMapWidth() - HQLocation.x - 1, HQLocation.y));

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1 &&
                    HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(rc.getMapWidth() - HQLocation.x - 1,
                                rc.getMapHeight() - HQLocation.y - 1));

            if (HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(HQLocation.x, rc.getMapHeight() - HQLocation.y - 1));
        }
        else {
            if (HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(HQLocation.x, rc.getMapHeight() - HQLocation.y - 1));

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1 &&
                    HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(rc.getMapWidth() - HQLocation.x - 1,
                                rc.getMapHeight() - HQLocation.y - 1));

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1)
                potentialEnemyHQLocations.add(
                        new MapLocation(rc.getMapWidth() - HQLocation.x - 1, HQLocation.y));
        }
        //System.out.println("Potential HQs: " + potentialEnemyHQLocations.toString());
    }
}

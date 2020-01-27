package Mark5.utils;

import Mark5.robots.DesignSchool;
import Mark5.robots.Drone;
import Mark5.robots.HQ;
import Mark5.robots.TwoMinerController;
import Mark5.utils.Symmetry.*;

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
    static LinkedList<Transaction> lastTransaction = new LinkedList<>();

    public static MapLocation HQLocation = null;
    public static MapLocation enemyHQLocation = null;
    public static List<MapLocation> potentialEnemyHQLocations = new ArrayList<>();
    public static MapLocation currentEnemyHQTarget = null;
    public static boolean onWallAndBlocking = false;

    public static Team myTeam;
    public static Team opponentTeam;

    public static List<MapLocation> enemyBuildings = new LinkedList<>();
    public static List<NetGun> enemyNetGuns = new LinkedList<>();
    public static List<MapLocation> refineries = new LinkedList<>();
    public static List<RobotInfo> enemyDrones = new LinkedList<>();
    public static List<RobotInfo> enemyUnits = new LinkedList<>();
    public static List<RobotInfo> alliedDrones = new LinkedList<>();
    public static List<RobotInfo> buriedFriendlyBuildings = new LinkedList<>();

    public static boolean[] robotsMet;
    public static int numDronesMet = 0;
    public static int dronesMetWithLowerID = 0;
    public static RobotInfo nearestLandscaper = null;
    public static RobotInfo nearestMiner = null;
    public static RobotInfo nearestPayload = null;

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
    public static boolean[][] occupied = null;
    public static int knownSoup = 0;
    public static boolean foundWater = false;
    public static boolean vaporatorBuilt = false;

    public static boolean shouldBuildLandscaper = false;
    public static boolean shouldCircle = true;

    public static MapLocation nearestBuriedFriendlyBuilding = null;
    public static MapLocation nearestEnemyBuilding = null;
    public static List<MapLocation> overlapLocations = new LinkedList<>();
    public static NetGun lastEnemyNetGunSeen = null;

    public static int[] dirSafetyCacheValid;
    public static boolean[] dirSafetyCache;

    public static Random rand;
    // iskreno nisam znao kom bloku polja da ovo polje pridruzim, sometimes i feel like coravi boromir
    public static int leastAmountOfSoup = 0;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;
        rand = new Random();

        switch (rc.getType()) {
            case HQ:
                HQSensor.init();
                break;
            case MINER:
                MinerSensor.init();
                break;
            case DELIVERY_DRONE:
                Grid.init();
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
                switch (Drone.state) {
                    case SWARMER:
                        return true;
                    case TAXI:
                        if (!water[target.x][target.y]) return true;
                        for (NetGun gun : enemyNetGuns)
                            if (target.isWithinDistanceSquared(
                                    gun.location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) &&
                                    Navigation.aerialDistance(target, gun.location) +
                                            (gun.readyOnRound - rc.getRoundNum()) * 3 / 2 <= 5
                            ) {
                                if (enemyHQLocation == null) return false;
                                int range = target.distanceSquaredTo(gun.location);
                                for (RobotInfo drone : alliedDrones)
                                    if (drone.location.isWithinDistanceSquared(gun.location, range)) return true;
                                return false;
                            }
                        return true;
                    default:
                        for (NetGun gun : enemyNetGuns){
                            if (target.isWithinDistanceSquared(
                                    gun.location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) &&
                                    Navigation.aerialDistance(target, gun.location) +
                                            (gun.readyOnRound - rc.getRoundNum()) * 3 / 2 <= 5
                            ) return false;
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
            if (rc.canSenseLocation(gun.location)) {
                try {
                    RobotInfo info = rc.senseRobotAtLocation(gun.location);
                    if (info == null){
                        Grid.unsafe[gun.location.x / 7 + gun.location.y / 7 * Grid.cols] = false;
                        return true;
                    }
                    if(info.getID() != gun.id && gun.id >= 0){
                        Grid.unsafe[gun.location.x / 7 + gun.location.y / 7 * Grid.cols] = false;
                        return true;
                    }
                    return false;
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

        int tmp = Clock.getBytecodeNum();
        if(Symmetry.nonVerticalCount <= 5) {
            Symmetry.checkVerticalSymmetry();
        }
        if(Symmetry.nonHorizontalCount <= 5) {
            Symmetry.checkHorizontalSymmetry();
        }
        if(HQLocation != null) {
            potentialEnemyHQLocations.removeIf(location -> {
                try {
                    return Symmetry.removeWrongSymmetry(location);
                } catch (GameActionException e) {
                    //e.printStackTrace();
                    return false;
                }
            });
        }
        System.out.println(Clock.getBytecodeNum() - tmp);
        if(enemyHQLocation != null) currentEnemyHQTarget = enemyHQLocation;
        else if(!potentialEnemyHQLocations.isEmpty()) currentEnemyHQTarget = potentialEnemyHQLocations.get(0);
        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

    }

    public static void gatherInfo(int bytecodesReq) throws GameActionException {
        upToDate = false;

        sense();
        if(rc.getRoundNum() < 4)
            return;
        switch(rc.getType()){
            case HQ:
                if(rc.getRoundNum() == 4)
                    Blockchain.reportHQLocation( 1);
                Blockchain.parseBlockchain(transactions);
                parseTransactions();
                break;

            case MINER:
            case DELIVERY_DRONE:
                while (!upToDate || HQLocation == null){
                    if(HQLocation != null)
                        Blockchain.parsingProgress = max(Blockchain.parsingProgress, rc.getRoundNum() - 50);
                    Blockchain.parseBlockchain(transactions);
                    parseTransactions();

                    if(rc.getRoundNum() == Blockchain.parsingProgress){
                        upToDate = true;
                    }
                }
            case LANDSCAPER:
                while (!upToDate || HQLocation == null){
                    if(HQLocation != null)
                        Blockchain.parsingProgress = max(Blockchain.parsingProgress, rc.getRoundNum() - 50);
                    Blockchain.parseBlockchain(transactions);
                    parseTransactions();

                    if(rc.getRoundNum() == Blockchain.parsingProgress){
                        upToDate = true;
                    }
                }
            default:
                while (HQLocation == null){
                    Blockchain.parseBlockchain(transactions);
                    parseTransactions();
//                    System.out.println("Baza je " + HQLocation);
                }
//                System.out.println("Baza je " + HQLocation);
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
                    if (HQLocation != null) break;
                    HQLocation = new MapLocation(message[0], message[1]);
                    if(rc.getType() == RobotType.DELIVERY_DRONE)
                        //TwoMinerController.searchRoute.add(0, HQLocation);
                    updatePotentialEnemyHQLocations();
                    Wall.init();
                    System.out.println("HQ LOCIRAN");
                    break;
                case 42:
                    leastAmountOfSoup  += message[2];
                    refineries.add(new MapLocation(message[5], message[6]));
                    break;
                case 17:
                    if (Strategium.enemyHQLocation != null) break;
                    Strategium.enemyHQLocation = new MapLocation(message[0], message[1]);
                    enemyNetGuns.add(new NetGun(Strategium.enemyHQLocation, -1, 10));
                    enemyBuildings.add(Strategium.enemyHQLocation);
                case 98:
                    MapLocation netGunLoc = new MapLocation(message[0], message[1]);
                    lastEnemyNetGunSeen = new NetGun(netGunLoc, message[2], message[5]);
                    if (!enemyNetGuns.contains(lastEnemyNetGunSeen)) {
                        enemyNetGuns.add(lastEnemyNetGunSeen);
                    }
                default:
                    break;
            }

            transactions.remove(0);
        }

    }

    static public void updatePotentialEnemyHQLocations() {
        if (HQLocation == null)
            return;
        Symmetry.verticalSymmetryEnemyHQLocation = new MapLocation(rc.getMapWidth() - HQLocation.x - 1, HQLocation.y);
        Symmetry.horizontalSymmetryEnemyHQLocation = new MapLocation(HQLocation.x, rc.getMapHeight() - HQLocation.y - 1);
        Symmetry.diagonalSymmetryEnemyHQLocation = new MapLocation(rc.getMapWidth() - HQLocation.x - 1,
                rc.getMapHeight() - HQLocation.y - 1);
        if (Math.abs(rc.getMapWidth() / 2 - HQLocation.x) >
                Math.abs(rc.getMapHeight() / 2 - HQLocation.y)) {
            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1)
                potentialEnemyHQLocations.add(Symmetry.verticalSymmetryEnemyHQLocation);

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1 &&
                    HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(Symmetry.diagonalSymmetryEnemyHQLocation);

            if (HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(Symmetry.horizontalSymmetryEnemyHQLocation);
        }
        else {
            if (HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(Symmetry.horizontalSymmetryEnemyHQLocation);

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1 &&
                    HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                potentialEnemyHQLocations.add(Symmetry.diagonalSymmetryEnemyHQLocation);

            if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1)
                potentialEnemyHQLocations.add(Symmetry.verticalSymmetryEnemyHQLocation);
        }
        //System.out.println("Potential HQs: " + potentialEnemyHQLocations.toString());
    }
}

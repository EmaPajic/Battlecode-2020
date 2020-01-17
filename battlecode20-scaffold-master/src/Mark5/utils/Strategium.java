package Mark5.utils;

import Mark5.RobotPlayer;
import Mark5.robots.Drone;
import Mark5.sensors.DroneSensor;
import Mark5.sensors.IndustrySensor;
import Mark5.sensors.LandscaperSensor;
import Mark5.sensors.MinerSensor;
import battlecode.common.*;

import java.util.*;

import static Mark5.RobotPlayer.rc;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Strategium {

    static boolean upToDate = false;
    static ArrayList<Transaction> transactions = new ArrayList<>();

    public static MapLocation HQLocation = null;
    public static MapLocation enemyHQLocation = null;
    public static List<MapLocation> potentialEnemyHQLocations = new ArrayList<>();
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

    public static Random rand;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;

        switch (rc.getType()) {
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
        if (!rc.canMove(dir)) return false;
        MapLocation target = rc.adjacentLocation(dir);
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

        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

        enemyBuildings.removeIf(building -> {
            if (rc.canSenseLocation(building)) {
                try {
                    RobotInfo info = rc.senseRobotAtLocation(building);
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
                    return info.type != RobotType.NET_GUN || info.team != opponentTeam;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        refineries.removeIf(refinery -> {
            if (rc.canSenseLocation(refinery)) {
                try {
                    RobotInfo info = rc.senseRobotAtLocation(refinery);
                    return info.type != RobotType.REFINERY || info.team != myTeam;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
            return false;
        });

        switch (rc.getType()) {
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
            case FULFILLMENT_CENTER:
                IndustrySensor.sense();
                break;
        }
    }

    static void gatherInfo(int bytecodesReq) throws GameActionException {

        upToDate = false;

        sense();

        if (rc.getType() == RobotType.HQ) do {

            parseTransactions(Blockchain.parseBlockchain(transactions));

        } while (Clock.getBytecodesLeft() > bytecodesReq && !upToDate);
    }

    static private void parseTransactions(List<Transaction> transactions) {
        if (transactions == null) {
            upToDate = true;
            return;
        }

        while (!transactions.isEmpty()) {

            int[] message = transactions.get(0).getMessage();

            switch (Blockchain.getType(message)) {
                case 73:
                    if (HQLocation != null) break;
                    HQLocation = new MapLocation(message[0], message[1]);

                    if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1)
                        potentialEnemyHQLocations.add(
                                new MapLocation(rc.getMapWidth() - HQLocation.x - 1, HQLocation.y));

                    if (HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                        potentialEnemyHQLocations.add(
                                new MapLocation(HQLocation.x, rc.getMapHeight() - HQLocation.y - 1));

                    if (HQLocation.x != rc.getMapWidth() - HQLocation.x - 1 &&
                            HQLocation.y != rc.getMapHeight() - HQLocation.y - 1)
                        potentialEnemyHQLocations.add(
                                new MapLocation(rc.getMapWidth() - HQLocation.x - 1,
                                        rc.getMapHeight() - HQLocation.y - 1));

                    break;
                default:
                    break;
            }

            transactions.remove(0);
        }

    }
}

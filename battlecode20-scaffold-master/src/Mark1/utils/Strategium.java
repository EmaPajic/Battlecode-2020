package Mark1.utils;

import battlecode.common.*;

import java.util.*;
import java.util.List;

import static Mark1.RobotPlayer.rc;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Strategium {

    static boolean upToDate = false;
    static ArrayList<Transaction> transactions = new ArrayList<>();

    public static MapLocation HQLocation = null;
    public static MapLocation enemyHQLocation = null;
    public static List<MapLocation> potentialEnemyHQLocations = new ArrayList<>();

    static Team myTeam;
    public static Team opponentTeam;

    private static HashMap<MapLocation, RobotInfo> enemyBuildings = new HashMap<>();
    private static HashMap<MapLocation, RobotInfo> enemyNetGuns = new HashMap<>();
    private static HashMap<MapLocation, RobotInfo> refineries = new HashMap<>();
    private static List<RobotInfo> enemyDrones = new ArrayList<>();
    private static List<RobotInfo> enemyUnits = new ArrayList<>();

    public static MapLocation nearestRefinery = null;
    public static MapLocation nearestSoup = null;

    public static MapLocation nearestWater = null;

    public static RobotInfo nearestEnemyDrone;
    public static RobotInfo nearestEnemyUnit;

    public static boolean[][] soup = null;
    public static int[][] elevation = null;
    public static boolean[][] water = null;
    public static boolean[][] explored = null;
    public static int knownSoup = 0;
    public static boolean foundWater = false;

    private static Random rand;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;

        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];

        rand = new Random();
    }

    public static void gatherInfo() throws GameActionException {
        gatherInfo(0);
    }

    private static void droneSense() throws GameActionException {

        enemyUnits.clear();
        enemyDrones.clear();
        nearestEnemyDrone = null;
        nearestEnemyUnit = null;

        for (RobotInfo robot : rc.senseNearbyRobots()) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) {
                    if (HQLocation == null) {
                        HQLocation = robot.location;
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
                    }
                }

            } else {

                if (robot.type == RobotType.HQ) {
                    enemyHQLocation = robot.location;
                    potentialEnemyHQLocations.clear();
                }
                else if (robot.type == RobotType.NET_GUN) enemyNetGuns.put(robot.location, robot);
                else if (robot.type.isBuilding()) enemyBuildings.put(robot.location, robot);
                else if (robot.type == RobotType.DELIVERY_DRONE) {
                    enemyDrones.add(robot);
                    if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                        nearestEnemyDrone = robot;
                } else {
                    enemyUnits.add(robot);
                    if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyUnit))
                        nearestEnemyUnit = robot;
                }

            }

        }

        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

        Iterator<Map.Entry<MapLocation, RobotInfo>> it = enemyBuildings.entrySet().iterator();
        while (it.hasNext()) {
            RobotInfo building = it.next().getValue();
            if (rc.canSenseLocation(building.location))
                if (!rc.canSenseRobot(building.ID)) {
                    it.remove();
                }
        }

        it = enemyNetGuns.entrySet().iterator();
        while (it.hasNext()) {
            RobotInfo netGun = it.next().getValue();
            if (rc.canSenseLocation(netGun.location))
                if (!rc.canSenseRobot(netGun.ID)) {
                    it.remove();
                }
        }

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        for (int i = xMin; i <= xMax; i++)
            for (int j = yMin; j <= yMax; j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    explored[i][j] = true;
                    elevation[i][j] = rc.senseElevation(location);
                    if (rc.senseFlooding(location)) {
                        if (!water[i][j]) {
                            water[i][j] = true;
                            foundWater = true;
                        }
                    } else {

                        water[i][j] = false;
                        if (location.equals(nearestWater))
                            nearestWater = null;

                    }
                }
            }

        if (!rc.isReady() && nearestWater == null) findWater();

    }

    private static void findWater() {
        if (foundWater)
            for (int i = 0; i < rc.getMapWidth(); i++)
                for (int j = 0; j < rc.getMapHeight(); j++) {
                    if (water[i][j]) if (Navigation.aerialDistance(i, j) < Navigation.aerialDistance(nearestWater))
                        nearestWater = new MapLocation(i, j);
                }

        if (nearestWater != null) return;

        if(!potentialEnemyHQLocations.isEmpty())
            nearestWater = potentialEnemyHQLocations.get(
                    rand.nextInt(potentialEnemyHQLocations.size()));
        else nearestWater = new MapLocation(rand.nextInt(rc.getMapWidth()), rand.nextInt(rc.getMapHeight()));

    }

    private static void minerSense() throws GameActionException {

        enemyDrones.clear();
        nearestEnemyDrone = null;

        for (RobotInfo robot : rc.senseNearbyRobots()) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) HQLocation = robot.location;
                if (robot.type.canRefine()) refineries.put(robot.location, robot);

            } else {

                if (robot.type == RobotType.DELIVERY_DRONE) {
                    enemyDrones.add(robot);
                    if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                        nearestEnemyDrone = robot;
                }

            }

        }

        Iterator<Map.Entry<MapLocation, RobotInfo>> it = refineries.entrySet().iterator();
        while (it.hasNext()) {
            RobotInfo refinery = it.next().getValue();
            if (rc.canSenseLocation(refinery.location))
                if (!rc.canSenseRobot(refinery.ID)) {
                    it.remove();
                }
        }

        nearestRefinery = null;
        for (MapLocation refinery : refineries.keySet())
            if (Navigation.aerialDistance(nearestRefinery, rc.getLocation()) >
                    Navigation.aerialDistance(refinery, rc.getLocation()))
                nearestRefinery = refinery;

        int xMin = rc.getLocation().x - 5;
        int yMin = rc.getLocation().y - 5;
        int xMax = rc.getLocation().x + 5;
        int yMax = rc.getLocation().y + 5;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location))
                    if (rc.senseSoup(location) > 0) {
                        explored[i][j] = true;
                        if (!soup[i][j]) {
                            knownSoup++;
                            soup[i][j] = true;
                            if (Navigation.aerialDistance(rc.getLocation(), i, j) <
                                    Navigation.aerialDistance(rc.getLocation(), nearestSoup))
                                nearestSoup = new MapLocation(i, j);
                        }

                    } else if (soup[i][j]) {
                        soup[i][j] = false;
                        knownSoup--;
                        if (nearestSoup.x == i && nearestSoup.y == j) nearestSoup = null;
                    }


            }

        if (knownSoup > 0 && (nearestSoup == null || !rc.isReady())) scanAllSoup();

    }

    private static void scanAllSoup() {
        if (knownSoup <= 0) {
            nearestSoup = null;
            return;
        }
        for (int i = 0; i < rc.getMapWidth(); i++)
            for (int j = 0; j < rc.getMapHeight(); j++)
                if (soup[i][j] && Navigation.aerialDistance(rc.getLocation(), i, j) <
                        Navigation.aerialDistance(rc.getLocation(), nearestSoup)) nearestSoup = new MapLocation(i, j);
    }

    static private void sense() throws GameActionException {
        switch (rc.getType()) {
            case MINER:
                minerSense();
                break;
            case DELIVERY_DRONE:
                droneSense();
                break;
        }
    }

    static void gatherInfo(int bytecodesReq) throws GameActionException {

        upToDate = false;

        sense();

        do {

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

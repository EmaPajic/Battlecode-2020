package Mark2.utils;

import battlecode.common.*;

import java.util.*;
import java.util.List;

import static Mark2.RobotPlayer.hqLocation;
import static Mark2.RobotPlayer.rc;
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

    public static HashMap<MapLocation, RobotInfo> enemyBuildings = new HashMap<>();
    public static HashMap<MapLocation, RobotInfo> enemyNetGuns = new HashMap<>();
    private static HashMap<MapLocation, RobotInfo> refineries = new HashMap<>();
    private static List<RobotInfo> enemyDrones = new ArrayList<>();
    public static List<RobotInfo> enemyUnits = new ArrayList<>();

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
    public static int knownSoup = 0;
    public static boolean foundWater = false;
    public static boolean vaporatorBuilt = false;

    public static boolean shouldBuildLandscaper = false;
    public static boolean shouldCircle = true;

    public static Random rand;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;

        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];

        rand = new Random();
    }

    private static RobotType robotAt(MapLocation location) throws GameActionException {
        if(!rc.canSenseLocation(location)) return null;
        RobotInfo robot = rc.senseRobotAtLocation(location);
        if(robot == null) return null;
        return robot.type;
    }

    public static void gatherInfo() throws GameActionException {
        gatherInfo(0);
    }

    private static void industrySense() throws GameActionException {

        if (HQLocation == null) {
            for (RobotInfo robot : rc.senseNearbyRobots()) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    HQLocation = robot.location;
                    Wall.init();
                    break;
                }

            }
        }

        System.out.println(HQLocation);

        if (HQLocation != null) {
            shouldBuildLandscaper = rc.senseRobotAtLocation(Strategium.HQLocation.translate(-1, -1)) == null;
            if (rc.senseRobotAtLocation(Strategium.HQLocation.translate(-2, -2)) != null)
                shouldBuildLandscaper = false;
            if (rc.senseRobotAtLocation(Strategium.HQLocation.translate(-2, -1)) != null)
                shouldBuildLandscaper = false;

        }

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
                for (MapLocation gun : enemyNetGuns.keySet())
                    if (target.isWithinDistanceSquared(
                            gun, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) return false;
        }

        return true;
    }

    private static void droneSense() throws GameActionException {

        enemyUnits.clear();
        enemyDrones.clear();
        nearestEnemyDrone = null;
        nearestEnemyUnit = null;
        blockedUnit = null;
        blockingUnit = null;

        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) {
                    if (HQLocation == null) {
                        HQLocation = robot.location;
                        Wall.init();
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
                } else if (HQLocation != null) {
                    if (robot.type == RobotType.LANDSCAPER) {
                        if (robot.location.equals(Wall.launchPad) && Wall.isLaunchPadBlocked()) {
                            blockedUnit = robot;
                        }
                    } else if (robot.type == RobotType.MINER) {
                        if (Wall.stuckOnWall(robot.location)) blockingUnit = robot;
                    }
                }

            } else {

                if (robot.type == RobotType.HQ) {
                    enemyHQLocation = robot.location;
                    potentialEnemyHQLocations.clear();
                    enemyNetGuns.put(robot.location, robot);
                } else if (robot.type == RobotType.NET_GUN) enemyNetGuns.put(robot.location, robot);
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
            for (int i = rc.getMapWidth(); i-- > 0; )
                for (int j = rc.getMapHeight(); j-- > 0; ) {
                    if (water[i][j]) if (Navigation.aerialDistance(i, j) < Navigation.aerialDistance(nearestWater))
                        nearestWater = new MapLocation(i, j);
                }

        if (nearestWater != null) return;

        if (!potentialEnemyHQLocations.isEmpty())
            nearestWater = potentialEnemyHQLocations.get(
                    rand.nextInt(potentialEnemyHQLocations.size()));
        else nearestWater = new MapLocation(rand.nextInt(rc.getMapWidth()), rand.nextInt(rc.getMapHeight()));

    }

    private static void minerSense() throws GameActionException {

        enemyDrones.clear();
        nearestEnemyDrone = null;
        RobotInfo[] robots = rc.senseNearbyRobots();

        if (HQLocation != null) onWallAndBlocking = Wall.onWallAndBlocking(robots, rc.getLocation());

        for (RobotInfo robot : robots) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) {
                    HQLocation = robot.location;
                    Wall.init();
                }
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
        for (MapLocation refinery : refineries.keySet()) {
            if (refinery == HQLocation && Strategium.vaporatorBuilt)
                continue;
            if (Navigation.aerialDistance(nearestRefinery, rc.getLocation()) >
                    Navigation.aerialDistance(refinery, rc.getLocation()))
                nearestRefinery = refinery;
        }
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

        if (knownSoup > 0 && nearestSoup == null) scanAllSoup();

    }

    private static void scanAllSoup() {
        if (knownSoup <= 0) {
            nearestSoup = null;
            return;
        }
        for (int i = rc.getMapWidth(); i-- > 0; )
            for (int j = rc.getMapHeight(); j-- > 0; )
                if (soup[i][j] && Navigation.aerialDistance(rc.getLocation(), i, j) <
                        Navigation.aerialDistance(rc.getLocation(), nearestSoup)) nearestSoup = new MapLocation(i, j);
    }

    static private void landscaperSense() throws GameActionException {

        shouldCircle = false;

        RobotInfo[] robots = rc.senseNearbyRobots();

        if (HQLocation == null) {
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    HQLocation = robot.location;
                    Wall.init();
                    break;
                }
            }
        }

        if (HQLocation != null){
            Wall.scanWall();
            if (Navigation.aerialDistance(HQLocation) <= 2){
                if(rc.getLocation().equals(HQLocation.translate(-1, -1))) shouldCircle = true;
                else if(!rc.getLocation().equals(HQLocation.translate(-1, -2))) {
                    shouldCircle = robotAt(Wall.clockwise(rc.getLocation())) != RobotType.LANDSCAPER;
                    System.out.println(shouldCircle);
                    if (rc.senseElevation(Wall.clockwise(rc.getLocation())) > 3 + rc.senseElevation(rc.getLocation()))
                        shouldCircle = false;
                    System.out.println(shouldCircle);
                    if (rc.senseElevation(Wall.clockwise(rc.getLocation())) < -3 + rc.senseElevation(rc.getLocation()))
                        shouldCircle = false;
                    System.out.println(shouldCircle);
                    System.out.println(rc.senseElevation(Wall.clockwise(rc.getLocation())));
                }
                else  {

                    shouldCircle = true;//robotAt(Strategium.HQLocation.translate(-1, -1)) != RobotType.LANDSCAPER;
                    if (robotAt(HQLocation.translate(-2, -2)) == RobotType.LANDSCAPER)
                        shouldCircle = false;
                    if (robotAt(HQLocation.translate(-2, -1)) == RobotType.LANDSCAPER)
                        shouldCircle = false;
                    if (rc.senseElevation(Wall.clockwise(rc.getLocation())) > 3 + rc.senseElevation(rc.getLocation()))
                        shouldCircle = false;
                    if (rc.senseElevation(Wall.clockwise(rc.getLocation())) < -3 + rc.senseElevation(rc.getLocation()))
                        shouldCircle = false;

/*
        shouldCircle = true;

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        int cntWall = 0;
        int cntLandscapper = 0;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++) {
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    if (Navigation.aerialDistance(hqLocation, location) == 2) {
                        ++cntWall;
                        if (rc.senseRobotAtLocation(location) != null) {
                            if (rc.senseRobotAtLocation(location).getType() == RobotType.LANDSCAPER)
                                ++cntLandscapper;
                        }
                    }
*/
                }
            }

            for(RobotInfo robot : rc.senseNearbyRobots()) {
                if(robot.type != RobotType.LANDSCAPER) if(Wall.onWallAndBlocking(robots, robot.location))
                    shouldCircle = true;
            }

        }
//        if(cntLandscapper >= cntWall - 1)
//            shouldCircle = false;

        //shouldCircle = rc.getRobotCount() < 30;

    }

    static private void sense() throws GameActionException {
        switch (rc.getType()) {
            case MINER:
                minerSense();
                break;
            case DELIVERY_DRONE:
                droneSense();
                break;
            case LANDSCAPER:
                landscaperSense();
                break;
            case DESIGN_SCHOOL:
            case FULFILLMENT_CENTER:
                industrySense();
                break;
        }
    }

    static void gatherInfo(int bytecodesReq) throws GameActionException {

        upToDate = false;

        sense();

        if(rc.getType() == RobotType.HQ) do {

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

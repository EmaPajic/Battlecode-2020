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
    static Team myTeam;
    public static Team opponentTeam;

    private static HashMap<MapLocation, RobotInfo> enemyBuildings = new HashMap<>();
    private static HashMap<MapLocation, RobotInfo> enemyNetGuns = new HashMap<>();
    private static HashMap<MapLocation, RobotInfo> refineries = new HashMap<>();
    private static List<RobotInfo> enemyDrones = new ArrayList<>();

    public static MapLocation nearestRefinery = null;
    public static MapLocation nearestSoup = null;

    public static boolean[][] soup = null;
    public static int[][] depth = null;
    public static boolean[][] water;
    public static int knownSoup = 0;

    public static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;

        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];

    }

    public static void gatherInfo() throws GameActionException {
        gatherInfo(0);
    }

    private static void droneSense() {


    }

    private static void minerSense() throws GameActionException {

        enemyDrones.clear();

        for (RobotInfo robot : rc.senseNearbyRobots()) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) HQLocation = robot.location;
                if (robot.type.canRefine()) refineries.put(robot.location, robot);

            } else {

                if (robot.type == RobotType.DELIVERY_DRONE) enemyDrones.add(robot);

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
        for (int i = 0; i < rc.getMapWidth(); i++)
            for (int j = 0; j < rc.getMapHeight(); j++)
                if (soup[i][j] && Navigation.aerialDistance(rc.getLocation(), i, j) <
                        Navigation.aerialDistance(rc.getLocation(), nearestSoup)) nearestSoup = new MapLocation(i, j);
    }

    static private void sense() throws GameActionException {
        switch (rc.getType()) {
            case MINER:
                minerSense();
            case DELIVERY_DRONE:
                droneSense();
        }
    }

    static void gatherInfo(int bytecodesReq) throws GameActionException {

        upToDate = false;

        if (rc.getCooldownTurns() < 1) sense();

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
                    System.out.println("HQ Located at: (" + HQLocation.x + ", " + HQLocation.y + ")");
                    break;
                default:
                    break;
            }

            transactions.remove(0);
        }

    }
}

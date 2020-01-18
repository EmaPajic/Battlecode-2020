package Mark5.sensors;

import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import Mark5.utils.Wall;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import java.util.Iterator;
import java.util.Map;

import static Mark5.RobotPlayer.rc;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static Mark5.utils.Strategium.*;

public class MinerSensor {
    public static int visibleSoup;

    public static void init() {
        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        //explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    }

    public static void sense() throws GameActionException {

        enemyDrones.clear();
        nearestEnemyDrone = null;
        visibleSoup = 0;

        int xMin = rc.getLocation().x - 5;
        int yMin = rc.getLocation().y - 5;
        int xMax = rc.getLocation().x + 5;
        int yMax = rc.getLocation().y + 5;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    elevation[i][j] = rc.senseElevation(location);
                    water[i][j] = rc.senseFlooding(location);
                    occupied[i][j] = false;
                    visibleSoup += rc.senseSoup(location);
                    if (rc.senseSoup(location) > 0) {
                        //explored[i][j] = true;
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
                        if (nearestSoup != null)
                            if (nearestSoup.x == i && nearestSoup.y == j) nearestSoup = null;
                    }

                }

            }

        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {
            occupied[robot.location.x][robot.location.y] = true;

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) {

                    if (HQLocation == null) {

                        HQLocation = robot.location;

                        Strategium.updatePotentialEnemyHQLocations();

                        Wall.init();

                    }

                }
                if (robot.type.canRefine()) if (!refineries.contains(robot.location)) refineries.add(robot.location);

            } else {
                switch (robot.type) {

                    case DELIVERY_DRONE:
                        enemyDrones.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                            nearestEnemyDrone = robot;
                        break;
                    case HQ:
                        potentialEnemyHQLocations.clear();
                        enemyHQLocation = robot.location;
                        currentEnemyHQTarget = enemyHQLocation;
                        break;
                }


            }

        }

        //Wall.checkBaseStatus();


        nearestRefinery = null;
        for (MapLocation refinery : refineries) {
            if (refinery == HQLocation && Strategium.vaporatorBuilt)
                continue;
            if (Navigation.aerialDistance(nearestRefinery, rc.getLocation()) >
                    Navigation.aerialDistance(refinery, rc.getLocation()))
                nearestRefinery = refinery;
        }

        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

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
}

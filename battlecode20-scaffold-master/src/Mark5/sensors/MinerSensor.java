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

    public static void init(){
        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    }

    public static void sense() throws GameActionException {

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
}

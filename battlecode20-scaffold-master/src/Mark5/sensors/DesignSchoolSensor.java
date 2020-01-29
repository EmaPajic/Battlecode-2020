package Mark5.sensors;

import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

import static Mark5.RobotPlayer.dir8;

import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;
import static java.lang.Math.*;

public class DesignSchoolSensor {
    public static Set<Direction> priorityBuildDirections = new HashSet<>();
    public static int numLandscapers = 0;
    public static int numThreats = 0;
    public static boolean netGunNearby;
    public static boolean droneNearby;

    public static void init() {

        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];

    }

    public static void sense() throws GameActionException {

        numLandscapers = 0;
        numThreats = 0;
        priorityBuildDirections.clear();
        nearestWater = null;
        nearestEnemyBuilding = null;
        nearestBuriedFriendlyBuilding = null;

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        if(HQLocation != null && rc.senseElevation(rc.getLocation()) < 5)
        for (int i = max(xMin, 0); i <= min(xMax, rc.getMapWidth() - 1); i++)
            for (int j = max(yMin, 0); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location))
                    if (rc.senseFlooding(location))
                        if(!Lattice.isPit(location)){
                        //if (Navigation.aerialDistance(nearestWater) > Navigation.aerialDistance(location) &&
                        //        numThreats < 2)
                        //    nearestWater = location;
                        //numThreats++;
                        priorityBuildDirections.add(rc.getLocation().directionTo(location));
                    }

            }


        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {

            occupied[robot.location.x][robot.location.y] = true;

            if (robot.team == myTeam) {

                switch (robot.type) {

                    case HQ:
                    case NET_GUN:
                        netGunNearby = true;
                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:
                        if (robot.dirtCarrying > 0) {
                            if (Navigation.aerialDistance(robot) <
                                    Navigation.aerialDistance(nearestBuriedFriendlyBuilding))
                                nearestBuriedFriendlyBuilding = robot.location;
                            numThreats++;
                            if (robot.type == RobotType.HQ) numThreats += 4;
                            priorityBuildDirections.add(rc.getLocation().directionTo(robot.location));
                        }
                        break;
                    // moving robots
                    case LANDSCAPER:
                        ++numLandscapers;
                }

            } else {

                switch (robot.type) {

                    case HQ:
                        numThreats += 2;
                        enemyHQLocation = robot.getLocation();
                    case FULFILLMENT_CENTER:
                    case NET_GUN:
                    case DESIGN_SCHOOL:
                    case VAPORATOR:
                    case REFINERY:
                    case LANDSCAPER:
                        ++numThreats;
                        priorityBuildDirections.add(rc.getLocation().directionTo(robot.location));
                        break;
                    case DELIVERY_DRONE:
                        droneNearby = true;
                }

            }

        }

    }

}

package Mark5.sensors;

import Mark5.utils.Navigation;
import Mark5.utils.Wall;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class LandscaperSensor {

    public static void init() {
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    }

    public static void sense() throws GameActionException {

        nearestBuriedFriendlyBuilding = null;
        nearestEnemyBuilding = null;
        overlapLocations.clear();
        enemyDrones.clear();
        buriedFriendlyBuildings.clear();

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    elevation[i][j] = rc.senseElevation(location);
                    water[i][j] = rc.senseFlooding(location);
                    occupied[i][j] = false;
                }
            }

        RobotInfo[] robots = rc.senseNearbyRobots();


        for (RobotInfo robot : robots) {
            occupied[robot.location.x][robot.location.y] = true;

            if (robot.getTeam() == myTeam) {

                switch (robot.type) {
                    case HQ:
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

                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:
                    case NET_GUN:

                        if (robot.dirtCarrying > 0) {
                            if (Navigation.aerialDistance(robot) <
                                    Navigation.aerialDistance(nearestBuriedFriendlyBuilding))
                                nearestBuriedFriendlyBuilding = robot.location;
                        }

                        buriedFriendlyBuildings.add(robot);

                        break;
                }
            } else {

                switch (robot.type) {
                    case HQ:
                        if (enemyHQLocation == null) {
                            enemyHQLocation = robot.location;
                            potentialEnemyHQLocations.clear();
                        }
                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:
                    case NET_GUN:
                        if (!enemyBuildings.contains(robot.location)) enemyBuildings.add(robot.location);
                        break;

                    case DELIVERY_DRONE:
                        enemyDrones.add(robot);
                }

            }

        }

        for(RobotInfo friendly : buriedFriendlyBuildings)
            {
                for(MapLocation enemy : enemyBuildings) if(Navigation.aerialDistance(friendly.location, enemy) <= 2) {

                    for(Direction dir : dir8){
                        MapLocation location = friendly.location.add(dir);
                        if(location.isAdjacentTo(enemy))
                            overlapLocations.add(location);
                    }
                }
            }

        for (MapLocation enemy : enemyBuildings)
            if (Navigation.aerialDistance(enemy) < Navigation.aerialDistance(nearestEnemyBuilding))
                nearestEnemyBuilding = enemy;





    }

}

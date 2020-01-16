package Mark5.sensors;

import Mark5.utils.Navigation;
import Mark5.utils.Wall;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;

public class LandscaperSensor {

    public static void sense() throws GameActionException {

        nearestBuriedFriendlyBuilding = null;
        nearestEnemyBuilding = null;
        overlapLocations.clear();
        enemyDrones.clear();
        buriedFriendlyBuildings.clear();

        RobotInfo[] robots = rc.senseNearbyRobots();

        if (HQLocation == null) {
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    Wall.init();
                    break;
                }
            }
        }


        for (RobotInfo robot : rc.senseNearbyRobots()) {

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

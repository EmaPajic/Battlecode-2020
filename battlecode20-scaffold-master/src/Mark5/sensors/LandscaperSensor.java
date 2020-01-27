package Mark5.sensors;

import Mark5.utils.*;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class LandscaperSensor {
    public static MapLocation combatWaypoint;
    public static MapLocation nearestNetGun;

    public static void init() {
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];
    }

    public static void sense() throws GameActionException {
        if(nearestNetGun == null && HQLocation != null) nearestNetGun = HQLocation;

        nearestBuriedFriendlyBuilding = null;
        nearestEnemyBuilding = null;
        nearestWater = null;
        combatWaypoint = null;
        overlapLocations.clear();
        buriedFriendlyBuildings.clear();
        enemyDrones.clear();
        nearestEnemyDrone = null;

        int xMin = rc.getLocation().x - 2;
        int yMin = rc.getLocation().y - 2;
        int xMax = rc.getLocation().x + 2;
        int yMax = rc.getLocation().y + 2;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    elevation[i][j] = rc.senseElevation(location);
                    water[i][j] = rc.senseFlooding(location);
                    occupied[i][j] = false;
                    if(rc.senseFlooding(location)){
                        if(Navigation.aerialDistance(nearestWater) > Navigation.aerialDistance(location)) {
                            nearestWater = location;
                            continue;
                        }
                        if(Navigation.aerialDistance(nearestWater) == Navigation.aerialDistance(location))
                            if(rc.senseElevation(nearestWater) < rc.senseElevation(location))
                                nearestWater = location;
                    }
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
                            Strategium.updatePotentialEnemyHQLocations();
                            Wall.init();
                        }
                    case NET_GUN:
                        if(Navigation.aerialDistance(nearestNetGun) > Navigation.aerialDistance(robot))
                            nearestNetGun = robot.location;

                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:


                        if (robot.dirtCarrying > 0 && !Lattice.isAdjacentToWater(robot.location) &&
                                (!robot.location.isAdjacentTo(HQLocation) || rc.getRoundNum() < 300 ||
                                        robot.location == HQLocation)) {
                            if (Navigation.aerialDistance(robot) <
                                    Navigation.aerialDistance(nearestBuriedFriendlyBuilding))
                                nearestBuriedFriendlyBuilding = robot.location;

                            buriedFriendlyBuildings.add(robot);
                        }


                        break;
                }
            } else {

                switch (robot.type) {
                    case HQ:
                        if (enemyHQLocation == null) {
                            enemyHQLocation = robot.location;
                            potentialEnemyHQLocations.clear();
                            Blockchain.reportEnemyHQLocation(2);
                        }
                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:
                    case NET_GUN:
                        if (!enemyBuildings.contains(robot.location)) enemyBuildings.add(robot.location);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyBuilding)){
                            nearestEnemyBuilding = robot.location;
                        }
                        break;

                    case DELIVERY_DRONE:
                        enemyDrones.add(robot);
                        if(Navigation.aerialDistance(nearestEnemyDrone) > Navigation.aerialDistance(robot))
                            nearestEnemyDrone = robot;
                }

            }

        }

        for(RobotInfo friendly : buriedFriendlyBuildings)
            if(friendly.type == RobotType.HQ && friendly.dirtCarrying > 1){
                if(rc.getLocation().isAdjacentTo(HQLocation) ||
                        !rc.getLocation().isAdjacentTo(nearestBuriedFriendlyBuilding)) {
                    nearestBuriedFriendlyBuilding = HQLocation;
                    combatWaypoint = HQLocation;
                    break;
                }
            }

        if(Navigation.aerialDistance(enemyHQLocation) < 4 &&
                (Navigation.aerialDistance(nearestEnemyBuilding) > 1 ||
                        Navigation.aerialDistance(enemyHQLocation) <= 1)) {
            nearestEnemyBuilding = enemyHQLocation;
            combatWaypoint = enemyHQLocation;
        }

        if (combatWaypoint == null)
            if(Navigation.aerialDistance(nearestBuriedFriendlyBuilding) <
                    Navigation.aerialDistance(nearestEnemyBuilding))
                        combatWaypoint = nearestBuriedFriendlyBuilding;
            else combatWaypoint = nearestEnemyBuilding;



        if(Navigation.aerialDistance(nearestEnemyBuilding, nearestBuriedFriendlyBuilding) <= 2)
            for(Direction dir : dir8) {
                MapLocation location = nearestEnemyBuilding.add(dir);
                if(!rc.onTheMap(location)) continue;
                if(location.isAdjacentTo(nearestBuriedFriendlyBuilding) && !location.equals(nearestEnemyBuilding))
                    overlapLocations.add(location);
            }



    }

}

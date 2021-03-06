package SeedingBot.sensors;

import SeedingBot.utils.Lattice;
import SeedingBot.utils.Navigation;
import SeedingBot.utils.Strategium;
import SeedingBot.utils.Wall;
import battlecode.common.*;

import static SeedingBot.RobotPlayer.dir8;
import static SeedingBot.RobotPlayer.rc;

import static SeedingBot.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class LandscaperSensor {

    public static void init() {
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];
    }

    public static void sense() throws GameActionException {

        nearestBuriedFriendlyBuilding = null;
        nearestEnemyBuilding = null;
        nearestWater = null;
        overlapLocations.clear();
        enemyDrones.clear();
        buriedFriendlyBuildings.clear();

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
                        if(Navigation.aerialDistance(nearestWater) > Navigation.aerialDistance(location) ||
                                (Navigation.aerialDistance(nearestWater) == Navigation.aerialDistance(location) &&
                                        elevation[nearestWater.x][nearestWater.y] < elevation[location.x][location.y]))
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

                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:
                    case NET_GUN:

                        if (robot.dirtCarrying > 0 && !Lattice.isAdjacentToWater(robot.location)) {
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
                        if(rc.onTheMap(location))
                        if(location.isAdjacentTo(enemy) && !occupied[location.x][location.y])
                            overlapLocations.add(location);
                    }
                }
            }

        for (MapLocation enemy : enemyBuildings)
            if (Navigation.aerialDistance(enemy) < Navigation.aerialDistance(nearestEnemyBuilding))
                nearestEnemyBuilding = enemy;





    }

}

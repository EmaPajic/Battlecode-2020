package Mark5.sensors;

import Mark5.utils.Navigation;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;

import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;
import static java.lang.Math.*;

public class DesignSchoolSensor {
    public static int numLandscapers;
    public static int numEnemyUnits;
    public static boolean dangerOfWater;

    public static Direction shouldCreateLandscaper() {
        // water, enemy swarm  vs our forces, buildings in danger

        if(enemyBuildings.size() > numLandscapers){
            return rc.getLocation().directionTo(nearestEnemyBuilding);
        }
        if(dangerOfWater){
            return rc.getLocation().directionTo((nearestWater));
        }
        if(numEnemyUnits > numLandscapers){
            return rc.getLocation().directionTo((nearestEnemyUnit.location));
        }
        for(RobotInfo building : buriedFriendlyBuildings){
            if(building.getDirtCarrying() > numLandscapers) {
                return rc.getLocation().directionTo(building.location);
            }
        }

        return null;

    }


    public void init() {

        water = new boolean[9][9];
        occupied = new boolean[9][9];





    }

    public static void sense() throws GameActionException {

        //enemyUnits.clear();

        enemyBuildings.clear();
        buriedFriendlyBuildings.clear();
        numLandscapers = 0;
        numEnemyUnits = 0;
        dangerOfWater = false;

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        for (int i = min(xMin, 0); i <= max(xMax, rc.getMapWidth() - 1); i++)
            for (int j = min(yMin, 0); j <= max(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {

                    occupied[i][j] = false;
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


        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {

            occupied[robot.location.x][robot.location.y] = true;

            if (robot.team == myTeam) {

                switch (robot.type) {

                    case HQ:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    case NET_GUN:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    case DESIGN_SCHOOL:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    case FULFILLMENT_CENTER:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    case VAPORATOR:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    case REFINERY:
                        if (!buriedFriendlyBuildings.contains(robot))
                            buriedFriendlyBuildings.add(robot);
                    // moving robots
                    case LANDSCAPER:
                        ++numLandscapers;
                }

            }else {

                switch (robot.type) {

                    case HQ:
                        if(!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);

                    case NET_GUN:

                        if (!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);

                    case DESIGN_SCHOOL:
                        if (!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);
                    case FULFILLMENT_CENTER:
                        if (!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);
                    case VAPORATOR:
                        if (!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);
                    case REFINERY:
                        if (!enemyBuildings.contains(robot))
                            enemyBuildings.add(robot.location);


                    case LANDSCAPER:
                        ++numEnemyUnits;


                }

            }

        }




        //if (!rc.isReady() && nearestWater == null) findWater();
        if(nearestWater != null){
            dangerOfWater = findWater();
        }

    }

    public static boolean findWater() {
        if (foundWater)
            for (int i = rc.getMapWidth(); i-- > 0; )
                for (int j = rc.getMapHeight(); j-- > 0; ) {
                    if (water[i][j]) if (Navigation.aerialDistance(i, j) < Navigation.aerialDistance(nearestWater))
                        nearestWater = new MapLocation(i, j);
                }

        if (nearestWater != null) return true;
        return false;

    }
}

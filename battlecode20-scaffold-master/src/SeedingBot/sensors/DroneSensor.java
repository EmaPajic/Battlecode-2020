package SeedingBot.sensors;

import SeedingBot.utils.Navigation;
import SeedingBot.utils.Strategium;
import SeedingBot.utils.Wall;
import battlecode.common.*;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;

import static SeedingBot.RobotPlayer.dir8;
import static SeedingBot.RobotPlayer.rc;
import static SeedingBot.utils.Strategium.*;

public class DroneSensor {

    public static DroneSensor strategium = null;
    public static RobotInfo potentialTaxiPayload = null;

    public static void init() {

        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        robotsMet = new boolean[GameConstants.MAX_ROBOT_ID];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];

    }

    public static void sense() throws GameActionException {

        enemyUnits.clear();
        enemyDrones.clear();
        alliedDrones.clear();
        nearestEnemyDrone = null;
        nearestEnemyUnit = null;
        blockedUnit = null;
        blockingUnit = null;
        nearestLandscaper = null;

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

                if (robot.type == RobotType.HQ) {
                    if (HQLocation == null) {
                        HQLocation = robot.location;
                        Wall.init();
                        Strategium.updatePotentialEnemyHQLocations();
                    }
                } else if(robot.type == RobotType.MINER) {
                    boolean NearFulfillmentCenter = false;
                    for(Direction dir : dir8) {
                        if(rc.canSenseLocation(robot.location.add(dir))) {
                            RobotInfo maybeFulfillment = rc.senseRobotAtLocation(robot.location.add(dir));
                            if(maybeFulfillment != null) if(maybeFulfillment.type == RobotType.FULFILLMENT_CENTER &&
                                                            maybeFulfillment.getTeam() == myTeam) {
                                NearFulfillmentCenter = true;
                                break;
                            }
                        }
                    }
                    boolean isRushMiner = true;
                    if(robot.getSoupCarrying() > 0)
                        isRushMiner = false;
                    if(NearFulfillmentCenter && isRushMiner)
                        potentialTaxiPayload = robot;
                }

            } else {

                switch (robot.type) {

                    case HQ:
                        if (enemyHQLocation == null) {
                            enemyHQLocation = robot.location;
                            potentialEnemyHQLocations.clear();
                            enemyNetGuns.add(robot.location);
                            enemyBuildings.add(robot.location);
                        }
                    case NET_GUN:

                        if (!enemyNetGuns.contains(robot.location)) enemyNetGuns.add(robot.location);

                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                    case VAPORATOR:
                    case REFINERY:

                        if (!enemyBuildings.contains(robot.location)) enemyBuildings.add(robot.location);
                        break;

                    case DELIVERY_DRONE:

                        enemyDrones.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                            nearestEnemyDrone = robot;
                        break;

                    default:
                        enemyUnits.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyUnit))
                            nearestEnemyUnit = robot;

                }

            }

        }




        if (!rc.isReady() && nearestWater == null) findWater();

    }

    public static void findWater() {
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

}

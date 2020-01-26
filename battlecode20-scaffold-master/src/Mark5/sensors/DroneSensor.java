package Mark5.sensors;

import Mark5.utils.*;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.*;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class DroneSensor {

    public static DroneSensor strategium = null;
    public static RobotInfo potentialTaxiPayload = null;
    private static RobotType targetType;

    private static int priority(RobotType type){
        switch (type){
            case COW: return 0;
            case LANDSCAPER: return 1;
            case MINER: return 2;
            case DELIVERY_DRONE: return 3;
        }
        return -1;
    }

    public static void init() {

        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        robotsMet = new boolean[GameConstants.MAX_ROBOT_ID + 1];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];
        targetType = RobotType.COW;

    }

    public static void sense() throws GameActionException {

        enemyUnits.clear();
        enemyDrones.clear();
        alliedDrones.clear();
        nearestEnemyDrone = null;
        if(nearestEnemyUnit != null)
            if(rc.canSenseLocation(nearestEnemyUnit.location)){
                nearestEnemyUnit = null;
                targetType = RobotType.COW;
            }
        blockedUnit = null;
        blockingUnit = null;
        nearestLandscaper = null;
        nearestMiner = null;
        nearestPayload = null;


        int xMin = max(0, rc.getLocation().x - 4);
        int yMin = max(0, rc.getLocation().y - 4);
        int xMax = min(rc.getLocation().x + 4, rc.getMapWidth() - 1);
        int yMax = min(rc.getLocation().y + 4, rc.getMapHeight() - 1);
        for (int i = xMin; i <= xMax; i++)
            for (int j = yMin; j <= yMax; j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
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
                switch (robot.type) {
                    case HQ:
                        if (HQLocation == null) {
                            HQLocation = robot.location;
                            //TwoMinerController.searchRoute.add(0, HQLocation);
                            Wall.init();
                            Strategium.updatePotentialEnemyHQLocations();
                        }
                        break;
                    case MINER:
                        /*
                        boolean NearFulfillmentCenter = false;
                        for (Direction dir : dir8) {
                            if (rc.canSenseLocation(robot.location.add(dir))) {
                                RobotInfo maybeFulfillment = rc.senseRobotAtLocation(robot.location.add(dir));
                                if (maybeFulfillment != null)
                                    if (maybeFulfillment.type == RobotType.FULFILLMENT_CENTER &&
                                            maybeFulfillment.getTeam() == myTeam) {
                                        NearFulfillmentCenter = true;
                                        break;
                                    }
                            }
                        }
                        boolean isRushMiner = true;
                        if (robot.getSoupCarrying() > 0)
                            isRushMiner = false;
                        if (NearFulfillmentCenter && isRushMiner)
                            potentialTaxiPayload = robot;
                        */
                        if(enemyHQLocation != null) {
                            if(Navigation.aerialDistance(robot.location, enemyHQLocation) <= 2) {
                                break;
                            }
                        }
                        if(nearestMiner == null)
                            nearestMiner = robot;
                        else
                        if(Navigation.aerialDistance(nearestMiner) > Navigation.aerialDistance(robot)) {
                            nearestMiner = robot;
                        }

                        // test for blockage
                        int cnt = 0;
                        for(Direction dir : dir8) {
                            MapLocation adjacentLoc = robot.location.add(dir);
                            if(rc.canSenseLocation(adjacentLoc)) {
                                if(rc.senseFlooding(adjacentLoc) ||
                                        Math.abs(rc.senseElevation(robot.location) - rc.senseElevation(adjacentLoc)) > 3)
                                    ++cnt;
                            }
                        }
                        if(cnt >= 7)
                            blockedUnit = robot;
                        break;
                    case LANDSCAPER:
                        if(enemyHQLocation != null) {
                            if(Navigation.aerialDistance(robot.location, enemyHQLocation) <= 2) {
                                break;
                            }
                        }
                        if(Navigation.aerialDistance(robot.location, HQLocation) > 1) {
                            if(nearestLandscaper == null)
                                nearestLandscaper = robot;
                            else
                                if(Navigation.aerialDistance(nearestLandscaper) > Navigation.aerialDistance(robot)) {
                                    nearestLandscaper = robot;
                                }
                        }

                        // test for blockage
                        int cntL = 0;
                        if(blockedUnit != null || Navigation.aerialDistance(robot.location, HQLocation) == 1)
                            break;
                        for(Direction dir : dir8) {
                            MapLocation adjacentLoc = robot.location.add(dir);
                            if(rc.canSenseLocation(adjacentLoc)) {
                                if(rc.senseFlooding(adjacentLoc) ||
                                        Math.abs(rc.senseElevation(robot.location) - rc.senseElevation(adjacentLoc)) > 3)
                                    ++cntL;
                            }
                        }
                        if(cntL >= 7)
                            blockedUnit = robot;
                        break;

                    case DELIVERY_DRONE:
                        if(!robotsMet[robot.getID()]){
                            robotsMet[robot.getID()] = true;
                            numDronesMet++;
                            if(robot.getID() < rc.getID()) dronesMetWithLowerID++;
                        }
                }
            } else {

                switch (robot.type) {

                    case HQ:
                        if (enemyHQLocation == null) {
                            enemyHQLocation = robot.location;
                            potentialEnemyHQLocations.clear();
                            enemyNetGuns.add(new NetGun(robot.location, -1, 10));
                            enemyBuildings.add(robot.location);
                            Blockchain.reportEnemyHQLocation(2);
                        }
                    case NET_GUN:

                        if (!enemyNetGuns.contains(new NetGun(robot))) enemyNetGuns.add(new NetGun(robot));

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
                        if(robot.isCurrentlyHoldingUnit()){
                            RobotInfo payload = rc.senseRobot(robot.heldUnitID);
                            if(payload.team != rc.getTeam()) {
                                if (priority(payload.type) >= priority(targetType))
                                    if (Navigation.aerialDistance(robot) <
                                            Navigation.aerialDistance(nearestEnemyUnit)) {
                                        nearestEnemyUnit = robot;
                                        targetType = payload.type;
                                    }
                            }
                        }
                        break;

                    default:
                        enemyUnits.add(robot);
                        if (priority(robot.type) >= priority(targetType))
                            if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyUnit)) {
                                nearestEnemyUnit = robot;
                                targetType = robot.type;
                            }

                }

            }

        }

        if(nearestMiner != null)
            nearestPayload = nearestMiner;
        if(nearestLandscaper != null && nearestPayload == null)
            nearestPayload = nearestLandscaper;


        if (!rc.isReady() && (nearestWater == null || Navigation.frustration >= 100)) findWater();

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

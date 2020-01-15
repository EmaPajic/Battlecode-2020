package Mark5.sensors;

import Mark5.utils.Navigation;
import Mark5.utils.Wall;
import battlecode.common.*;

import java.util.Iterator;
import java.util.Map;

import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.*;

public class DroneSensor {

    public static DroneSensor strategium = null;

    public static void init() {

        water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        robotsMet = new boolean[GameConstants.MAX_ROBOT_ID];

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


        RobotInfo[] robots = rc.senseNearbyRobots();

        for (RobotInfo robot : robots) {

            if (robot.team == myTeam) {

                if (robot.type == RobotType.HQ) {
                    if (HQLocation == null) {
                        HQLocation = robot.location;
                        Wall.init();
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
                } else if (HQLocation != null) {
                    if (robot.type == RobotType.LANDSCAPER) {
                        if (robot.location.equals(Wall.launchPad)){
                            System.out.println("ZAGLAVIO SE " + robot + " " + Wall.isLaunchPadBlocked());
                        }
                        if (robot.location.equals(Wall.launchPad) && Wall.isLaunchPadBlocked()) {

                            blockedUnit = robot;
                        }
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestLandscaper) &&
                                Navigation.aerialDistance(robot.location, HQLocation) <= 2) nearestLandscaper = robot;
                    } else if (robot.type == RobotType.MINER) {
                        if (Wall.stuckOnWall(robot.location)) blockingUnit = robot;
                    }
                }
                if (robot.type == RobotType.DELIVERY_DRONE) {
                    alliedDrones.add(robot);

                    if (!robotsMet[robot.ID]) {
                        robotsMet[robot.ID] = true;
                        numDronesMet++;
                        if (robot.ID < rc.getID()) dronesMetWithLowerID++;
                    }
                }

            } else {

                if (robot.type == RobotType.HQ) {
                    enemyHQLocation = robot.location;
                    potentialEnemyHQLocations.clear();
                    enemyNetGuns.put(robot.location, robot);
                } else if (robot.type == RobotType.NET_GUN) enemyNetGuns.put(robot.location, robot);
                else if (robot.type.isBuilding()) enemyBuildings.put(robot.location, robot);
                else if (robot.type == RobotType.DELIVERY_DRONE) {
                    enemyDrones.add(robot);
                    if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                        nearestEnemyDrone = robot;
                } else {
                    enemyUnits.add(robot);
                    if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyUnit))
                        nearestEnemyUnit = robot;
                }

            }

        }

        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));

        Iterator<Map.Entry<MapLocation, RobotInfo>> it = enemyBuildings.entrySet().iterator();
        while (it.hasNext()) {
            RobotInfo building = it.next().getValue();
            if (rc.canSenseLocation(building.location))
                if (!rc.canSenseRobot(building.ID)) {
                    it.remove();
                }
        }

        it = enemyNetGuns.entrySet().iterator();
        while (it.hasNext()) {
            RobotInfo netGun = it.next().getValue();
            if (rc.canSenseLocation(netGun.location))
                if (!rc.canSenseRobot(netGun.ID)) {
                    it.remove();
                }
        }

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

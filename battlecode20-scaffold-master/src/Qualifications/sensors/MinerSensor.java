package Qualifications.sensors;

import Qualifications.utils.*;
import battlecode.common.*;

import java.util.Iterator;
import java.util.Map;

import static Qualifications.RobotPlayer.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static Qualifications.utils.Strategium.*;

public class MinerSensor {


    public static int visibleSoup;
    public static boolean seenWater = false;
    public static MapLocation vacantBuildSpot;

    public static void init() {
        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        //explored = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        //elevation = new int[rc.getMapWidth()][rc.getMapHeight()];
        occupied = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        //water = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        dirSafetyCacheValid = new int[10];
        dirSafetyCache = new boolean[10];
    }

    public static void sense() throws GameActionException {

        enemyDrones.clear();
        nearestEnemyDrone = null;
        visibleSoup = 0;
        seenWater = false;
        vacantBuildSpot = null;



        int xMin = max(0, rc.getLocation().x - 5);
        int yMin = max(0, rc.getLocation().y - 5);
        int xMax = min(rc.getLocation().x + 5, rc.getMapWidth() - 1);
        int yMax = min(rc.getLocation().y + 5, rc.getMapHeight() - 1);
        int waterLevel = (int) GameConstants.getWaterLevel(rc.getRoundNum() + 10);
        for (int i = xMin; i <= xMax; ++i)
            for (int j = yMin; j <= yMax; ++j) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    if (rc.senseFlooding(location)) seenWater = true;
                    occupied[i][j] = false;
                    if(myFun != 4 && rc.getRoundNum() <= 600) {
                        visibleSoup += rc.senseSoup(location);
                        if (rc.senseSoup(location) > 0) {
                            //explored[i][j] = true;
                            if (!soup[i][j]) {
                                ++knownSoup;
                                soup[i][j] = true;
                                if (Navigation.aerialDistance(rc.getLocation(), location) <
                                        Navigation.aerialDistance(rc.getLocation(), nearestSoup))
                                    nearestSoup = location;
                            }

                        } else if (soup[i][j]) {
                            soup[i][j] = false;
                            knownSoup--;
                            if (nearestSoup != null)
                                if (nearestSoup.x == i && nearestSoup.y == j){
                                    nearestSoup = null;
                                    for(Direction dir : dir8) {
                                        if(rc.senseSoup(location.add(dir))>0){
                                            nearestSoup = location.add(dir);
                                            break;
                                        }
                                    }
                                }
                        }
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

                        Strategium.updatePotentialEnemyHQLocations();

                    }

                }
                if (robot.type.canRefine()) if (!refineries.contains(robot.location)) refineries.add(robot.location);

            } else {
                switch (robot.type) {

                    case DELIVERY_DRONE:
                        enemyDrones.add(robot);
                        if (Navigation.aerialDistance(robot) < Navigation.aerialDistance(nearestEnemyDrone))
                            nearestEnemyDrone = robot;
                        break;
                    case HQ:
                        if (enemyHQLocation == null) {
                            potentialEnemyHQLocations.clear();
                            enemyHQLocation = robot.location;
                            currentEnemyHQTarget = enemyHQLocation;
                            Blockchain.reportEnemyHQLocation(2);
                        }
                        break;
                }


            }

        }

        //Wall.checkBaseStatus();




        if(myFun != 4 && rc.getRoundNum() <= 600) {
            nearestRefinery = null;
            for (MapLocation refinery : refineries) {
                if (refinery == HQLocation && rc.getRoundNum() > 600)
                    continue;
                if (Navigation.aerialDistance(nearestRefinery, rc.getLocation()) >
                        Navigation.aerialDistance(refinery, rc.getLocation()))
                    nearestRefinery = refinery;
            }
            if (knownSoup > 0 && nearestSoup == null) scanAllSoup();
        } else {
            if(xMin % 2 == HQLocation.x % 2) xMin++;
            if(yMin % 2 == HQLocation.y % 2) yMin++;
            for (int i = xMin; i <= xMax; i+=2)
                for (int j = yMin; j <= yMax; j+=2)
                    if(!occupied[i][j] && (i!=0 || j!=0)) {
                        MapLocation location = new MapLocation(i, j);
                        if(!rc.canSenseLocation(location)) continue;
                        if(rc.senseFlooding(location)) continue;
                        if(rc.senseElevation(location) < 8) continue;
                        if(Math.abs(rc.senseElevation(location) - rc.senseElevation(rc.getLocation())) <= 3)
                            if(Navigation.aerialDistance(vacantBuildSpot) > Navigation.aerialDistance(location))
                                vacantBuildSpot = location;
                    }
        }

        potentialEnemyHQLocations.removeIf(location -> rc.canSenseLocation(location));



    }

    private static void scanAllSoup() {
        if (knownSoup <= 0) {
            nearestSoup = null;
            return;
        }
        for (int i = rc.getMapWidth(); i-- > 0; )
            for (int j = rc.getMapHeight(); j-- > 0; )
                if (soup[i][j])
                    if(Navigation.aerialDistance(rc.getLocation(), i, j) <
                        Navigation.aerialDistance(rc.getLocation(), nearestSoup)) nearestSoup = new MapLocation(i, j);
    }
}

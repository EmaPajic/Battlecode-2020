package Qualifications.robots;

import Qualifications.sensors.FulfillmentCenterSensor;
import Qualifications.sensors.MinerSensor;
import Qualifications.utils.BFS;
import Qualifications.utils.Lattice;
import Qualifications.utils.Navigation;
import Qualifications.utils.Strategium;
import battlecode.common.*;

import java.util.List;
import java.util.Map;

import static Qualifications.RobotPlayer.*;

public class RushMiner {

    static boolean builtFulfillment = false;
    static boolean followBfs = false;
    static int numDesignSchools = 0;
    static int numNetGuns = 0;
    static int numEnemyDrones = 0;
    static int numEnemyFulfillmentCenters = 0;
    static boolean waterDanger = false;
    static int waterLevel = 0;
    static MapLocation nearestEnemyDroneLoc = null;
    static MapLocation nearestEnemyFulfillmentCenterLoc = null;
    static boolean foundEnemyHQ;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        if (Strategium.enemyHQLocation != null) {
            Strategium.currentEnemyHQTarget = Strategium.enemyHQLocation;
            buildToAttack();
            return;
        } else if (Strategium.currentEnemyHQTarget == null) {
            Strategium.currentEnemyHQTarget = Strategium.potentialEnemyHQLocations.get(0);
        }
        waterDanger = false;
        waterLevel = (int) GameConstants.getWaterLevel(rc.getRoundNum() + 10);
        rushToEnemyHQ();
    }

    public static void rushToEnemyHQ() throws GameActionException {
        if (rc.canSenseLocation(Strategium.currentEnemyHQTarget)) {
            //System.out.println("Enemy HQ: " + Strategium.currentEnemyHQTarget);
            RobotInfo info = rc.senseRobotAtLocation(Strategium.currentEnemyHQTarget);
            if (info != null) {
                if (info.getTeam() == Strategium.opponentTeam && info.getType() == RobotType.HQ) {
                    buildToAttack();
                    return;
                } else {
                    TwoMinerController.updateEnemyHQTarget();
                }
            } else {
                TwoMinerController.updateEnemyHQTarget();
            }
        }

        /*if (Navigation.aerialDistance(Strategium.enemyHQLocation) <= 3) {
            buildToAttack();
            return;
        }*/

        Direction goToDir = Navigation.moveTowards(Strategium.currentEnemyHQTarget);
        MapLocation goToLoc = rc.adjacentLocation(goToDir);
        Direction circumnavigateDir = Direction.CENTER;
        if (followBfs) {
            circumnavigateDir = BFS.step(Strategium.currentEnemyHQTarget);
            if (goToDir == circumnavigateDir)
                followBfs = false;
        }
        if (MinerSensor.seenWater) {
            MapLocation loc = rc.getLocation();
            waterDanger = true;
            for (int i = 0; i < 4; ++i) {
                loc = loc.add(goToDir);
                if (rc.canSenseLocation(loc))
                if (rc.senseElevation(loc) > waterLevel) waterDanger = false;
            }
        }
        if (Strategium.canSafelyMove(goToDir) && !followBfs && !waterDanger) {
            System.out.println("Danger? " + waterDanger);
            rc.move(goToDir);
        } else {
            //build fulfillment center and wait for drone
            //
            //System.out.println("BFSSTART");
            rc.setIndicatorLine(rc.getLocation(), Strategium.currentEnemyHQTarget,
                    255, 255, 255);
            if (!followBfs) {
                circumnavigateDir = BFS.step(Strategium.currentEnemyHQTarget);
            }
            //System.out.println("BFSEND " + circumnavigateDir);
            if (circumnavigateDir != Direction.CENTER) {
                if (MinerSensor.seenWater) {
                    MapLocation loc = rc.getLocation();
                    waterDanger = true;
                    for (int i = 0; i < 4; ++i) {
                        loc = loc.add(circumnavigateDir);
                        if (rc.canSenseLocation(loc))
                        if (rc.senseElevation(loc) > waterLevel) waterDanger = false;
                    }
                }
                if (Strategium.canSafelyMove(circumnavigateDir) && !waterDanger) {
                    followBfs = true;
                    rc.move(circumnavigateDir);
                    return;
                }
            }
            if (!builtFulfillment) {
                    //build
                    for (Direction buildDir : dir8)
                        if (Lattice.isBuildingSite(rc.getLocation().add(buildDir)) &&
                                rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDir) && rc.isReady()) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDir);
                            builtFulfillment = true;
                        }
                    for (Direction buildDir : dir8)
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDir)) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDir);
                            builtFulfillment = true;
                        }

                }

        }
    }

    public static void buildToAttack() throws GameActionException {
        //System.out.println("BILD TRN: " + buildTurn);
        RobotInfo[] robots = rc.senseNearbyRobots();
        numNetGuns = 0;
        numDesignSchools = 0;
        numEnemyDrones = 0;
        numEnemyFulfillmentCenters = 0;

        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DESIGN_SCHOOL && robot.getTeam() == Strategium.myTeam) {
                ++numDesignSchools;
            }
            if (robot.getType() == RobotType.NET_GUN && robot.getTeam() == Strategium.myTeam) {
                ++numNetGuns;
            }
            if (robot.getType() == RobotType.FULFILLMENT_CENTER && robot.getTeam() == Strategium.opponentTeam) {
                ++numEnemyFulfillmentCenters;
                if (nearestEnemyFulfillmentCenterLoc == null)
                    nearestEnemyFulfillmentCenterLoc = robot.getLocation();
                else if (Navigation.aerialDistance(robot.getLocation()) <
                        Navigation.aerialDistance(nearestEnemyFulfillmentCenterLoc)) {
                    nearestEnemyFulfillmentCenterLoc = robot.getLocation();
                }
            }
            if (robot.getType() == RobotType.DELIVERY_DRONE && robot.getTeam() == Strategium.opponentTeam) {
                ++numEnemyDrones;
                if (nearestEnemyDroneLoc == null)
                    nearestEnemyDroneLoc = robot.getLocation();
                else if (Navigation.aerialDistance(robot.getLocation()) <
                        Navigation.aerialDistance(nearestEnemyDroneLoc)) {
                    nearestEnemyDroneLoc = robot.getLocation();
                }
            }
        }
        if (numNetGuns < 1 && (numEnemyDrones > 0 || numEnemyFulfillmentCenters > 0)) {
            buildNetGunNearEnemy();
        }
        if (numDesignSchools == 0) {
            TwoMinerController.buildDesignCenterNearEnemy();
        }
    }

    public static boolean buildNetGunNearEnemy() throws GameActionException {


        if (Navigation.aerialDistance(Strategium.currentEnemyHQTarget) <= 4) {
            Direction dirToEnemy = null;
            if (nearestEnemyDroneLoc != null)
                dirToEnemy = rc.getLocation().directionTo(nearestEnemyDroneLoc);
            else
                dirToEnemy = rc.getLocation().directionTo(nearestEnemyFulfillmentCenterLoc);
            List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemy.opposite()));
            for(Direction dir : towards)
                if(tryBuild(RobotType.NET_GUN, dir)) {
                    return true;
                }
            for (Direction dir : dir8) {
                if (rc.canBuildRobot(RobotType.NET_GUN, dir) &&
                        Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3)
                    if (tryBuild(RobotType.NET_GUN, dir)) {
                        return true;
                    }
            }
        }
        return false;

    }

}

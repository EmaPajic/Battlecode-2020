package Mark5.robots;

import Mark5.utils.BFS;
import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.Map;

import static Mark5.RobotPlayer.*;

public class RushMiner {

    static boolean builtFulfillment = false;
    static boolean followBfs = false;
    static int numDesignSchools = 0;
    static int numNetGuns = 0;

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
        if (Strategium.canSafelyMove(goToDir) && !followBfs) {
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
            if (circumnavigateDir != Direction.CENTER)
                if (Strategium.canSafelyMove(circumnavigateDir)) {
                    followBfs = true;
                    rc.move(circumnavigateDir);
                    return;
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
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DESIGN_SCHOOL && robot.getTeam() == Strategium.myTeam) {
                ++numDesignSchools;
            }
            if (robot.getType() == RobotType.NET_GUN && robot.getTeam() == Strategium.myTeam) {
                ++numNetGuns;
            }
        }
        if (numDesignSchools == 0) {
            TwoMinerController.buildDesignCenterNearEnemy();
        }
        if (numNetGuns < 2) {
            buildNetGunNearEnemy();
        }
    }

    public static boolean buildNetGunNearEnemy() throws GameActionException {


        if (Navigation.aerialDistance(Strategium.currentEnemyHQTarget) <= 4) {
            for (Direction dir : dir8) {
                if (rc.canBuildRobot(RobotType.NET_GUN, dir) &&
                        Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3)
                    if (tryBuild(RobotType.NET_GUN, dir)) {
                        return true;
                    }
            }
        }
        Navigation.bugPath(Strategium.currentEnemyHQTarget);
        return false;

    }

}

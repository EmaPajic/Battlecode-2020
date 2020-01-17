package Mark5.robots;

import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.Map;

import static Mark5.RobotPlayer.*;

public class RushMiner {

    static int buildTurn = 0;
    static boolean builtFulfillment = false;

    static boolean foundEnemyHQ;
    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        if (Strategium.enemyHQLocation != null) {
            Strategium.currentEnemyHQTarget = Strategium.enemyHQLocation;
        }
        else if (Strategium.currentEnemyHQTarget == null) {
                Strategium.currentEnemyHQTarget = Strategium.potentialEnemyHQLocations.get(0);
            }
        rushToEnemyHQ();
    }

    public static void rushToEnemyHQ() throws GameActionException{
        if (rc.canSenseLocation(Strategium.currentEnemyHQTarget)) {
            System.out.println("Enemy HQ: " + Strategium.currentEnemyHQTarget);
            RobotInfo info = rc.senseRobotAtLocation(Strategium.currentEnemyHQTarget);
            if (info != null) {
                if (info.getTeam() == Strategium.opponentTeam && info.getType() == RobotType.HQ) {
                    buildToAttack();
                } else {
                    TwoMinerController.updateEnemyHQTarget();
                }
            } else {
                TwoMinerController.updateEnemyHQTarget();
            }
        }

        Direction goToDir = Navigation.moveTowards(Strategium.currentEnemyHQTarget);
        rc.setIndicatorLine(rc.getLocation(), Strategium.currentEnemyHQTarget, 255, 255, 255);
        MapLocation goToLoc = rc.adjacentLocation(goToDir);
        if (rc.senseFlooding(goToLoc) ||
                Math.abs(rc.senseElevation(goToLoc) - rc.senseElevation(rc.getLocation())) > 3) {
            //build fulfillment center and wait for drone
            //
            if (!builtFulfillment) {
                //build
                for (Direction buildDir : dir8)
                    if (Lattice.isBuildingSite(rc.getLocation().add(buildDir)) &&
                            rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDir) && rc.isReady()) {
                        rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDir);
                        builtFulfillment = true;
                    }
                for (Direction buildDir : dir8)
                    if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDir)) {
                        rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDir);
                        builtFulfillment = true;
                    }

            }
        }
        else {
            if (Strategium.canSafelyMove(goToDir) && rc.isReady()) {
                rc.move(goToDir);
            }
        }
    }

    public static void buildToAttack() throws GameActionException{

        if (buildTurn == 0) {
            TwoMinerController.buildDesignCenterNearEnemy();
            ++buildTurn;
        }
        else {
            buildNetGunNearEnemy();
        }
    }

    public static void buildNetGunNearEnemy() throws GameActionException {
        // Build DesignCenter near enemy
        if (rc.canSenseLocation(Strategium.currentEnemyHQTarget)) {
            RobotInfo info = rc.senseRobotAtLocation(Strategium.currentEnemyHQTarget);
            if (info != null)
                if (info.getTeam() == Strategium.opponentTeam && info.getType() == RobotType.HQ) {
                    if (Navigation.aerialDistance(Strategium.currentEnemyHQTarget) > 4) {
                        Navigation.bugPath(Strategium.currentEnemyHQTarget);
                    } else {
                        for (Direction dir : dir8) {
                            if (rc.canBuildRobot(RobotType.NET_GUN, dir) &&
                                    Navigation.aerialDistance(rc.getLocation().add(dir), Strategium.currentEnemyHQTarget) <= 3) {
                                tryBuild(RobotType.DESIGN_SCHOOL, dir);
                            }
                        }
                    }
                }
        }
    }

}

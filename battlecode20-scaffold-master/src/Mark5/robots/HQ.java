package Mark5.robots;

import Mark5.RobotPlayer;
import Mark5.sensors.HQSensor;
import Mark5.utils.Blockchain;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import java.awt.*;
import java.util.List;

import static Mark5.RobotPlayer.rc;


public class HQ {
    public static void run(){
        rc.getTeamSoup();
    }

    public static void spamBlockchain() throws GameActionException {
        if (rc.getRoundNum() == 1) {
            for (int[] msg : Blockchain.falseMessages) {
                Blockchain.sendFalseMessage(msg, 1);
            }
        }
        if (rc.getRoundNum() == 2) {
            Blockchain.sendFalseMessage(Blockchain.false2ndturn, 2);
        }
        if (rc.getRoundNum() == 3) {
            Blockchain.sendFalseMessage(Blockchain.false3rdTurn, 2);
        }

    }

    public static void produceMiners() throws GameActionException {

        if (RobotPlayer.numMiners >= 3) {
            if (Strategium.enemyHQLocation != null)
                if (!rc.canSenseLocation(Strategium.enemyHQLocation)) {
                    RobotInfo[] robots = rc.senseNearbyRobots();
                    for (RobotInfo robot : robots) {
                        if (robot.getTeam() == Strategium.opponentTeam &&
                                (robot.getType() == RobotType.MINER ||
                                        robot.getType() == RobotType.LANDSCAPER))
                            return;
                    }
                }
        }

        if(RobotPlayer.numMiners < HQSensor.totalMiners && rc.getRoundNum() < 600) {
            if (rc.getRoundNum() == 1) {
                Direction dirToCenter = rc.getLocation().directionTo(
                        new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToCenter.opposite()));
                for (Direction dir : towards)
                    if (RobotPlayer.tryBuild(RobotType.MINER, dir)) {
                        ++RobotPlayer.numMiners;
                        return;
                    }
                for (Direction dir : RobotPlayer.dir8)
                    if (RobotPlayer.tryBuild(RobotType.MINER, dir)) {
                        ++RobotPlayer.numMiners;
                        return;
                    }
            } else if (Strategium.nearestSoup != null) {
                Direction dirToSoup = rc.getLocation().directionTo(Strategium.nearestSoup);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToSoup.opposite()));
                for (Direction dir : towards)
                    if (RobotPlayer.tryBuild(RobotType.MINER, dir)) {
                        ++RobotPlayer.numMiners;
                        return;
                    }
            }
            for (Direction dir : RobotPlayer.dir8)
                if (RobotPlayer.tryBuild(RobotType.MINER, dir)) {
                    ++RobotPlayer.numMiners;
                    return;
                }
        }
    }
}

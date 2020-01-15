package Mark5.sensors;

import Mark5.RobotPlayer;
import Mark5.utils.Wall;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;

public class IndustrySensor {

    public static void sense() throws GameActionException {

        if (HQLocation == null) {
            for (RobotInfo robot : rc.senseNearbyRobots()) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    HQLocation = robot.location;
                    Wall.init();
                    break;
                }

            }
        }

        if (HQLocation != null) {
            int landscaperCnt = 0;
            for(int i = 16; i-- >0;) if(RobotPlayer.rc.canSenseLocation(Wall.wall[i]))
                if(robotAt(Wall.wall[i]) == RobotType.LANDSCAPER) landscaperCnt++;

            shouldBuildLandscaper = landscaperCnt < 15;
            System.out.println("LANCPED:" + Wall.launchPad + " " + robotAt(Wall.launchPad));
            if(robotAt(Wall.launchPad) != null) shouldBuildLandscaper = false;

        } else {
            shouldBuildLandscaper = rc.getType() == RobotType.DESIGN_SCHOOL;
        }

    }

}

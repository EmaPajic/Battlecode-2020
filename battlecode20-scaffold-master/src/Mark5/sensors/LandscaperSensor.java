package Mark5.sensors;

import Mark5.utils.Navigation;
import Mark5.utils.Wall;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static Mark5.RobotPlayer.rc;

import static Mark5.utils.Strategium.*;

public class LandscaperSensor {

    public static void sense() throws GameActionException {

        shouldCircle = false;

        RobotInfo[] robots = rc.senseNearbyRobots();

        if (HQLocation == null) {
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    HQLocation = robot.location;
                    Wall.init();
                    break;
                }
            }
        }

        if (HQLocation != null) {
            Wall.scanWall();
            if (Navigation.aerialDistance(HQLocation) <= 2) {
                if (rc.getLocation().equals(HQLocation.translate(-1, -1))) shouldCircle = true;
                else if (!rc.getLocation().equals(HQLocation.translate(-1, -2))) {
                    shouldCircle = robotAt(Wall.clockwise(rc.getLocation())) != RobotType.LANDSCAPER;
                    System.out.println(shouldCircle);
                    if (Math.abs(rc.senseElevation(Wall.clockwise(rc.getLocation())) -
                            rc.senseElevation(rc.getLocation())) > 3) {
                        if(Math.abs(rc.senseElevation(Wall.clockwise(Wall.clockwise(rc.getLocation()))) -
                                rc.senseElevation(rc.getLocation())) > 3 ||
                                Navigation.aerialDistance(rc.getLocation(),
                                        Wall.clockwise(Wall.clockwise(rc.getLocation()))) > 1)
                            shouldCircle = false;
                    }

                    System.out.println(shouldCircle);
                    System.out.println(rc.senseElevation(Wall.clockwise(rc.getLocation())));
                } else {

                    shouldCircle = true;//robotAt(Strategium.HQLocation.translate(-1, -1)) != RobotType.LANDSCAPER;
                    if (robotAt(HQLocation.translate(-2, -2)) == RobotType.LANDSCAPER)
                        shouldCircle = false;
                    if (robotAt(HQLocation.translate(-2, -1)) == RobotType.LANDSCAPER)
                        shouldCircle = false;
                    if (Math.abs(rc.senseElevation(Wall.clockwise(rc.getLocation())) -
                            rc.senseElevation(rc.getLocation())) > 3) {
                        if(Math.abs(rc.senseElevation(Wall.clockwise(Wall.clockwise(rc.getLocation()))) -
                                rc.senseElevation(rc.getLocation())) > 3 ||
                                Navigation.aerialDistance(rc.getLocation(),
                                        Wall.clockwise(Wall.clockwise(rc.getLocation()))) > 1)
                            shouldCircle = false;
                    }



/*
        shouldCircle = true;

        int xMin = rc.getLocation().x - 4;
        int yMin = rc.getLocation().y - 4;
        int xMax = rc.getLocation().x + 4;
        int yMax = rc.getLocation().y + 4;
        int cntWall = 0;
        int cntLandscapper = 0;
        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++) {
            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                MapLocation location = new MapLocation(i, j);
                if (rc.canSenseLocation(location)) {
                    if (Navigation.aerialDistance(hqLocation, location) == 2) {
                        ++cntWall;
                        if (rc.senseRobotAtLocation(location) != null) {
                            if (rc.senseRobotAtLocation(location).getType() == RobotType.LANDSCAPER)
                                ++cntLandscapper;
                        }
                    }
*/
                }
            }

            for (RobotInfo robot : rc.senseNearbyRobots()) {
                if (robot.type != RobotType.LANDSCAPER) if (Wall.onWallAndBlocking(robots, robot.location))
                    shouldCircle = true;
            }

        }
//        if(cntLandscapper >= cntWall - 1)
//            shouldCircle = false;

        //shouldCircle = rc.getRobotCount() < 30;

    }

}

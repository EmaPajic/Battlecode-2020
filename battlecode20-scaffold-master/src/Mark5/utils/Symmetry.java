package Mark5.utils;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.rc;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

import java.lang.Math;
import java.util.Map;

public class Symmetry {
    public static MapLocation verticalSymmetryEnemyHQLocation;
    public static MapLocation horizontalSymmetryEnemyHQLocation;
    public static MapLocation diagonalSymmetryEnemyHQLocation;
    public static int nonHorizontalCount = 0;
    public static int nonVerticalCount = 0;

    public static boolean nearVerticalLine(MapLocation location) {
        if(Math.abs(location.x * 2 + 1 - rc.getMapWidth()) <= 5)
            return true;
        return false;
    }

    public static boolean nearHorizontalLine(MapLocation location) {
        if(Math.abs(location.y * 2 + 1 - rc.getMapHeight()) <= 5)
            return true;
        return false;
    }

    public static boolean nearCenter(MapLocation location) {
        if(nearHorizontalLine(location) && nearVerticalLine(location))
            return true;
        return false;
    }

    public static boolean checkBigDifference(MapLocation locA, MapLocation locB) throws GameActionException {
        if(locA == locB)
            return false;
        if(!rc.canSenseLocation(locA))
            return false;
        if(!rc.canSenseLocation(locB))
            return false;
        if(Math.abs(rc.senseElevation(locA) - rc.senseElevation(locB)) >= rc.getRoundNum() / 2)
            return true;
        if(rc.getRoundNum() > 200)
            return false;
        if(rc.senseFlooding(locA) && !rc.senseFlooding(locB))
            return true;
        if(!rc.senseFlooding(locA) && rc.senseFlooding(locB))
            return true;
        if(rc.senseSoup(locA) > 0 && rc.senseSoup(locB) == 0)
            return true;
        if(rc.senseSoup(locA) == 0 && rc.senseSoup(locB) > 0)
            return true;
        return false;
    }

    // Returns false if it can detect that the map is not vertically symmetric, otherwise true.
    public static void checkVerticalSymmetry() throws GameActionException {
        if(!nearVerticalLine(rc.getLocation()))
            return;

        int yCoord = rc.getLocation().y;
        int upYCoord = min(yCoord + 1, rc.getMapHeight() - 1);
        int middleX = (rc.getMapWidth() - 1)/2;
        int offset1X = max(0, middleX - 1);
        int[] Xs = {middleX, offset1X};
        int[] Ys = {yCoord, upYCoord};
        for(int x : Xs) {
            for(int y : Ys) {
                if(checkBigDifference(new MapLocation(x, y),
                        new MapLocation(rc.getMapWidth() - x - 1, y)))
                    ++nonVerticalCount;
            }
        }
    }

    public static void checkHorizontalSymmetry() throws GameActionException {
        if(!nearHorizontalLine(rc.getLocation()))
            return;

        int xCoord = rc.getLocation().x;
        int upXCoord = min(xCoord + 1, rc.getMapWidth() - 1);
        int middleY = (rc.getMapHeight() - 1)/2;
        int offset1Y = max(0, middleY - 1);
        int[] Ys = {middleY, offset1Y};
        int[] Xs = {xCoord, upXCoord};
        for(int x : Xs) {
            for(int y : Ys) {
                if(checkBigDifference(new MapLocation(x, y),
                        new MapLocation(x, rc.getMapHeight() - y - 1)))
                    ++nonHorizontalCount;
            }
        }
    }

    public static boolean removeWrongSymmetry(MapLocation location) throws GameActionException {
        if(Strategium.HQLocation == null)
            return false;
        if(nonHorizontalCount > 5 && location.x == Strategium.HQLocation.x &&
                                        location.y == rc.getMapHeight() - Strategium.HQLocation.y - 1) {
            System.out.println("Nije horizontal");
            return true;
        }

        if(nonVerticalCount > 5 && location.y == Strategium.HQLocation.y &&
                location.x == rc.getMapWidth() - Strategium.HQLocation.x - 1) {
            System.out.println("Nije vertikal");
            return true;
        }
        return false;
    }
}

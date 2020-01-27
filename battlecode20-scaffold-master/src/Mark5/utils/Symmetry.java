package Mark5.utils;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.rc;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

import java.lang.Math;

public class Symmetry {
    public static MapLocation verticalSymmetryEnemyHQLocation;
    public static MapLocation horizontalSymmetryEnemyHQLocation;
    public static MapLocation diagonalSymmetryEnemyHQLocation;

    public boolean nearVerticalLine(MapLocation location) {
        if(Math.abs(location.x * 2 + 1 - rc.getMapWidth()) <= 3)
            return true;
        return false;
    }

    public boolean nearHorizontalLine(MapLocation location) {
        if(Math.abs(location.y * 2 + 1 - rc.getMapHeight()) <= 3)
            return true;
        return false;
    }

    public boolean nearCenter(MapLocation location) {
        if(nearHorizontalLine(location) && nearVerticalLine(location))
            return true;
        return false;
    }

    public boolean checkBigDifference(MapLocation locA, MapLocation locB) throws GameActionException {
        if(locA == locB)
            return false;
        if(!rc.canSenseLocation(locA))
            return false;
        if(!rc.canSenseLocation(locB))
            return false;
        if(Math.abs(rc.senseElevation(locA) - rc.senseElevation(locB)) >= rc.getRoundNum() / 2)
            return true;
        return false;
    }

    // Returns false if it can detect that the map is not vertically symmetric, otherwise true.
    public boolean checkVerticalSymmetry() throws GameActionException{
        if(!nearVerticalLine(rc.getLocation()))
            return true;

        int yCoord = rc.getLocation().y;
        int upYCoord = min(yCoord + 1, rc.getMapHeight() - 1);
        int downYCoord = max(0, yCoord - 1);
        int middleX = (rc.getMapWidth() - 1)/2;
        int offset1X = max(0, middleX - 1);
        int offset2X = max(0, middleX - 2);
        int[] Xs = {middleX, offset1X, offset2X};
        int[] Ys = {yCoord, upYCoord, downYCoord};
        for(int x : Xs) {
            for(int y : Ys) {
                if(checkBigDifference(new MapLocation(x, y),
                        new MapLocation(rc.getMapWidth() - x - 1, y)))
                    return false;
            }
        }
        System.out.println("Vertikalna simetrija");

        return true;
    }

    public boolean checkHorizontalSymmetry() throws GameActionException {
        if(!nearHorizontalLine(rc.getLocation()))
            return true;

        int xCoord = rc.getLocation().x;
        int upXCoord = min(xCoord + 1, rc.getMapWidth() - 1);
        int downXCoord = max(0, xCoord - 1);
        int middleY = (rc.getMapHeight() - 1)/2;
        int offset1Y = max(0, middleY - 1);
        int offset2Y = max(0, middleY - 2);
        int[] Ys = {middleY, offset1Y, offset2Y};
        int[] Xs = {xCoord, upXCoord, downXCoord};
        for(int x : Xs) {
            for(int y : Ys) {
                if(checkBigDifference(new MapLocation(x, y),
                        new MapLocation(x, rc.getMapHeight() - y - 1)))
                    return false;
            }
        }
        System.out.println("Horizontalna simetrija");
        return true;
    }
}

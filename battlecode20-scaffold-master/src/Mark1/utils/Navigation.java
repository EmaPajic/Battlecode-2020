package Mark1.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import static Mark1.RobotPlayer.rc;

public class Navigation {

    public static Direction moveTowards(MapLocation target) {

        if(target == null) return Direction.CENTER;

        target = target.translate(-rc.getLocation().x, -rc.getLocation().y);

        if (target.x > 0 && target.y > 0) return Direction.NORTHEAST;
        if (target.x > 0 && target.y < 0) return Direction.SOUTHEAST;
        if (target.x < 0 && target.y < 0) return Direction.SOUTHWEST;
        if (target.x < 0 && target.y > 0) return Direction.NORTHWEST;
        if (target.y > 0) return Direction.NORTH;
        if (target.y < 0) return Direction.SOUTH;
        if (target.x > 0) return Direction.EAST;
        if (target.x < 0) return Direction.WEST;

        return Direction.CENTER;

    }

    public static int aerialDistance(MapLocation source, MapLocation destination){
        if(source == null || destination == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destination.x), Math.abs(source.y - destination.y));
    }

    public static int aerialDistance(MapLocation source, int destX, int destY){
        if(source == null) return  Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destX), Math.abs(source.y - destY));
    }
}
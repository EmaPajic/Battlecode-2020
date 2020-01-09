package Mark1;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {

    static RobotController rc;

    static Direction moveTowards(MapLocation target) {

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
}
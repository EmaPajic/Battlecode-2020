package Mark1.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import java.util.ArrayList;
import java.util.List;

import static Mark1.RobotPlayer.rc;

public class Navigation {

    public static Direction moveTowards(MapLocation target) {

        if (target == null) return Direction.CENTER;

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

    public static List<Direction> moveAwayFrom(MapLocation target) {

        List<Direction> ret = new ArrayList<>();

        if (target == null) return ret;

        target = target.translate(-rc.getLocation().x, -rc.getLocation().y);

        if (target.y > 0 && target.x > 0) {
            ret.add(Direction.SOUTHWEST);
            if (target.x > target.y) {
                ret.add(Direction.WEST);
                ret.add(Direction.NORTHWEST);
                ret.add(Direction.SOUTH);
                return ret;
            }
            if (target.x < target.y) {
                ret.add(Direction.SOUTH);
                ret.add(Direction.SOUTHEAST);
                ret.add(Direction.WEST);
                return ret;
            }

            ret.add(Direction.SOUTH);
            ret.add(Direction.WEST);
            ret.add(Direction.SOUTHEAST);
            ret.add(Direction.NORTHWEST);
            return ret;
        }

        if (target.y > 0 && target.x < 0) {
            ret.add(Direction.SOUTHEAST);
            if (-target.x > target.y) {
                ret.add(Direction.EAST);
                ret.add(Direction.NORTHEAST);
                ret.add(Direction.SOUTH);
                return ret;
            }
            if (-target.x < target.y) {
                ret.add(Direction.SOUTH);
                ret.add(Direction.SOUTHWEST);
                ret.add(Direction.EAST);
                return ret;
            }

            ret.add(Direction.SOUTH);
            ret.add(Direction.EAST);
            ret.add(Direction.SOUTHWEST);
            ret.add(Direction.NORTHEAST);

            return ret;
        }

        if (target.y < 0 && target.x < 0) {
            ret.add(Direction.NORTHEAST);
            if (target.x < target.y) {
                ret.add(Direction.EAST);
                ret.add(Direction.SOUTHEAST);
                ret.add(Direction.NORTH);
                return ret;
            }
            if (target.x > target.y) {
                ret.add(Direction.NORTH);
                ret.add(Direction.NORTHWEST);
                ret.add(Direction.EAST);
                return ret;
            }

            ret.add(Direction.NORTH);
            ret.add(Direction.EAST);
            ret.add(Direction.NORTHWEST);
            ret.add(Direction.SOUTHEAST);


            return ret;
        }

        if (target.y < 0 && target.x > 0) {
            ret.add(Direction.NORTHWEST);
            if (-target.x < target.y) {
                ret.add(Direction.WEST);
                ret.add(Direction.SOUTHWEST);
                ret.add(Direction.NORTH);
                return ret;
            }
            if (-target.x > target.y) {
                ret.add(Direction.NORTH);
                ret.add(Direction.NORTHEAST);
                ret.add(Direction.WEST);
                return ret;
            }

            ret.add(Direction.NORTH);
            ret.add(Direction.WEST);
            ret.add(Direction.NORTHEAST);
            ret.add(Direction.SOUTHWEST);

            return ret;
        }

        if (target.x == 0 && target.y > 0) {
            ret.add(Direction.SOUTHWEST);
            ret.add(Direction.SOUTHEAST);
            ret.add(Direction.SOUTH);
            ret.add(Direction.WEST);
            ret.add(Direction.EAST);
            return ret;
        }

        if (target.x == 0 && target.y < 0) {
            ret.add(Direction.NORTHEAST);
            ret.add(Direction.NORTHWEST);
            ret.add(Direction.NORTH);
            ret.add(Direction.EAST);
            ret.add(Direction.WEST);
            return ret;
        }

        if (target.x > 0) {
            ret.add(Direction.NORTHWEST);
            ret.add(Direction.SOUTHWEST);
            ret.add(Direction.WEST);
            ret.add(Direction.NORTH);
            ret.add(Direction.SOUTH);
            return ret;
        }

        if (target.x < 0) {
            ret.add(Direction.NORTHEAST);
            ret.add(Direction.SOUTHEAST);
            ret.add(Direction.EAST);
            ret.add(Direction.SOUTH);
            ret.add(Direction.NORTH);
            return ret;
        }

        ret.add(Direction.NORTHEAST);
        ret.add(Direction.SOUTHEAST);
        ret.add(Direction.SOUTHWEST);
        ret.add(Direction.NORTHWEST);
        ret.add(Direction.EAST);
        ret.add(Direction.SOUTH);
        ret.add(Direction.WEST);
        ret.add(Direction.NORTH);
        return ret;

    }

    public static Direction clockwiseSquare(MapLocation center) {

        if (center == null) return Direction.CENTER;

        center = center.translate(-rc.getLocation().x, -rc.getLocation().y);

        boolean verticalCorner = Math.abs(center.x) > Math.abs(center.y) || center.x == center.y;
        if (center.x > 0 && verticalCorner) return Direction.NORTH;
        if (center.x < 0 && verticalCorner) return Direction.SOUTH;

        boolean horizontalCorner = Math.abs(center.x) < Math.abs(center.y) || -center.x == center.y;
        if (center.y > 0 && horizontalCorner) return Direction.WEST;
        if (center.y < 0 && horizontalCorner) return Direction.EAST;

        return Direction.CENTER;
    }

    public static int aerialDistance(MapLocation source, MapLocation destination) {
        if (source == null || destination == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destination.x), Math.abs(source.y - destination.y));
    }

    public static int aerialDistance(MapLocation source, int destX, int destY) {
        if (source == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destX), Math.abs(source.y - destY));
    }

    public static int aerialDistance(RobotInfo target) {
        if (target == null) return Integer.MAX_VALUE;
        return aerialDistance(rc.getLocation(), target.location);
    }

    public static int aerialDistance(MapLocation destination) {
        return aerialDistance(rc.getLocation(), destination);
    }

    public static int aerialDistance(int destX, int destY) {
        return aerialDistance(rc.getLocation(), destX, destY);
    }
}
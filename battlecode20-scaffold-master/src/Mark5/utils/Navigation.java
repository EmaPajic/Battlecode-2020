package Mark5.utils;

import Mark5.utils.Strategium;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Navigation {

    private static MapLocation lastIntersection;
    private static boolean avoiding = false;
    private static Direction lastDirection;
    private static Direction lastAvoidingDirection = Direction.SOUTHEAST;
    private static MapLocation lastDestination = new MapLocation(100, 100);

    private static int typeMobility(RobotType type) {
        switch (type) {
            case REFINERY:
            case HQ:
            case FULFILLMENT_CENTER:
            case DESIGN_SCHOOL:
            case VAPORATOR:
            case NET_GUN:
                return -1;
            case LANDSCAPER:
                return 10;
            case MINER:
                return 20;
            case COW:
                return 30;
            case DELIVERY_DRONE:
                return 50;
        }
        return 0;
    }

    public static boolean diagonal(MapLocation a, MapLocation b) {
        if (a == null || b == null) return false;
        MapLocation distance = a.translate(-b.x, -b.y);
        return Math.abs(distance.x) == Math.abs(distance.y);
    }

    /**
     * the amount of obstacles encountered while bug pathing
     */
    public static int frustration = 0;

    /**
     * Checks if a location is suitable for offensive landing
     *
     * @param location The location to check. It needs to be within the map boundaries.
     * @return true if it is known to be a good landing spot, false otherwise.
     */
    public static boolean goodLandingSpot(MapLocation location) {
        if (location == null) return false;
        if (Strategium.enemyHQLocation == null) return false;
        if (Strategium.water[location.x][location.y]) return false;
        if (location.distanceSquaredTo(Strategium.enemyHQLocation) > RobotType.LANDSCAPER.sensorRadiusSquared)
            return false;
        if (location.isAdjacentTo(Strategium.enemyHQLocation)) return true;
        return Strategium.elevation[location.x][location.y] > 15;
    }

    public static boolean fuzzyNav(MapLocation destination) throws GameActionException {
        for (Direction dir : dir8)
            if (rc.adjacentLocation(dir).distanceSquaredTo(destination)
                    < rc.getLocation().distanceSquaredTo(destination))
                if (Strategium.canSafelyMove(dir)) {
                    lastDirection = dir;
                    rc.move(dir);
                    return true;
                }
        if (rc.getLocation().isAdjacentTo(destination)) {
            frustration = 0;
            for (Direction dir : dir8)
                if (rc.adjacentLocation(dir).isAdjacentTo(destination))
                    if (Strategium.canSafelyMove(dir)) {
                        lastDirection = dir;
                        rc.move(dir);
                        return true;
                    }
        }
        return false;
    }

    /**
     * Moves the unit one step towards the destination, circumnavigating any hazards and obstacles along the way.
     * The field "frustration" represents the amount of obstacles encountered along the way. It can and should be used
     * to determine if the destination is unreachable, the unit got stuck or it has spent a certain amount of time
     * circling the target.
     *
     * @param destination the location to go towards
     * @return true if move was made, false otherwise
     * @throws GameActionException it doesn't
     */
    public static boolean bugPath(MapLocation destination) throws GameActionException {
        if(!lastDestination.isAdjacentTo(destination)) {
            avoiding = false;
            frustration = 0;
        }

        lastDestination = destination;

        if(!avoiding || frustration > 30) {
            lastIntersection = rc.getLocation();
            if (fuzzyNav(destination)) {
                return true;
            }
        }

        avoiding = true;
        frustration++;

        if(rc.getLocation().distanceSquaredTo(destination) <= lastIntersection.distanceSquaredTo(destination)) {
            if (fuzzyNav(destination)) {
                avoiding = false;
                lastIntersection = rc.getLocation();
                return true;
            }
        }

        Direction dir = lastDirection.opposite().rotateLeft();
        int i = 0;
        for(; Strategium.canSafelyMove(dir); dir = dir.rotateLeft(), i++){
            if(i == 8) {
                avoiding = false;
                return (fuzzyNav(destination));
            }
        }

        for(; !Strategium.canSafelyMove(dir); dir = dir.rotateLeft(), i++){
            if(i == 8) {
                return false;
            }
        }
        rc.move(dir);
        lastDirection = dir;
        return true;

    }


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

    public static Direction moveToBuild(MapLocation target) {

        Direction dir = moveTowards(target);
        if (dir == Direction.NORTHEAST) dir = Direction.NORTH;
        if (dir == Direction.NORTHWEST) dir = Direction.WEST;
        if (dir == Direction.SOUTHEAST) dir = Direction.EAST;
        if (dir == Direction.SOUTHWEST) dir = Direction.SOUTH;

        return dir;
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

    /**
     * Calculates a direction in which the unit should move in order to follow a clockwise square path.
     *
     * @param center center of the square
     * @return direction to move in
     */
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

    /**
     * Computes the aerial distance (ie. the distance between them along either x or y axis, whichever is larger)
     * between the two points.
     *
     * @param source      one point
     * @param destination other point
     * @return distance between the points
     */
    public static int aerialDistance(MapLocation source, MapLocation destination) {
        if (source == null || destination == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destination.x), Math.abs(source.y - destination.y));
    }

    /**
     * Computes the aerial distance (ie. the distance between them along either x or y axis, whichever is larger)
     * between the two points.
     *
     * @param source one point
     * @param destX  other point x coordinate
     * @param destY  other point y coordinate
     * @return distance between the points
     */
    public static int aerialDistance(MapLocation source, int destX, int destY) {
        if (source == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(source.x - destX), Math.abs(source.y - destY));
    }

    /**
     * Computes the aerial distance (ie. the distance between them along either x or y axis, whichever is larger)
     * to the target robot
     *
     * @param target target robot
     * @return distance to the target robot
     */
    public static int aerialDistance(RobotInfo target) {
        if (target == null) return Integer.MAX_VALUE;
        return aerialDistance(rc.getLocation(), target.location);
    }

    /**
     * Computes the aerial distance (ie. the distance between them along either x or y axis, whichever is larger)
     * to the location
     *
     * @param destination point to compute the distance to
     * @return distance to the target location
     */
    public static int aerialDistance(MapLocation destination) {
        return aerialDistance(rc.getLocation(), destination);
    }

    /**
     * Computes the aerial distance (ie. the distance between them along either x or y axis, whichever is larger)
     * to the location
     *
     * @param destX x coordinate of the point to compute the distance to
     * @param destY y coordinate of the point to compute the distance to
     * @return distance to the target location
     */
    public static int aerialDistance(int destX, int destY) {
        return aerialDistance(rc.getLocation(), destX, destY);
    }

    /**
     * Clamps the location coordinates to be within the map boundaries
     *
     * @param location the location to clamp
     * @return the location with clamped coordinates
     */
    public static MapLocation clamp(MapLocation location) {
        int x = location.x;
        int y = location.y;
        if (x >= rc.getMapWidth()) x = rc.getMapWidth() - 1;
        if (y >= rc.getMapHeight()) y = rc.getMapHeight() - 1;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        return new MapLocation(x, y);
    }

    public static int index(Direction dir){
        switch (dir){
            case NORTH:
                return 0;
            case NORTHEAST:
                return 1;
            case EAST:
                return 2;
            case SOUTHEAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTHWEST:
                return 5;
            case WEST:
                return 6;
            case NORTHWEST:
                return 7;
            case CENTER:
                return 9;
        }
        return -1;
    }

}
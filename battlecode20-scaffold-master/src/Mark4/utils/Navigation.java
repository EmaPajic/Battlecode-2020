package Mark4.utils;

import battlecode.common.*;

import javax.print.attribute.standard.Destination;
import java.util.ArrayList;
import java.util.List;

import static Mark4.RobotPlayer.rc;

public class Navigation {

    private static MapLocation lastIntersection;
    private static boolean avoiding = false;
    private static Direction lastDirection;
    private static Direction lastAvoidingDirection = Direction.SOUTHEAST;
    private static MapLocation lastDestination;

    private static int typeMobility(RobotType type){
        switch (type) {
            case REFINERY:
            case HQ:
            case FULFILLMENT_CENTER:
            case DESIGN_SCHOOL:
            case VAPORATOR:
            case NET_GUN:
                return -1;
            case LANDSCAPER: return 10;
            case MINER: return 20;
            case COW: return 30;
            case DELIVERY_DRONE: return 50;
        }
        return 0;
    }

    public static boolean diagonal(MapLocation a, MapLocation b){
        if (a == null || b == null) return false;
        MapLocation distance = a.translate(-b.x, -b.y);
        return Math.abs(distance.x) == Math.abs(distance.y);
    }

    public static int frustration = 0;

    public static boolean goodLandingSpot(MapLocation location){
        if(location == null) return false;
        if(Strategium.enemyHQLocation == null) return false;
        if(Strategium.water[location.x][location.y]) return false;
        if(location.distanceSquaredTo(Strategium.enemyHQLocation) > RobotType.LANDSCAPER.sensorRadiusSquared)
            return false;
        if(location.isAdjacentTo(Strategium.enemyHQLocation)) return true;
        if(Strategium.elevation[location.x][location.y] > 15) return true;
        return false;
    }

    private static boolean isOnLine(MapLocation destination) {
        MapLocation line = destination.translate(-lastIntersection.x, -lastIntersection.y);
        MapLocation point = rc.getLocation().translate(-lastIntersection.x, -lastIntersection.y);
        if(line.equals(point)) return true;
        if(line.x * point.x < 0  || line.y * point.y < 0) return false;
        if(line.x == 0) return point.x == 0;
        if(line.y == 0) return point.y == 0;
        return Math.abs(point.x) == Math.abs(point.y);
    }

    public static boolean bugPath(MapLocation destination) throws GameActionException {
        Direction dir = moveTowards(destination);
        if(dir != moveTowards(lastDestination))
            avoiding = false;

        lastDestination = destination;

        System.out.println(dir + " " + lastDirection + " " + frustration);

        if(!avoiding) {
            lastIntersection = rc.getLocation();
            lastDirection = dir;
            if(Strategium.canSafelyMove(dir)){
                rc.move(dir);
                return true;
            }
            MapLocation obstacle = rc.adjacentLocation(dir);
            if(rc.canSenseLocation(obstacle)){
                RobotInfo robot = rc.senseRobotAtLocation(obstacle);
                if(robot != null) {
                    if(robot.team == Strategium.myTeam && robot.ID > rc.getID() && !robot.type.isBuilding() &&
                            frustration < typeMobility(robot.type)) {
                        frustration += 10;
                        return false;
                    }
                }
            }

        }

        System.out.println(aerialDistance(destination) + " " + aerialDistance(lastIntersection, destination) + " " + lastIntersection);

        if(avoiding && dir == lastDirection &&
                aerialDistance(destination) <= aerialDistance(lastIntersection, destination)) {
            if(Strategium.canSafelyMove(dir)){
                avoiding = false;
                lastIntersection = rc.getLocation();
                rc.move(dir);
                return true;
            }
        }

        frustration++;
        dir = lastAvoidingDirection;

        int i = 0;
        for(i = 0; Strategium.canSafelyMove(dir); dir = dir.rotateRight(), i++) {
            if (i == 8) {
                avoiding = false;
                lastAvoidingDirection = dir;
                rc.move(dir);
                return true;
            }
        }
        if(i > 0) {
            rc.move(dir.rotateLeft());
            lastAvoidingDirection = dir.rotateLeft();
            return true;
        }
        for(; !Strategium.canSafelyMove(dir); dir = dir.rotateLeft(), i++){
            if (i == 8){
                return false;
            }
        }
        avoiding = true;
        lastAvoidingDirection = dir;
        rc.move(dir);
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

    public static MapLocation clamp(MapLocation location){
        int x = location.x;
        int y = location.y;
        if(x >= rc.getMapWidth()) x = rc.getMapWidth() - 1;
        if(y >= rc.getMapHeight()) y = rc.getMapHeight() - 1;
        if(x < 0) x = 0;
        if(y < 0) y = 0;
        return  new MapLocation(x, y);
    }
}
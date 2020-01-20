package Mark5.utils;

import battlecode.common.*;

import java.util.LinkedList;
import java.util.List;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.HQLocation;
import static Mark5.utils.Strategium.elevation;

public class Wall {
    public static MapLocation[] wall;

    public static int minHeight = Integer.MIN_VALUE;
    public static int maxHeight = Integer.MIN_VALUE;

    public static MapLocation lowestPoint = null;

    /**
     * Required before using the class. Initializes the wall position. Needs to know the HQ location in order to work.
     */
    public static void init() throws GameActionException {
        int size = 0;
        for (Direction dir : dir8) if (rc.onTheMap(HQLocation.add(dir)) && !Lattice.isPit(HQLocation.add(dir))) size++;
        wall = new MapLocation[size];
        for (Direction dir : dir8)
            if (rc.onTheMap(HQLocation.add(dir)) && !Lattice.isPit(HQLocation.add(dir))) {
                size--;
                wall[size] = HQLocation.add(dir);
            }
    }


    /**
     * Scans all visible parts of the wall measuring their height
     *
     * @throws GameActionException it doesn't
     */
    public static void scanWall() throws GameActionException {

        minHeight = Integer.MAX_VALUE;

        for (int i = wall.length; i-- > 0; ) {
            if (rc.canSenseLocation(wall[i])) {
                if (rc.senseElevation(wall[i]) < minHeight) minHeight = rc.senseElevation(wall[i]);
            }
        }

    }

    public static boolean reposition() throws GameActionException {
        switch (HQLocation.directionTo(rc.getLocation())) {
            case SOUTHEAST:
            case NORTHEAST:
            case SOUTHWEST:
            case NORTHWEST:
                for (int i = wall.length; i-- > 0; )
                    if (wall[i].x == HQLocation.x || wall[i].y == HQLocation.y)
                        if (rc.getLocation().isAdjacentTo(wall[i]))
                            if (rc.canMove(rc.getLocation().directionTo(wall[i]))) {
                                rc.move(rc.getLocation().directionTo(wall[i]));
                                return true;
                            }
                return false;
            case NORTH:
            case EAST:
            case SOUTH:
            case WEST:
                Direction dir = rc.getLocation().directionTo(HQLocation);
                MapLocation location = rc.getLocation().add(dir).add(dir);
                if(Strategium.occupied[location.x][location.y]) return false;
                for (int i = wall.length; i-- > 0; )
                    if (wall[i].x == HQLocation.x || wall[i].y == HQLocation.y)
                        if (rc.getLocation().isAdjacentTo(wall[i])) if (Strategium.occupied[wall[i].x][wall[i].y])
                            for (int j = wall.length; j-- > 0; )
                                if (wall[j].x == HQLocation.x || wall[j].y == HQLocation.y)
                                    if (rc.getLocation().isAdjacentTo(wall[j]))
                                        if (rc.canMove(rc.getLocation().directionTo(wall[j]))) {
                                            rc.move(rc.getLocation().directionTo(wall[j]));
                                            return true;
                                        }
                return false;
        }
        return false;

    }

    public static MapLocation freeSpot() throws GameActionException {
        if (rc.getRoundNum() < 300) return null;
        for (int i = wall.length; i-- > 0; ) {
            if (rc.canSenseLocation(wall[i]))
                if (!rc.isLocationOccupied(wall[i]) &&
                        rc.senseElevation(wall[i]) < rc.senseElevation(rc.getLocation()) + 4) return wall[i];
        }
        return null;
    }

    public static MapLocation buildSpot() throws GameActionException {
        MapLocation buildSpot = rc.getLocation();
        for (int i = wall.length; i-- > 0; ) {
            if (rc.getLocation().isAdjacentTo(wall[i]))
                if (rc.canSenseLocation(wall[i]))
                    if (Strategium.robotAt(wall[i]) == RobotType.LANDSCAPER || rc.getRoundNum() > 600)
                        if (rc.senseElevation(wall[i]) < rc.senseElevation(buildSpot))
                            buildSpot = wall[i];
        }
        return buildSpot;
    }

    /**
     * Checks if depositing dirt in the provided direction would make the wall impassable
     *
     * @param dir the direction to deposit dirt in
     * @return false if depositing dirt would certainly upset the wall, true otherwise
     * @throws GameActionException might throw an exception in very high pollution
     */
    public static boolean shouldBuild(Direction dir) throws GameActionException {
        return rc.senseElevation(rc.adjacentLocation(dir)) < minHeight + 3;
    }


}

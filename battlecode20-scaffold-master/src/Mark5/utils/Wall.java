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
    public static void init() {
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
                    if (rc.isLocationOccupied(wall[i]) || rc.getRoundNum() > 600)
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

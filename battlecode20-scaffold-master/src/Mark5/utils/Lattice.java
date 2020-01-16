package Mark5.utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Lattice {

    /**
     * Checks if the map location is a pit (it's elevation doesn't matter)
     * @param location the location to check for
     * @return true if it is a pit, false otherwise
     */
    public static boolean isPit(MapLocation location) {
        return location.x % 2 == 0 && location.y % 2 == 0;
    }

    /**
     * Checks if the map location is a path (it should be kept above water and traversable)
     * @param location the location to check for
     * @return true if it is a path, false otherwise
     */
    public static boolean isPath(MapLocation location) {
        return (location.x + location.y) % 2 == 1;
    }

    /**
     * Checks if the map location is a building site (A building is/will be there.
     * It should be kept as high as possible while being reachable from the path)
     * @param location the the location to check for
     * @return true if it is a building site, false otherwise
     */
    public static boolean isBuildingSite(MapLocation location) {
        return location.x % 2 == 1 && location.y % 2 == 1;
    }

    /**
     * Checks if the lattice is even at the target location
     * @param location the location to check for
     * @return true if it is even, false otherwise
     */
    public static boolean isEven(MapLocation location) {
        int elevation = Strategium.elevation[location.x][location.y];
        for (Direction dir : dir8) {
            MapLocation loc = location.add(dir);
            if(rc.onTheMap(loc) && !(isPit(location)) && !loc.isAdjacentTo(Strategium.HQLocation) &&
                    !loc.equals(Strategium.HQLocation))
                if(Math.abs(Strategium.elevation[loc.x][loc.y] - elevation) > 3) return false;
        }
        return true;
    }

    /**
     * Calculates how much dirt can be deposited at the location while keeping it even
     * @param location the location to calculate for
     * @return the amount of dirt
     */
    public static int maxDeposit(MapLocation location) {
        int elevation = Strategium.elevation[location.x][location.y];
        int maxDeposit = 6;
        for (Direction dir : dir8) {
            MapLocation loc = location.add(dir);
            if(rc.onTheMap(loc) && !(isPit(location)) && !loc.isAdjacentTo(Strategium.HQLocation) &&
                    !loc.equals(Strategium.HQLocation))
                if(Strategium.elevation[loc.x][loc.y] + 3 - elevation < maxDeposit)
                maxDeposit = Strategium.elevation[loc.x][loc.y] + 3 - elevation;
        }
        return maxDeposit;
    }

}

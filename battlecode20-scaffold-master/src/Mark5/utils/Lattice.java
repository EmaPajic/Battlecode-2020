package Mark5.utils;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Lattice {

    /**
     * Checks if the map location is a pit (it's elevation doesn't matter)
     *
     * @param location the location to check for
     * @return true if it is a pit, false otherwise
     */
    public static boolean isPit(MapLocation location) {
        return location.x % 2 == 0 && location.y % 2 == 0 || Strategium.elevation[location.x][location.y] < -100
                && !location.equals(Strategium.HQLocation);
    }

    /**
     * Checks if the map location is a path (it should be kept above water and traversable)
     *
     * @param location the location to check for
     * @return true if it is a path, false otherwise
     */
    public static boolean isPath(MapLocation location) {

        return ((location.x + location.y) % 2 == 1 /*|| location.isAdjacentTo(Strategium.HQLocation)*/) &&
                !location.equals(Strategium.HQLocation);
    }

    /**
     * Checks if the map location is a building site (A building is/will be there.
     * It should be kept as high as possible while being reachable from the path)
     *
     * @param location the the location to check for
     * @return true if it is a building site, false otherwise
     */
    public static boolean isBuildingSite(MapLocation location) {
        return location.x % 2 == 1 && location.y % 2 == 1 && !location.isAdjacentTo(Strategium.HQLocation);
    }

    /**
     * Checks if the lattice is even and safe at the target location
     *
     * @param location the location to check for
     * @param waterLevel the minimum safe elevation
     * @return true if it is even and safe, false otherwise
     */
    public static boolean isEven(MapLocation location, int waterLevel) {
        int elevation = Strategium.elevation[location.x][location.y];
        for (Direction dir : dir8) {
            MapLocation loc = location.add(dir);
            if (rc.onTheMap(loc) && isPath(loc) && !loc.isAdjacentTo(Strategium.HQLocation) &&
                    !loc.equals(Strategium.HQLocation))
                if (Math.abs(Strategium.elevation[loc.x][loc.y] - elevation) > 3 &&
                        !isAdjacentToWater(loc) || Strategium.elevation[loc.x][loc.y] < waterLevel) {
                    return false;
                }
        }
        return true;
    }


    /**
     * Calculates how much dirt can be deposited at the location while keeping it even
     *
     * @param location the location to calculate for
     * @return the amount of dirt
     */
    public static int maxDeposit(MapLocation location) {
        if (location.equals(Strategium.HQLocation)) return 0;
        if (isPit(location)) return Integer.MAX_VALUE;
        if (location.isAdjacentTo(Strategium.HQLocation)) return Integer.MAX_VALUE;
        int elevation = Strategium.elevation[location.x][location.y];
        int maxDeposit = 6;
        for (Direction dir : dir8) {
            MapLocation loc = location.add(dir);
            if (rc.onTheMap(loc) && !isPit(loc) && !loc.isAdjacentTo(Strategium.HQLocation) &&
                    !loc.equals(Strategium.HQLocation))
                if (Strategium.elevation[loc.x][loc.y] + 3 - elevation < maxDeposit && !Strategium.water[loc.x][loc.y])
                    maxDeposit = Strategium.elevation[loc.x][loc.y] + 3 - elevation;
        }
        return maxDeposit;
    }

    /**
     * Returns the best direction to take dirt from
     *
     * @return the direction. If there is no suitable direction, returns null.
     */
    public static Direction bestDigDirection() {
        for (Direction dir : Direction.allDirections()) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (isAdjacentToWater(location)) continue;
            if (isPit(location)) return dir;
        }
        for (Direction dir : Direction.allDirections()) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (isPit(location)) return dir;
        }
        return null;
    }

    /**
     * Checks if a location is adjacent to water
     *
     * @param location the location to check for
     * @return true if it is adjacent to water, false otherwise
     */
    public static boolean isAdjacentToWater(MapLocation location) {
        if(Strategium.water[location.x][location.y]) return false;
        if(Strategium.nearestWater == null) return false;
        for (Direction dir : dir8) {
            MapLocation loc = location.add(dir);
            if (rc.onTheMap(loc))
                if (Strategium.water[loc.x][loc.y]) return true;
        }
        return false;
    }

    /**
     * Calculates the best direction to deposit dirt
     *
     * @return the direction
     */
    public static Direction bestDepositDirection() {
        int minElevation = Integer.MAX_VALUE;
        Direction bestDir = null;
        if (Strategium.nearestWater != null)
            if (rc.getLocation().isAdjacentTo(Strategium.nearestWater))
                for (Direction dir : Direction.allDirections()) {
                    MapLocation loc = rc.adjacentLocation(dir);
                    if (!rc.onTheMap(loc)) continue;
                    if (isAdjacentToWater(loc) && !Strategium.water[loc.x][loc.y])
                        if (minElevation > Strategium.elevation[loc.x][loc.y]) {
                            bestDir = dir;
                            minElevation = Strategium.elevation[loc.x][loc.y];
                        }
                }
        if (bestDir != null) return bestDir;

        for (Direction dir : Direction.allDirections()) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) continue;
            if (!Strategium.occupied[loc.x][loc.y])
                if (isBuildingSite(loc) && maxDeposit(loc) > 0) {
                    return dir;
                }
        }

        for (Direction dir : Direction.allDirections()) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) continue;
            if (isPath(loc) && maxDeposit(loc) > 0) {
                return dir;
            }
        }

        for (Direction dir : Direction.allDirections()) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) continue;
            if (isPit(loc)) return dir;
        }
        return Direction.CENTER;

    }

}

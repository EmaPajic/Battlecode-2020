package Mark4.utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static Mark4.RobotPlayer.rc;

public class Wall {
    private static final int[] wallX = {0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1};
    private static final int[] wallY = {2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1, 0, 1, 2, 2};
    private static final int launchPadX = -1;
    private static final int launchPadY = -1;
    private static int[] wallHeight;

    public static MapLocation[] wall = new MapLocation[16];
    public static MapLocation launchPad;

    public static int minHeight = Integer.MIN_VALUE;
    public static int maxHeight = Integer.MIN_VALUE;

    public static MapLocation lowestPoint = null;


    public static void init() {
        wallHeight = new int[16];
        for (int i = 16; i-- > 0; ) {
            wall[i] = Strategium.HQLocation.translate(wallX[i], wallY[i]);
            wallHeight[i] = -10;
        }
        launchPad = Strategium.HQLocation.translate(launchPadX, launchPadY);

    }

    public static boolean onWallAndBlocking(RobotInfo[] victims, MapLocation location) {
        for (int i = 16; i-- > 0; )
            if (wall[i].equals(location)) {
                for (RobotInfo robot : victims)
                    if (wall[(15 + i) % 16].equals(robot.location) && !robot.type.isBuilding())
                        return true;
                return false;
            }
        return false;
    }

    public static boolean isLaunchPadBlocked() {
        int threshold = Strategium.elevation[launchPad.x][launchPad.y] + 3;
        return //Strategium.elevation[launchPad.x - 1][launchPad.y - 1] > threshold ||
                Strategium.elevation[launchPad.x - 1][launchPad.y] > threshold; //||
                //Strategium.elevation[launchPad.x][launchPad.y - 2] > threshold;
    }

    public static boolean isOnWall(Direction direction) {
        return Navigation.aerialDistance(rc.adjacentLocation(direction), Strategium.HQLocation) == 2;
    }

    public static boolean stuckOnWall(MapLocation location) {
        return (isLaunchPadBlocked() && launchPad.equals(location)) ||
                (Navigation.aerialDistance(location, Strategium.HQLocation) == 2 &&
                        Strategium.elevation[location.x][location.y] > 15) ||
                (Navigation.aerialDistance(location, Strategium.HQLocation) == 3 &&
                        Strategium.elevation[location.x][location.y] < 0);
    }

    public static MapLocation clockwise(MapLocation location) {
        for (int i = 16; i-- > 0; ) if (location.equals(wall[i])) return wall[(i + 1) % 16];
        return null;
    }

    public static void scanWall() throws GameActionException {

        minHeight = Integer.MAX_VALUE;

        for (int i = 16; i-- > 0; ){
            if (rc.canSenseLocation(wall[i])) {
                wallHeight[i] = rc.senseElevation(wall[i]);
                if(wallHeight[i] < maxHeight){
                    maxHeight = wallHeight[i];
                }
                if(wallHeight[i] < minHeight){
                    minHeight = wallHeight[i];
                    lowestPoint = wall[i];
                }
            }
        }

    }

    public static boolean shouldBuild(Direction dir) throws GameActionException {
        return rc.senseElevation(rc.adjacentLocation(dir)) < minHeight + 3;
    }


}

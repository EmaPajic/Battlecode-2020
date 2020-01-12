package Mark2.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static Mark2.RobotPlayer.rc;

public class Wall {
    private static final int[] wallX = {0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1};
    private static final int[] wallY = {2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1, 0, 1, 2, 2};
    private static final int launchPadX = -1;
    private static final int launchPadY = -1;

    public static MapLocation[] wall = new MapLocation[16];
    public static MapLocation launchPad;

    public static void init(){
        for(int i = 15; i >= 0; i--) wall[i] = Strategium.HQLocation.translate(wallX[i], wallY[i]);
        launchPad = Strategium.HQLocation.translate(launchPadX, launchPadY);
    }

    public static boolean onWallAndBlocking(RobotInfo[] victims, MapLocation location){
        for(int i = 15; i >= 0; i--) if (wall[i].equals(location)) {
            for(RobotInfo robot : victims) if(wall[(15 + i) % 16].equals(robot.location) && !robot.type.isBuilding())
                return true;
            return false;
        }
        return false;
    }

    public static boolean isLaunchPadBlocked(){
        int threshold = Strategium.elevation[launchPad.x][launchPad.y] + 3;
        return Strategium.elevation[launchPad.x -1][launchPad.y - 1] > threshold;
    }

    public static boolean isOnWall(Direction direction){
        return Navigation.aerialDistance(rc.adjacentLocation(direction), Strategium.HQLocation) == 2;
    }


}

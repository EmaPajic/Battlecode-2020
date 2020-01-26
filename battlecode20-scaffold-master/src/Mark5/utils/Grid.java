package Mark5.utils;

import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.rc;

public class Grid {
    public static boolean[] interesting;
    public static boolean[] huntingGround;
    public static boolean[] unsafe;
    public static boolean[] taken;
    public static boolean[] flooded;
    public static int rows;
    public static int cols;
    public static int size;

    public static void init(){
        rows = rc.getMapHeight() / 7;
        cols = rc.getMapWidth() / 7;
        interesting = new boolean[rows * cols];
        unsafe = new boolean[rows * cols];
        taken = new boolean[rows * cols];
        huntingGround = new boolean[rows * cols];
    }

    public static int index(MapLocation location){
        return location.x / 7 + location.y / 7 * cols;
    }

    public static boolean interesting(MapLocation location){
        return interesting[location.x / 7 + location.y / 7 * cols];
    }

    public static boolean unsafe(MapLocation location){
        return unsafe[location.x / 7 + location.y / 7 * cols];
    }

    public static MapLocation findHuntingGround(){
        MapLocation nearestHuntingGround = null;
        for (int i = size; i-- > 0;)
            if(huntingGround[i]){
                if(Navigation.aerialDistance(nearestHuntingGround) >
                        Navigation.aerialDistance(i % cols * 7 + 3, i / cols * 7 + 3))
                            nearestHuntingGround = new MapLocation(i % cols * 7 + 3, i / cols * 7 + 3);
            }
        return nearestHuntingGround;
    }
}



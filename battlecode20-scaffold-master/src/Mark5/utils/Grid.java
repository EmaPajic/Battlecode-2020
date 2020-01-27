package Mark5.utils;

import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.*;
import static Mark5.utils.Strategium.potentialEnemyHQLocations;

public class Grid {
    public static boolean[] interesting;
    public static boolean[] huntingGround;
    public static boolean[] unsafe;
    public static boolean[] taken;
    public static boolean[] flooded;
    public static boolean[] checked;
    public static int rows;
    public static int cols;
    public static int size;
    public static int currentHuntingGround = -1;
    public static int idleTime;
    public static MapLocation waypoint;
    public static int destination;
    public static boolean circumnavigating;

    private class Waypoint{
        public final int tile;
        public final Waypoint prev;

        public Waypoint(int tile, Waypoint prev) {
            this.tile = tile;
            this.prev = prev;
        }

        public int up(){
            if(tile / cols == rows - 1) return -1;
            return tile + cols;
        }

        public int down(){
            if(tile / cols == 0) return -1;
            return tile - cols;
        }

        public int right(){
            if(tile % cols == cols - 1) return -1;
            return tile + 1;
        }

        public int left(){
            if(tile % cols == 0) return -1;
            return tile - 1;
        }
    }

    public static void init(){
        rows = rc.getMapHeight() / 7 + (rc.getMapHeight() % 7 > 0 ? 1 : 0);
        cols = rc.getMapWidth() / 7 + (rc.getMapWidth() % 7 > 0 ? 1 : 0);
        size = rows * cols;
        interesting = new boolean[size];
        unsafe = new boolean[size];
        taken = new boolean[size];
        huntingGround = new boolean[size];
        flooded = new boolean[size];
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

    public static void update(){
        if(Navigation.aerialDistance(waypoint) <= 3){
            idleTime++;
            if(currentHuntingGround<0) idleTime += 10;
            else if(!huntingGround[currentHuntingGround]) idleTime += 10;
        }
        if(idleTime >= 10 || Navigation.frustration >= 30 || waypoint == null){
            idleTime = 0;
            huntingGround[index(rc.getLocation())] = false;
            findHuntingGround();
        }
    }

    static MapLocation center(int tile){
        return Navigation.clamp(new MapLocation(tile % cols * 7 + 3, tile / cols * 7 + 3));
    }

    public static void findHuntingGround(){
        System.out.println("FINDING...");
        waypoint = null;
        for (int i = size; i-- > 0;)
            if(huntingGround[i] && i != currentHuntingGround){
                if(Navigation.aerialDistance(waypoint) >
                        Navigation.aerialDistance(i % cols * 7 + 3, i / cols * 7 + 3)) {
                    waypoint = new MapLocation(i % cols * 7 + 3, i / cols * 7 + 3);
                    currentHuntingGround = i;
                }
            }
        if(waypoint != null) return;

        if(enemyHQLocation != null){
            waypoint = enemyHQLocation;
            return;
        }

        if (!potentialEnemyHQLocations.isEmpty()) {
            waypoint = potentialEnemyHQLocations.get(
                    rand.nextInt(potentialEnemyHQLocations.size()));
            System.out.println("POTENTIAL: " + waypoint);
            return;
        }

        currentHuntingGround = rand.nextInt(size);
        waypoint = Navigation.clamp(
                new MapLocation(currentHuntingGround % cols * 7 + 3, currentHuntingGround / cols * 7 + 3));

    }

    public static void gridNav(MapLocation destination){
        checked = new boolean[size];

        destination = center(index(destination));
    }
}



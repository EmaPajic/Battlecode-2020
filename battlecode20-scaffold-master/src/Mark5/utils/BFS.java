package Mark5.utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class BFS {

    public static List<LocationNode> queue;
    public static boolean[][] visitedLocations = null;

    public static void init() {
        queue = new LinkedList<>();

    }


    public void addVertex(MapLocation location) {
        location = location.translate(-rc.getLocation().x + 6, -rc.getLocation().y + 6);
        visitedLocations[location.x][location.y] = true;
    }

    private static boolean visited(MapLocation location){
        location = location.translate(-rc.getLocation().x + 6, -rc.getLocation().y + 6);
        return visitedLocations[location.x][location.y];
    }

    /**
     * Finds the shortest route to a point within the field of view
     * @param target the target to find route to
     * @return the direction
     */
    public Direction bfs(MapLocation target) {
        //

        queue.clear();
        queue.add(new LocationNode(rc.getLocation(), null));

        /*switch (rc.getType()) {
            case MINER:
                visitedLocations = new boolean[11][11];
                break;
            case LANDSCAPER:
            case DELIVERY_DRONE:
                visitedLocations = new boolean[9][9];
                break;
        }*/
        visitedLocations = new boolean[11][11];

        LocationNode currHead;

        while (!queue.isEmpty()) {
            currHead = queue.remove(0);
            for (Direction dir : dir8) {
                MapLocation step = currHead.loc.add(dir);
                MapLocation curr = currHead.loc;
                int x = step.x;
                int y = step.y;
                if (!visited(step)) if (rc.canSenseLocation(step)) {
                    switch (rc.getType()) {
                        case MINER:
                        case LANDSCAPER:
                            if (Math.abs(Strategium.elevation[x][y] - Strategium.elevation[curr.x][curr.y])
                                    <= 3 && !Strategium.occupied[x][y] && !Strategium.water[x][y]) {
                                if (step.equals(target)) {

                                    while(currHead.prev != null) currHead = currHead.prev;
                                    return rc.getLocation().directionTo(currHead.loc);

                                }
                                queue.add(new LocationNode(step, currHead));
                                addVertex(step);
                            }
                            break;
                        case DELIVERY_DRONE:
                            if(!Strategium.occupied[x][y]) {
                                if (step.equals(target)) {

                                    while(currHead.prev != null) currHead = currHead.prev;
                                    return rc.getLocation().directionTo(currHead.loc);

                                }
                                queue.add(new LocationNode(step, currHead));
                                addVertex(step);
                            }
                    }
                }

            }
        }

        return Direction.CENTER;

    }


    static class LocationNode {
        public final MapLocation loc;
        public final LocationNode prev;

        public LocationNode(MapLocation loc, LocationNode prev) {
            this.loc = loc;
            this.prev = prev;
        }


    }

}

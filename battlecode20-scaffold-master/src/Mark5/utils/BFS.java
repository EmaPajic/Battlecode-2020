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

    public static List<LocationNode> queue = new LinkedList<>();
    public static List<LocationNode> stack = new LinkedList<>();
    public static MapLocation bfsTarget;
    public static boolean[][] visitedLocations = null;

    public static void addVertex(MapLocation location) {
        location = location.translate(-rc.getLocation().x + 5, -rc.getLocation().y + 5);
        visitedLocations[location.x][location.y] = true;
    }

    private static boolean visited(MapLocation location){
        location = location.translate(-rc.getLocation().x + 5, -rc.getLocation().y + 5);
        return visitedLocations[location.x][location.y];
    }

    public static Direction step(MapLocation target) throws GameActionException {
        LocationNode node;
        for(LocationNode n : stack) rc.setIndicatorLine(n.prev.loc, n.loc, 255, 0, 0);
        if(!stack.isEmpty() && target == bfsTarget){
            node = stack.remove(0);
            if(rc.getLocation().equals(node.prev.loc)) {
                return rc.getLocation().directionTo(node.loc);
            }
        }
        bfsTarget = target;
        if(bfs(target)){
            for(LocationNode n : stack) rc.setIndicatorLine(n.prev.loc, n.loc, 255, 0, 0);
            return rc.getLocation().directionTo(stack.remove(0).loc);
        }

        return Direction.CENTER;
    }

    /**
     * Finds the shortest route to a point within the field of view
     * @param target the target to find route to
     * @return the direction
     */
    public static boolean bfs(MapLocation target) throws GameActionException {
        //

        queue.clear();
        stack.clear();
        queue.add(new LocationNode(rc.getLocation(), null));

        visitedLocations = new boolean[11][11];

        LocationNode currHead;

        while (!queue.isEmpty()) {
            currHead = queue.remove(0);
            for (Direction dir : dir8) {
                MapLocation step = currHead.loc.add(dir);
                if (rc.canSenseLocation(step)) if(!visited(step)) {
                    switch (rc.getType()) {
                        case MINER:
                        case LANDSCAPER:
                            if (Math.abs(rc.senseElevation(step) - rc.senseElevation(rc.getLocation()))
                                    <= 3 && !Strategium.occupied[step.x][step.y] && !rc.senseFlooding(step)) {
                                if (Navigation.aerialDistance(target) > Navigation.aerialDistance(step, target)) {

                                    stack.add(new LocationNode(step, currHead));
                                    while(currHead.prev != null){
                                        stack.add(0, currHead);
                                        currHead = currHead.prev;
                                    }
                                    return true;

                                }
                                queue.add(new LocationNode(step, currHead));
                                addVertex(step);
                            }
                            break;
                        case DELIVERY_DRONE:
                            if(!Strategium.occupied[step.x][step.y]) {
                                if (Navigation.aerialDistance(target) > Navigation.aerialDistance(step, target)) {

                                    stack.add(new LocationNode(step, currHead));
                                    while(currHead.prev != null){
                                        stack.add(0, currHead);
                                        currHead = currHead.prev;
                                    }
                                    return true;

                                }
                                queue.add(new LocationNode(step, currHead));
                                addVertex(step);
                            }
                    }
                }

            }
        }

        return false;

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

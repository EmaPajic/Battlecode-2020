package Mark1.utils;

import Mark1.utils.Navigation;
import Mark1.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import java.util.ArrayList;

import static Mark1.RobotPlayer.*;
import static Mark1.RobotPlayer.tryMove;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class TwoMinerController {
    enum SearchStrategy {
        GOES_AROUND, GOES_TO_MIDDLE;
    }

    enum Activity {
        ROAMING, MINING;
    }
    static ArrayList<MapLocation> searchRoute;
    static ArrayList<MapLocation> searchAroundRoute;
    static ArrayList<MapLocation> searchMiddleRoute;
    static Activity currentActivity;
    static int currentTargetIndex;
    static MapLocation currentTarget;
    static SearchStrategy mySearchStrategy;
    static MapLocation MAP_CENTER;
    static MapLocation NORTHWEST;
    static MapLocation NORTHEAST;
    static MapLocation SOUTHWEST;
    static MapLocation SOUTHEAST;

    static void chooseSearchRoute() {
        MAP_CENTER = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        NORTHWEST = new MapLocation(5, rc.getMapHeight() - 5);
        SOUTHWEST = new MapLocation(5, 5);
        SOUTHEAST = new MapLocation(rc.getMapWidth() - 5, 5);
        NORTHEAST = new MapLocation(rc.getMapWidth() - 5, rc.getMapHeight() -5);
        MapLocation[] cornerPoints = {NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST};

        // Populate search middle route
        searchMiddleRoute = new ArrayList<>();
        searchMiddleRoute.add(MAP_CENTER);

        // Populate search around route
        searchAroundRoute = new ArrayList<>();
        MapLocation firstLocation = new MapLocation(1000, 1000);
        for(MapLocation location : cornerPoints) {
            if(location.distanceSquaredTo(hqLocation) < firstLocation.distanceSquaredTo(hqLocation)) {
                firstLocation = location;
            }
        }
        searchAroundRoute.add(firstLocation);
        for(int count = 1; count <= 3; ++count) {
            MapLocation nextBestLocation = new MapLocation(1000, 1000);
            for(MapLocation location: cornerPoints) {
                if (!searchAroundRoute.contains(location)) {
                    if (location.distanceSquaredTo(searchAroundRoute.get(searchAroundRoute.size() - 1)) <
                            nextBestLocation.distanceSquaredTo(searchAroundRoute.get(searchAroundRoute.size() - 1))) {
                        nextBestLocation = location;
                    }
                }
            }
            searchAroundRoute.add(nextBestLocation);
        }

        searchRoute = new ArrayList<>();
        if(mySearchStrategy == SearchStrategy.GOES_TO_MIDDLE) {
            searchRoute.addAll(searchMiddleRoute);
            searchRoute.addAll(searchAroundRoute);
        } else {
            searchRoute.addAll(searchAroundRoute);
            searchRoute.addAll(searchMiddleRoute);
        }
    }

    public static void init() {
        if(rc.getRoundNum() % 2 == 0) {
            mySearchStrategy = SearchStrategy.GOES_AROUND;
        }
        else
            mySearchStrategy = SearchStrategy.GOES_TO_MIDDLE;
        chooseSearchRoute();
        currentActivity = Activity.ROAMING;
        currentTargetIndex = 0;
        currentTarget = searchRoute.get(currentTargetIndex);
    }

    public static void control() throws GameActionException {
        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {

            for (Direction dir : dir8)
                if (rc.canMineSoup(dir)) {
                    if(Navigation.aerialDistance(rc.getLocation(), Strategium.nearestRefinery) > 8) {
                        int xMin = rc.getLocation().x - 3;
                        int yMin = rc.getLocation().y - 3;
                        int xMax = rc.getLocation().x + 3;
                        int yMax = rc.getLocation().y + 3;
                        int totalSoup = 0;
                        for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++) {
                            for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                                MapLocation location = new MapLocation(i, j);
                                if (rc.canSenseLocation(location))
                                    totalSoup += rc.senseSoup(location);
                            }
                        }
                        if(totalSoup > 200) {
                            for(Direction dir2 : dir8) {
                                if(tryBuild(RobotType.REFINERY, dir2))
                                    return;
                            }
                        }
                    }
                    if(tryMine(dir))
                        return;
                }
            if (Strategium.nearestSoup != null)
                if (Navigation.bugPath(Strategium.nearestSoup)) return;
            Navigation.bugPath(currentTarget);
            if (rc.getLocation().distanceSquaredTo(currentTarget) < 5) {
                currentTargetIndex = (currentTargetIndex + 1) % searchRoute.size();
                currentTarget = searchRoute.get(currentTargetIndex);
            }

        } else {

            for (Direction dir : dir8)
                if (tryRefine(dir)) {
                    return;
                }
            Navigation.bugPath(Strategium.nearestRefinery);

        }
    }
}

package Mark3.utils;

import Mark3.utils.Navigation;
import Mark3.utils.Strategium;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static Mark3.RobotPlayer.*;
import static Mark3.RobotPlayer.tryMove;
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
    static int[] adjecencyCount = {0, 0, 0, 0, 0, 0, 0, 0};
    static int[] adjecencyID = {-1, -1, -1, -1, -1, -1, -1, -1};
    static int designSchoolsBuilt = 0;

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
        // Avoid collisions
        for(int i = 0; i < 8; ++i) {
            Direction dir = dir8[i];
            if(rc.canSenseLocation(rc.getLocation().add(dir))) {
                RobotInfo info = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                if(info != null) {
                    if(info.getID() == adjecencyID[i]) {
                        ++adjecencyCount[i];
                    }
                    else {
                        adjecencyCount[i] = 1;
                        adjecencyID[i] = info.getID();
                    }
                }
                else {
                    adjecencyCount[i] = 0;
                    adjecencyID[i] = -1;
                }
            }
        }
        for(int i = 0; i < 8; ++i) {
            if(adjecencyCount[i] > 50) {
                for(Direction awayDir : dir8) {
                    if(tryMove(awayDir)) {
                        adjecencyCount[i] = 0;
                        return;
                    }
                }
            }
        }


        // Build DesignCenter near enemy
        for(MapLocation enemyLocation : Strategium.potentialEnemyHQLocations) {
            System.out.println("Pot" + Strategium.potentialEnemyHQLocations.toString());
            if(rc.canSenseLocation(enemyLocation)) {
                RobotInfo info = rc.senseRobotAtLocation(enemyLocation);
                if(info != null)
                    if(info.getTeam() == Strategium.opponentTeam && info.getType() == RobotType.HQ) {
                        if(Navigation.aerialDistance(enemyLocation) > 3) {
                            Navigation.bugPath(enemyLocation);
                        }
                        else if(designSchoolsBuilt == 0) {
                            int xMin = enemyLocation.x - 3;
                            int yMin = enemyLocation.y - 3;
                            int xMax = enemyLocation.x + 3;
                            int yMax = enemyLocation.y + 3;
                            int designSchoolExists = 0;
                            for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++) {
                                for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {
                                    MapLocation location = new MapLocation(i, j);
                                    if (rc.canSenseLocation(location)) {
                                        RobotInfo Designinfo = rc.senseRobotAtLocation(location);
                                        if(Designinfo != null)
                                            if(Designinfo.getType() == RobotType.DESIGN_SCHOOL &&
                                                    Designinfo.getTeam() == Strategium.myTeam) {
                                                designSchoolExists = 1;
                                                break;
                                        }
                                    }
                                }
                            }
                            if(designSchoolExists == 0)
                            for (Direction dir : dir8) {
                                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) &&
                                        Navigation.aerialDistance(rc.getLocation().add(dir),enemyLocation) <= 2)  {
                                    if(tryBuild(RobotType.DESIGN_SCHOOL, dir))
                                        designSchoolsBuilt = 1;
                                }
                            }
                        }
                    }
            }
        }
        if(rc.canSenseLocation(hqLocation)) {
            if(rc.canSenseLocation(vaporatorLocation2)) {
                RobotInfo maybeVaporator = rc.senseRobotAtLocation(vaporatorLocation2);
                if(maybeVaporator != null) {
                    if(maybeVaporator.getType() == RobotType.VAPORATOR) {
                        Strategium.vaporatorBuilt = true;
                        Strategium.nearestRefinery = null;
                    }
                }
            }
        }
        if(Strategium.vaporatorBuilt && Navigation.aerialDistance(hqLocation) < 4) {
            Navigation.bugPath(currentTarget);
            if (rc.getLocation().distanceSquaredTo(currentTarget) < 5) {
                currentTargetIndex = (currentTargetIndex + 1) % searchRoute.size();
                currentTarget = searchRoute.get(currentTargetIndex);
            }
        }
        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {

            for (Direction dir : dir8)
                if (rc.canMineSoup(dir)) {
                    if(Navigation.aerialDistance(Strategium.nearestRefinery) > 7 &&
                            Navigation.aerialDistance(hqLocation) > 4) {
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

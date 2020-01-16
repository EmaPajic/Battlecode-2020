package Mark5.utils;

import battlecode.common.*;

import java.util.LinkedList;
import java.util.List;

import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.HQLocation;
import static Mark5.utils.Strategium.elevation;

public class Wall {
    private static final int[] wallX = {0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1};
    private static final int[] wallY = {2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1, 0, 1, 2, 2};
    private static final int[] buildingAreaX = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    private static final int[] buildingAreaY = {1, 1, 0, -1, -1, -1, 0, 1, 0};

    private static final int launchPadX = -1;
    private static final int launchPadY = -1;
    private static int[] wallHeight;
    private static int[] buildingAreaHeight;
    private static boolean[] occupied;

    public static MapLocation[] wall;
    public static MapLocation[] buildingArea;

    public static MapLocation launchPad;

    public static int minHeight = Integer.MIN_VALUE;
    public static int maxHeight = Integer.MIN_VALUE;

    public static MapLocation lowestPoint = null;
    public static MapLocation mainDesignSchoolLocation = null;
    public static MapLocation mainFulfillmentCenterLocation = null;
    public static List<MapLocation> mainVaporatorLocations = null;
    public static List<MapLocation> mainNetGunLocations = null;

    public static boolean baseCompleted = false;

    public static MapLocation nextBuildLocation = null;
    public static RobotType nextBuilding = RobotType.DESIGN_SCHOOL;

    /**
     * Required before using the class. Initializes the wall position. Needs to know the HQ location in order to work.
     */
    public static void init() {
        wall = new MapLocation[16];
        buildingArea = new MapLocation[9];
        mainVaporatorLocations = new LinkedList<>();
        mainNetGunLocations = new LinkedList<>();

        wallHeight = new int[16];
        buildingAreaHeight = new int[9];
        occupied = new boolean[9];

        for (int i = 16; i-- > 0; ) {
            wall[i] = HQLocation.translate(wallX[i], wallY[i]);
            wallHeight[i] = -10;
        }

        for (int i = 9; i-- > 0; ) {
            buildingArea[i] = HQLocation.translate(buildingAreaX[i], buildingAreaY[i]);
            buildingAreaHeight[i] = 3;
        }
        //launchPad = HQLocation.translate(launchPadX, launchPadY);

        boolean changed = false;

        do {

            changed = false;

            for (int i = 9; i-- > 0; ) {
                while (buildingArea[i].x < 0) {
                    changed = true;
                    for (int j = 16; j-- > 0; ) {
                        wall[j] = wall[j].add(Direction.EAST);
                    }
                    for (int j = 9; j-- > 0; ) {
                        buildingArea[j] = buildingArea[j].add(Direction.EAST);
                    }
                }

                while (buildingArea[i].x >= rc.getMapWidth()) {
                    changed = true;
                    for (int j = 16; j-- > 0; ) {
                        wall[j] = wall[j].add(Direction.WEST);
                    }
                    for (int j = 9; j-- > 0; ) {
                        buildingArea[j] = buildingArea[j].add(Direction.WEST);
                    }
                }

                while (buildingArea[i].y < 0) {
                    changed = true;
                    for (int j = 16; j-- > 0; ) {
                        wall[j] = wall[j].add(Direction.NORTH);
                    }
                    for (int j = 9; j-- > 0; ) {
                        buildingArea[j] = buildingArea[j].add(Direction.NORTH);
                    }
                }

                while (buildingArea[i].y >= rc.getMapHeight()) {
                    changed = true;
                    for (int j = 16; j-- > 0; ) {
                        wall[j] = wall[j].add(Direction.SOUTH);
                    }
                    for (int j = 9; j-- > 0; ) {
                        buildingArea[j] = buildingArea[j].add(Direction.SOUTH);
                    }
                }

            }

        } while (changed);


    }

    /**
     * Checks if a potential obstacle would block clockwise movement along the wall of any of the robots
     *
     * @param victims  robots to check for
     * @param location location of the potential obstacle
     * @return true if the obstacle does block movement, false otherwise
     */
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

    /**
     * Checks if the robot would end up on the wall by moving in given direction
     *
     * @param direction direction to check for
     * @return true if the adjacent location belongs to the wall, false otherwise
     */
    public static boolean isOnWall(Direction direction) {
        MapLocation location = rc.adjacentLocation(direction);
        for (int i = 16; i-- > 0; ) {
            if (wall[i].equals(location)) return true;
        }
        return false;
    }

    public static boolean stuckOnWall(MapLocation location) {
        return (isLaunchPadBlocked() && launchPad.equals(location)) ||
                (Navigation.aerialDistance(location, HQLocation) == 2 &&
                        Strategium.elevation[location.x][location.y] > 15) ||
                (Navigation.aerialDistance(location, HQLocation) == 3 &&
                        Strategium.elevation[location.x][location.y] < 0);
    }

    /**
     * Returns point on thr wall clockwise to the one given
     *
     * @param location the point on the wall
     * @return point immediately clockwise to the one provided, if it is on the wall. null otherwise
     */
    public static MapLocation clockwise(MapLocation location) {
        for (int i = 16; i-- > 0; ) if (location.equals(wall[i])) return wall[(i + 1) % 16];
        return null;
    }

    /**
     * Scans all visible parts of the wall measuring their height
     *
     * @throws GameActionException it doesn't
     */
    public static void scanWall() throws GameActionException {

        minHeight = Integer.MAX_VALUE;

        for (int i = 16; i-- > 0; ) {
            if (rc.canSenseLocation(wall[i])) {
                wallHeight[i] = rc.senseElevation(wall[i]);
                if (wallHeight[i] < maxHeight) {
                    maxHeight = wallHeight[i];
                }
                if (wallHeight[i] < minHeight) {
                    minHeight = wallHeight[i];
                    lowestPoint = wall[i];
                }
            }
        }

    }

    /**
     * Checks if depositing dirt in the provided direction would make the wall impassable
     *
     * @param dir the direction to deposit dirt in
     * @return false if depositing dirt would certainly upset the wall, true otherwise
     * @throws GameActionException might throw an exception in very high pollution
     */
    public static boolean shouldBuild(Direction dir) throws GameActionException {
        return rc.senseElevation(rc.adjacentLocation(dir)) < minHeight + 3;
    }

    /**
     * Checks if the point is within the HQ area (the wall, the trench and the building area)
     *
     * @param location the location to check for
     * @return true if the point is within the area, false otherwise
     */
    public static boolean isWithinHQArea(MapLocation location) {
        return Navigation.aerialDistance(location, buildingArea[8]) <= 3;
    }

    /**
     * Checks if the point is within the HQ building area
     *
     * @param location the location to check for
     * @return true if the point is within the area, false otherwise
     */
    public static boolean isWithinBuildingArea(MapLocation location) {
        return Navigation.aerialDistance(location, buildingArea[8]) <= 1;
    }

    private static int index(MapLocation location){
        for (int i = 9; i-- > 0;) if(wall[i].equals(location)) return i;
        return -1;
    }

    /**
     * Scans all visible locations within the HQ building area and updates the relevant data structures accordingly
     *
     * @throws GameActionException it doesn't
     */
    public static void checkBaseStatus() throws GameActionException {

        for (int i = 9; i-- > 0; )
            if (rc.canSenseLocation(buildingArea[i])) {

                MapLocation location = buildingArea[i];
                RobotInfo building = rc.senseRobotAtLocation(location);
                boolean isGoodBuilding = building != null;

                buildingAreaHeight[i] = rc.senseElevation(location);

                if (isGoodBuilding) isGoodBuilding = building.team == Strategium.myTeam && building.type.isBuilding();

                if (isGoodBuilding) {
                    occupied[i] = true;
                    switch (building.type) {
                        case DESIGN_SCHOOL:
                            mainDesignSchoolLocation = location;
                            break;
                        case FULFILLMENT_CENTER:
                            mainFulfillmentCenterLocation = location;
                            break;
                        case NET_GUN:
                            if (!mainNetGunLocations.contains(location)) mainNetGunLocations.add(location);
                            break;
                        case VAPORATOR:
                            if (!mainVaporatorLocations.contains(location)) mainVaporatorLocations.add(location);
                            break;
                    }
                } else {
                    occupied[i] = false;
                    if (location.equals(mainDesignSchoolLocation)) mainDesignSchoolLocation = null;
                    if (location.equals(mainFulfillmentCenterLocation)) mainFulfillmentCenterLocation = null;
                    mainVaporatorLocations.remove(location);
                    mainNetGunLocations.remove(location);
                }
            }

        baseCompleted = mainDesignSchoolLocation != null &&
                mainFulfillmentCenterLocation != null &&
                mainVaporatorLocations.size() == 2 &&
                mainNetGunLocations.size() == 2;

    }


}

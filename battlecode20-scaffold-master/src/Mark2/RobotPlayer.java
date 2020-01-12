package Mark2;

import Mark2.robots.Drone;
import Mark2.utils.Blockchain;
import Mark2.utils.Strategium;
import Mark2.utils.TwoMinerController;
import battlecode.common.*;
import Mark2.utils.Navigation;
import java.util.ArrayList;

public strictfp class RobotPlayer {
    public static RobotController rc;
    static int turnCount = 0;
    public static MapLocation hqLocation = null;
    public static MapLocation designSchoolLocation;
    static MapLocation fulfillmentCenterLocation;
    static MapLocation netGunLocation1;
    static MapLocation netGunLocation2;
    static MapLocation netGunLocation3;
    public static MapLocation vaporatorLocation1;
    public static MapLocation vaporatorLocation2;
    static int buildstage = 0; // tells miners what to build

    static int myFun = 0;
    static int numMiners = 0;
    static int numLandscapers = 0;
    static int numDrones = 0;
    static int landscaperTurns = 0;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST};

    public static Direction[] dir8 = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.NORTHWEST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST
    };

    static RobotType[] spawnedByMiner = {
            RobotType.REFINERY,
            RobotType.VAPORATOR,
            RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER,
            RobotType.NET_GUN
    };


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        Strategium.init();

        if (rc.getType() == RobotType.MINER) {
            if (rc.getRobotCount() == 2) {
                myFun = 1; // main search miner
            }
            else if (rc.getRobotCount() == 3) {
                myFun = 2; // 2nd search miner
            }
            else {
                myFun = 3; // build miner
            }
        }
        else if (rc.getType() == RobotType.LANDSCAPER) {
            myFun = 1; // protect yourself
        }
        else if (rc.getType() == RobotType.DELIVERY_DRONE) {
            myFun = 1;
        }
        if (hqLocation == null) {
            // search surroundings for hq
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLocation = robot.location;
                    designSchoolLocation = new MapLocation(hqLocation.x - 1, hqLocation.y);
                    fulfillmentCenterLocation = new MapLocation(hqLocation.x, hqLocation.y - 1);
                    vaporatorLocation1 = new MapLocation(hqLocation.x, hqLocation.y + 1);
                    vaporatorLocation2 = new MapLocation(hqLocation.x + 1, hqLocation.y);
                    netGunLocation1 = new MapLocation(hqLocation.x - 1, hqLocation.y + 1);
                    netGunLocation2 = new MapLocation(hqLocation.x + 1, hqLocation.y + 1);
                    netGunLocation3 = new MapLocation(hqLocation.x + 1, hqLocation.y - 1);
                    System.out.println("Found HQ!");
                }
            }
            if (rc.getType() == RobotType.MINER) {
                TwoMinerController.init();
            }
        }
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (numMiners == 2) {
            if (tryBuild(RobotType.MINER, Direction.WEST)) {
                ++numMiners;
            }
        }
        else if (numMiners < 2) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) {
                    ++numMiners;
                }
        }
        else {
            runNetGun();
        }
    }
    static boolean builtFulfillmentCenter = false;

    static void runMiner() throws GameActionException {
        if (myFun < 3)
            runSearchMiner();
        else
            runBuildMiner();
    }

    static void runSearchMiner() throws GameActionException {
        Strategium.gatherInfo();
        TwoMinerController.control();
    }

    static void runBuildMiner() throws GameActionException {
        switch(buildstage) {
            case -1:
                if (tryMove(Direction.WEST)) {
                    ++buildstage;
                }
                break;
            case 0: build(RobotType.DESIGN_SCHOOL, designSchoolLocation); break;
            case 1: build(RobotType.VAPORATOR, vaporatorLocation1); break;
            case 2: build(RobotType.VAPORATOR, vaporatorLocation2); break;
            case 3: build(RobotType.FULFILLMENT_CENTER, fulfillmentCenterLocation); break;
            case 4: build(RobotType.NET_GUN, netGunLocation1); break;
            case 5: build(RobotType.NET_GUN, netGunLocation2); break;
            case 6: build(RobotType.NET_GUN, netGunLocation3); break;
            case 7: runSearchMiner(); break;
        }
    }

    static void build(RobotType type, MapLocation loc) throws GameActionException{

        MapLocation goToLoc = null;
        Direction buildDir = null;
        switch (buildstage % 4) {
            case 0:
                goToLoc = new MapLocation(loc.x - 1, loc.y);
                buildDir = Direction.EAST;
                break;
            case 1:
                goToLoc = new MapLocation(loc.x, loc.y + 1);
                buildDir = Direction.SOUTH;
                break;
            case 2:
                goToLoc = new MapLocation(loc.x + 1, loc.y);
                buildDir = Direction.WEST;
                break;
            case 3:
                goToLoc = new MapLocation(loc.x, loc.y - 1);
                buildDir = Direction.NORTH;
                break;
        }

        Direction goToDir = Navigation.moveToBuild(goToLoc);
        if (goToDir == Direction.CENTER) {
            // add build permission
            if (tryBuild(type, buildDir) ||
                    rc.senseElevation(rc.getLocation().add(buildDir)) + 4 < rc.senseElevation(rc.getLocation())) {
                ++buildstage;
            }
        }
        else {
            if (!tryMove(goToDir)) {
                goToDir = Navigation.moveTowards(goToLoc);
                tryMove(goToDir);
            }
        }
    }
    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        if (numLandscapers == 1 && !(rc.getRoundNum() > 500 || rc.getRobotCount() > 8)) {
            return;
        }
        else {
            if (tryBuild(RobotType.LANDSCAPER, Direction.SOUTH)) {
                ++numLandscapers;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        if(rc.getRoundNum() % 100 == 0)
            for (Direction dir : directions)
                tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLateLandscaper() throws GameActionException {
        if (rc.getDirtCarrying() >= 1) {
            Direction depositDirtDir = getOptimalDepositDir();
            if (rc.canDepositDirt(depositDirtDir)) {
                rc.depositDirt(depositDirtDir);
            }
        }
        ArrayList<Direction> digDirs = new ArrayList<>();
        if (rc.getLocation().x == hqLocation.x - 2) {
            digDirs.add(Direction.WEST);
            digDirs.add(Direction.NORTHWEST);
            digDirs.add(Direction.SOUTHWEST);
        } else if (rc.getLocation().x == hqLocation.x + 2) {
            digDirs.add(Direction.EAST);
            digDirs.add(Direction.NORTHEAST);
            digDirs.add(Direction.SOUTHEAST);
        } else if (rc.getLocation().y == hqLocation.y - 2) {
            digDirs.add(Direction.SOUTH);
            digDirs.add(Direction.SOUTHEAST);
            digDirs.add(Direction.SOUTHWEST);
        } else {
            digDirs.add(Direction.NORTH);
            digDirs.add(Direction.NORTHEAST);
            digDirs.add(Direction.NORTHWEST);
        }
        for (Direction digDir : digDirs) {
            if (rc.canDigDirt(digDir)) {
                rc.digDirt(digDir);
                ++landscaperTurns;
                return;
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if (landscaperTurns == 0 && (rc.getLocation().x == hqLocation.x - 1)
                && (rc.getLocation().y == hqLocation.y - 1)) {
            tryMove(Direction.WEST);
        }
        else if ((landscaperTurns % 3 == 0) && rc.getDirtCarrying() < 1) {
            ArrayList<Direction> digDirs = new ArrayList<>();
            if (rc.getLocation().x == hqLocation.x - 2) {
                digDirs.add(Direction.WEST);
                digDirs.add(Direction.NORTHWEST);
                digDirs.add(Direction.SOUTHWEST);
            } else if (rc.getLocation().x == hqLocation.x + 2) {
                digDirs.add(Direction.EAST);
                digDirs.add(Direction.NORTHEAST);
                digDirs.add(Direction.SOUTHEAST);
            } else if (rc.getLocation().y == hqLocation.y - 2) {
                digDirs.add(Direction.SOUTH);
                digDirs.add(Direction.SOUTHEAST);
                digDirs.add(Direction.SOUTHWEST);
            } else {
                digDirs.add(Direction.NORTH);
                digDirs.add(Direction.NORTHEAST);
                digDirs.add(Direction.NORTHWEST);
            }
            for (Direction digDir : digDirs) {
                if (rc.canDigDirt(digDir)) {
                    rc.digDirt(digDir);
                    ++landscaperTurns;
                    return;
                }
            }
        }
        else if (landscaperTurns % 3 == 1 || (landscaperTurns % 3 == 0 && rc.getDirtCarrying() >= 1)) {
            if (landscaperTurns % 3 == 0) landscaperTurns = 1;
            Direction depositDirtDir = getOptimalDepositDir();
            if (rc.canDepositDirt(depositDirtDir)) {
                rc.depositDirt(depositDirtDir);
                ++landscaperTurns;
            }
        }
        else {
            MapLocation goToLoc = null;
            if (rc.getLocation().x == hqLocation.x - 2 && rc.getLocation().y == hqLocation.y + 2) {
                goToLoc = rc.getLocation().add(Direction.EAST);
            }
            else if (rc.getLocation().x == hqLocation.x - 2) {
                goToLoc = rc.getLocation().add(Direction.NORTH);
            }
            else if (rc.getLocation().y == hqLocation.y + 2 && rc.getLocation().x == hqLocation.x + 2) {
                goToLoc = rc.getLocation().add(Direction.SOUTH);
            }
            else if (rc.getLocation().y == hqLocation.y + 2) {
                goToLoc = rc.getLocation().add(Direction.EAST);
            }
            else if (rc.getLocation().x == hqLocation.x + 2 && rc.getLocation().y == hqLocation.y - 2) {
                goToLoc = rc.getLocation().add(Direction.WEST);
            }
            else if (rc.getLocation().x == hqLocation.x + 2) {
                goToLoc = rc.getLocation().add(Direction.SOUTH);
            }
            else if (rc.getLocation().y == hqLocation.y - 2 && rc.getLocation().x == hqLocation.x - 2) {
                goToLoc = rc.getLocation().add(Direction.NORTH);
            }
            else if (rc.getLocation().y == hqLocation.y - 2) {
                goToLoc = rc.getLocation().add(Direction.WEST);
            }

            Direction goToDir = Navigation.moveToBuild(goToLoc);
            if (tryMove(goToDir)) {
                ++landscaperTurns;
            }
            else {
                RobotInfo robot = rc.senseRobotAtLocation(goToLoc);
                if (robot == null) {
                    if (goToDir == Direction.EAST)
                        goToDir = Direction.SOUTHEAST;
                    if (goToDir == Direction.SOUTH)
                        goToDir = Direction.SOUTHWEST;
                    if (goToDir == Direction.WEST)
                        goToDir = Direction.NORTHWEST;
                    if (goToDir == Direction.NORTH)
                        goToDir = Direction.NORTHEAST;

                    if (tryMove(goToDir)) {
                        ++landscaperTurns;
                    }
                }
                else {
                    Direction digDir;
                    if (rc.getLocation().x == hqLocation.x - 2) {
                        digDir = Direction.WEST;
                    }
                    else if (rc.getLocation().x == hqLocation.x + 2) {
                        digDir = Direction.EAST;
                    }
                    else if (rc.getLocation().y == hqLocation.y - 2) {
                        digDir = Direction.SOUTH;
                    }
                    else {
                        digDir = Direction.NORTH;
                    }
                    if (rc.canDigDirt(digDir)) {
                        rc.digDirt(digDir);
                    }
                }
            }

        }
    }

    static Direction getOptimalDepositDir() throws GameActionException{
        Direction prevStepDir = null;
        Direction nextStepDir = null;
        Direction edgeDir = null;
        if (rc.getLocation().x == hqLocation.x - 2 && rc.getLocation().y == hqLocation.y + 2) {
            nextStepDir = Direction.EAST;
            prevStepDir = Direction.SOUTH;
        }
        else if (rc.getLocation().x == hqLocation.x - 2 && rc.getLocation().y == hqLocation.y - 2) {
            nextStepDir = Direction.NORTH;
            prevStepDir = Direction.EAST;
        }
        else if (rc.getLocation().x == hqLocation.x - 2) {
            if (rc.getLocation().y == hqLocation.y + 1) {
                edgeDir = Direction.NORTHEAST;
            }
            if (rc.getLocation().y == hqLocation.y - 1) {
                edgeDir = Direction.SOUTHEAST;
            }
            nextStepDir = Direction.NORTH;
            prevStepDir = Direction.SOUTH;
        }
        else if (rc.getLocation().y == hqLocation.y + 2 && rc.getLocation().x == hqLocation.x + 2) {
            nextStepDir = Direction.SOUTH;
            prevStepDir = Direction.WEST;
        }
        else if (rc.getLocation().y == hqLocation.y + 2) {
            if (rc.getLocation().x == hqLocation.x + 1) {
                edgeDir = Direction.SOUTHEAST;
            }
            if (rc.getLocation().x == hqLocation.x - 1) {
                edgeDir = Direction.SOUTHWEST;
            }
            nextStepDir = Direction.EAST;
            prevStepDir = Direction.WEST;
        }
        else if (rc.getLocation().x == hqLocation.x + 2 && rc.getLocation().y == hqLocation.y - 2) {
            nextStepDir = Direction.WEST;
            prevStepDir = Direction.NORTH;
        }
        else if (rc.getLocation().x == hqLocation.x + 2) {
            if (rc.getLocation().y == hqLocation.y + 1) {
                edgeDir = Direction.NORTHWEST;
            }
            if (rc.getLocation().y == hqLocation.y - 1) {
                edgeDir = Direction.SOUTHWEST;
            }
            nextStepDir = Direction.SOUTH;
            prevStepDir = Direction.NORTH;
        }
        else if (rc.getLocation().y == hqLocation.y - 2) {
            if (rc.getLocation().x == hqLocation.x + 1) {
                edgeDir = Direction.NORTHEAST;
            }
            if (rc.getLocation().x == hqLocation.x - 1) {
                edgeDir = Direction.NORTHWEST;
            }
            nextStepDir = Direction.WEST;
            prevStepDir = Direction.EAST;
        }

        MapLocation prevLoc = rc.getLocation().add(prevStepDir);
        MapLocation nextLoc = rc.getLocation().add(nextStepDir);
        MapLocation edgeLoc = null;
        int elevationPrev = rc.senseElevation(prevLoc);
        int elevationNext = rc.senseElevation(nextLoc);
        int elevationCurr = rc.senseElevation(rc.getLocation());
        int elevationEgde = -1;
        if (edgeDir != null) {
            edgeLoc = rc.getLocation().add(edgeDir);
            elevationEgde = rc.senseElevation(edgeLoc);
        }
        System.out.println("Prev: " + elevationPrev + " " + prevStepDir.toString() + " " + prevLoc.toString());
        System.out.println("Curr: " + elevationCurr + " " + Direction.CENTER.toString() + rc.getLocation());
        System.out.println("Next: " + elevationNext + " " + nextStepDir.toString() + " " + nextLoc.toString());
        if (edgeDir != null)
            System.out.println("Edge: " + elevationEgde + " " + edgeDir.toString() + " " + edgeLoc.toString());
        if (elevationPrev < elevationNext && elevationPrev < elevationCurr) {
            if (edgeDir != null) {
                if (elevationEgde < elevationPrev)
                    return edgeDir;
            }
            return prevStepDir;
        }

        if (elevationNext < elevationPrev && elevationNext < elevationCurr) {
            if (edgeDir != null) {
                if (elevationEgde < elevationNext)
                    return edgeDir;
            }
            return nextStepDir;
        }
        if (edgeDir != null) {
            if (elevationEgde < elevationCurr)
                return edgeDir;
        }
        return  Direction.CENTER;
    }

    static void runDeliveryDrone() throws GameActionException {
        Drone.run();
    }

    static void runNetGun() throws GameActionException {
        RobotInfo[] targets = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, Strategium.opponentTeam);
        RobotInfo bestTarget = null;
        int bestTargetRange = 100;
        for (RobotInfo target : targets)
            if (rc.canShootUnit(target.ID))
                if (target.location.distanceSquaredTo(rc.getLocation()) < bestTargetRange) {
                    bestTarget = target;
                    bestTargetRange = target.location.distanceSquaredTo(rc.getLocation());
                }

        if (bestTarget != null) rc.shootUnit(bestTarget.ID);
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return dir8[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    public static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir  The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }


}

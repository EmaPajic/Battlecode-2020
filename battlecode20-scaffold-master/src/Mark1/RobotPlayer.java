package Mark1;

import Mark1.robots.Drone;
import Mark1.utils.Blockchain;
import Mark1.utils.Navigation;
import Mark1.utils.Strategium;
import Mark1.utils.TwoMinerController;
import battlecode.common.*;


public strictfp class RobotPlayer {
    public static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
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
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    public static MapLocation hqLocation;
    public static MapLocation designSchoolLocation;
    public static MapLocation fulfillmentCenterLocation;
    public static MapLocation netGunLocation1;
    public static MapLocation netGunLocation2;
    public static MapLocation netGunLocation3;
    public static MapLocation vaporatorLocation1;
    public static MapLocation vaporatorLocation2;
    public static int myFun = 0;

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
            if (2 == 2) {
                myFun = 1; // main search miner
            }
            else if (3 == 3) {
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

        turnCount = 0;

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        //noinspection InfiniteLoopStatement
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        Drone.run();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
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
        if (turnCount == 1) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) return;
        }
        if (turnCount == 2) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) return;
        }
        runNetGun();
    }

    static boolean builtFulfillmentCenter = false;

    static void runMiner() throws GameActionException {
        Strategium.gatherInfo();
        /*
        if (rc.getTeamSoup() >= 300 && !builtFulfillmentCenter) {
            for (Direction dir : dir8)
                if (rc.adjacentLocation(dir).isAdjacentTo(Strategium.HQLocation))
                    if (tryBuild(RobotType.FULFILLMENT_CENTER, dir)) {
                        builtFulfillmentCenter = true;
                        return;
                    }
        }
        */
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
            TwoMinerController.init();
        }

        if (myFun < 3)
            runSearchMiner();
        else
            runBuildMiner();
    }
    
    static void runSearchMiner() throws GameActionException {
        TwoMinerController.control();
    }

    static void runBuildMiner() throws GameActionException {

    }
    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
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

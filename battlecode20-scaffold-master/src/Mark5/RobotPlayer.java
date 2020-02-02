package Mark5;

import Mark5.robots.*;
import Mark5.utils.*;
import battlecode.common.*;


public strictfp class RobotPlayer {
    public static RobotController rc;
    public static int turnCount = 0;

    public static Miner.MinerType minerType = null;
    public static int numMiners = 0;
    public static int numLandscapers = 0;

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


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // option to have a rush miner, but we don't have it in the final version
        if (rc.getType() == RobotType.MINER) {
            if (rc.getRoundNum() == 2) {
                minerType = Miner.MinerType.RUSH;
            } else {
                minerType = Miner.MinerType.SEARCH;
            }
        }

        // since we don't have a rush miner, remove next line if you want rush
        minerType = Miner.MinerType.SEARCH;

        Strategium.init();
        if (rc.getType() == RobotType.MINER || rc.getType() == RobotType.DELIVERY_DRONE) {
            Miner.init();
        }

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            if(turnCount == 1 && rc.getType() != RobotType.HQ)
                Blockchain.init();
            else if(turnCount == 3 && rc.getType() == RobotType.HQ)
                Blockchain.init();

            try {
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:
                        HQ.run();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        Refinery.run();
                        break;
                    case VAPORATOR:
                        break;
                    case DESIGN_SCHOOL:
                        DesignSchool.run();
                        break;
                    case FULFILLMENT_CENTER:
                        FulfillmentCenter.run();
                        break;
                    case LANDSCAPER:
                        Landscaper.run();
                        break;
                    case DELIVERY_DRONE:
                        Drone.run();
                        break;
                    case NET_GUN:
                        NetGun.run();
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


    static void runMiner() throws GameActionException {
        //if we have a rush miner
        if (minerType == Miner.MinerType.RUSH) {
            RushMiner.run();
        } else {
            //run non-rush miners
            Miner.control();
        }
    }

    public static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMove(Direction dir) throws GameActionException {
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

}

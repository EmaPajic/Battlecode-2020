package Mark5.robots;

import Mark5.sensors.DroneSensor;
import Mark5.utils.*;
import battlecode.common.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Drone {

    public enum State {
        SENTRY,
        TAXI,
        PREDATOR,
        LANDSCAPER_GATHERER,
        SWARMER
    }

    public enum Payload {
        POTENTIAL,
        FRIENDLY_LANDSCAPER,
        RUSH_MINER,
        FRIENDLY_MINER,
        BIOLOGICAL,
        ENEMY
    }

    public static State state = State.PREDATOR;

    public static Payload payload = Payload.POTENTIAL;

    private static int patrolRange = 4;
    private static int numOfDefensiveDrones = 15;
    private static int patrolWaypointIndex = 1;
    private static MapLocation waypoint;
    private static boolean crunchComplete = false;

    private static Iterator<MapLocation> target = null;

    private static MapLocation getTarget(List<MapLocation> list) {
        if (target != null) if (target.hasNext()) return target.next();
        if (!list.isEmpty()) {
            target = list.iterator();
            return target.next();
        }
        return null;
    }
    public static void roam() {
        Grid.update();
        waypoint = Grid.waypoint;
    }
    public static boolean isCrunchingTime() {
        if(Strategium.enemyHQLocation == null)
            return false;

        /* first crunch - turn 1500
        first - empty drones that should kill enemy landscapers
        second - drones with miners so that enemy drones can be killed
        trird - drones with landscapers for attack
         */
        if(!rc.canSenseLocation(Strategium.enemyHQLocation))
            return false;
        if(rc.getRoundNum() > 1500 && rc.getRoundNum() <= 1550 && !rc.isCurrentlyHoldingUnit())
            return true;
        if(rc.getRoundNum() > 1515 && rc.getRoundNum() <= 1565 && rc.isCurrentlyHoldingUnit())
            if(payload == Payload.FRIENDLY_MINER)
                return true;
        if(rc.getRoundNum() > 1525 && rc.getRoundNum() <= 1575 && rc.isCurrentlyHoldingUnit())
            if(payload == Payload.FRIENDLY_LANDSCAPER)
                return true;

        // periodic crunch
        if(rc.getRoundNum() > 1500 && rc.getRoundNum() % 200 > 100 && rc.getRoundNum() % 200 <= 150 &&
           !rc.isCurrentlyHoldingUnit())
            return true;
        if(rc.getRoundNum() > 1500 && rc.getRoundNum() % 200 > 115 && rc.getRoundNum() % 200 <= 165 &&
                rc.isCurrentlyHoldingUnit())
            if(payload == Payload.FRIENDLY_MINER)
                return true;
        if(rc.getRoundNum() > 1500 && rc.getRoundNum() % 200 > 125 && rc.getRoundNum() % 200 <= 175 &&
                rc.isCurrentlyHoldingUnit())
            if(payload == Payload.FRIENDLY_LANDSCAPER)
                return true;
        return false;
    }

    public static boolean isPayloadGatheringTime() {
        if(rc.getRoundNum() > 1300 && rc.getRoundNum() < 1400)
            return true;
        return false;
    }

    public static boolean goodSwarmingLandingSpot(MapLocation location) throws GameActionException{
        if(location == null)
            return false;
        if(!rc.canSenseLocation(location))
            return false;
        if(Strategium.enemyHQLocation == null)
            return false;
        if(rc.senseFlooding(location))
            return false;
        if((rc.senseElevation(location) >= 100 && Navigation.aerialDistance(Strategium.enemyHQLocation, location) <= 2)
            || Navigation.aerialDistance(Strategium.enemyHQLocation, location) == 1)
            return true;
        return false;
    }

    public static void run() throws GameActionException {

        Strategium.gatherInfo();

        patrolRange = rc.getRoundNum() > 1000 ? 2 : 4;
        if(isCrunchingTime())
            state = State.SWARMER;
        else
            state = State.PREDATOR;

        if(state != State.SWARMER) {
            if(isPayloadGatheringTime()) {
                state = State.LANDSCAPER_GATHERER;
                waypoint = null;
            }
            else {
                state = State.PREDATOR;
            }
        }
        if(Strategium.enemyHQLocation != null) {
            if (rc.getLocation().isWithinDistanceSquared(Strategium.enemyHQLocation,
                    GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED))
                state = State.SWARMER;
        }

        numOfDefensiveDrones = Strategium.numDronesMet > 65 ? 15 : 6;
        if(Strategium.dronesMetWithLowerID < (rc.getRoundNum() < 1200 ? 1 : numOfDefensiveDrones)) state = State.SENTRY;

        //if(rc.getRoundNum() < 150) state = State.SENTRY;
        if (state == State.SWARMER && isCrunchingTime() && Navigation.aerialDistance(Strategium.enemyHQLocation) == 1 &&
            !rc.isCurrentlyHoldingUnit() && Strategium.nearestEnemyUnit == null) {
            rc.disintegrate();
        }

        //System.out.println(state);
        //System.out.println(payload);
        //System.out.println(state);

        if (!rc.isReady()) return;
        //System.out.println(payload);

        switch (payload) {
            case ENEMY:
            case BIOLOGICAL:
                drown();
                break;
            case POTENTIAL:
                //if(DroneSensor.potentialTaxiPayload != null) {
                //    if(attack(DroneSensor.potentialTaxiPayload))
                //        return;
                //}
                /*
                if (rc.adjacentLocation(Direction.NORTHEAST).equals(Strategium.HQLocation)) {
                    boolean canMove = false;
                    for (Direction dir : dir8) if(rc.canMove(dir)) canMove = true;
                    if (!canMove) {
                            RobotInfo blocker = rc.senseRobotAtLocation(rc.adjacentLocation(Direction.SOUTHWEST));
                            if (blocker != null) if (blocker.type == RobotType.LANDSCAPER) {
                                if(attack(blocker)) break;
                            }
                        }
                }
                 */
                if (Strategium.nearestEnemyUnit != null) if (attack(Strategium.nearestEnemyUnit)) break;
                if (Strategium.blockedUnit != null) if (attack(Strategium.blockedUnit)) break;
                if (state == State.PREDATOR && rc.getRoundNum() > 1400 &&
                    Navigation.aerialDistance(Strategium.enemyHQLocation) >= 6) {
                    if(Strategium.nearestMiner != null) {
                        if(Navigation.aerialDistance(Strategium.nearestMiner.location,
                                                     Strategium.HQLocation) >= 2) {
                            if(attack(Strategium.nearestMiner)) break;
                        }
                    }
                    if(Strategium.nearestLandscaper != null) {
                        if(Navigation.aerialDistance(Strategium.nearestLandscaper.location,
                                                     Strategium.HQLocation) >= 3) {
                            if(attack(Strategium.nearestLandscaper)) break;
                        }
                    }
                }
                patrol();
                break;
            case RUSH_MINER:
            case FRIENDLY_LANDSCAPER:
            case FRIENDLY_MINER:
                if(rc.getRoundNum() < 1300 || (state == State.SENTRY && payload == Payload.FRIENDLY_LANDSCAPER) ||
                        (state == State.SWARMER && (payload == Payload.FRIENDLY_LANDSCAPER ||
                                payload == Payload.FRIENDLY_MINER)))
                    climb();
                else
                    patrol();

                break;
        }


    }

    private static boolean attack(RobotInfo target) throws GameActionException {
        if (rc.canPickUpUnit(target.ID)) {
            rc.pickUpUnit(target.ID);
            if (target.team == Strategium.myTeam) {
                switch (target.type) {
                    case LANDSCAPER:
                        payload = Payload.FRIENDLY_LANDSCAPER;
                        return true;
                    case MINER:
                        DroneSensor.potentialTaxiPayload = null;
                        payload = Payload.FRIENDLY_MINER;
                        //state = State.TAXI;
                        return true;
                }
            }
            payload = target.team == Strategium.opponentTeam ? Payload.ENEMY : Payload.BIOLOGICAL;
            return true;
        }
        return Navigation.bugPath(target.location);
    }

    private static boolean drown() throws GameActionException {

        for (Direction dir : dir8) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj))
                if (Strategium.water[adj.x][adj.y]) if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    payload = Payload.POTENTIAL;
                    return true;
                }
        }

        //System.out.println("DROWN:" + Strategium.nearestWater);

        if (Strategium.nearestWater != null) {

            if (Strategium.nearestWater.equals(rc.getLocation())) {
                for (Direction dir : Navigation.moveAwayFrom(rc.getLocation()))
                    if (Strategium.canSafelyMove(dir)) {
                        rc.move(dir);
                        return true;
                    }
                return true;
            } else return Navigation.bugPath(Strategium.nearestWater);

        }

        return patrol();
    }

    private static boolean patrol() throws GameActionException {

        if (Strategium.HQLocation == null) return false;

        if (waypoint == null || rc.getLocation().equals(waypoint) || Navigation.frustration >= 30) {

            switch (state) {
                case PREDATOR:
                    waypoint = null;
                    if (rc.getRoundNum() > 1400) {
                        if (Strategium.enemyHQLocation != null) waypoint = Strategium.enemyHQLocation;
                        else if (!Strategium.potentialEnemyHQLocations.isEmpty())
                            waypoint = Strategium.potentialEnemyHQLocations.get(
                                    Strategium.rand.nextInt(Strategium.potentialEnemyHQLocations.size()));
                    }
                    else {
                        roam();
                    }

                    if (waypoint == null)
                        waypoint = getTarget(Strategium.enemyBuildings);
                    if (waypoint == null) waypoint = new MapLocation(
                            Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));

                    break;
                case LANDSCAPER_GATHERER:
                    waypoint = null;
                    if(Strategium.nearestPayload != null)
                        System.out.println(Strategium.nearestPayload.location);
                    if(!rc.isCurrentlyHoldingUnit()) {
                        if(Strategium.nearestPayload != null) {
                            if(Navigation.aerialDistance(Strategium.nearestPayload.location,
                                    Strategium.HQLocation) != 1) {
                                if(Strategium.enemyHQLocation != null) {
                                    if(Navigation.aerialDistance(Strategium.nearestPayload.location,
                                            Strategium.enemyHQLocation) > 1) {
                                       if(attack(Strategium.nearestPayload))
                                           return true;
                                    }
                                }
                                else
                                    if(attack(Strategium.nearestPayload))
                                        return true;
                            }
                        }
                        else {
                            roam();
                        }
                    }
                    else {
                        roam();
                    }
                    break;
                case SWARMER:
                    waypoint = null;

                    if (Strategium.enemyHQLocation != null) waypoint = Strategium.enemyHQLocation;
                    else if (!Strategium.potentialEnemyHQLocations.isEmpty())
                        waypoint = Strategium.potentialEnemyHQLocations.get(
                                Strategium.rand.nextInt(Strategium.potentialEnemyHQLocations.size()));


                    if (waypoint == null)
                        waypoint = getTarget(Strategium.enemyBuildings);

                    if (waypoint == null) waypoint = new MapLocation(
                            Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));


                    break;
                /*
                case TAXI:
                    waypoint = null;

                    if(rc.isCurrentlyHoldingUnit()) {
                        waypoint = Strategium.currentEnemyHQTarget;


                        if (waypoint == null)
                            waypoint = getTarget(Strategium.enemyBuildings);

                        if (waypoint == null) waypoint = new MapLocation(
                                Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));
                    } else waypoint = Strategium.HQLocation;
                    break;

                 */
                case SENTRY:
                    if(Navigation.aerialDistance(Strategium.HQLocation) > patrolRange)
                        return Navigation.bugPath(Strategium.HQLocation);
                    if(Navigation.aerialDistance(Strategium.HQLocation) < patrolRange) {
                        List<Direction> dirs = Navigation.moveAwayFrom(Strategium.HQLocation);
                        for (Direction dir : dirs)
                            if(Strategium.canSafelyMove(dir)){
                                rc.move(dir);
                                return true;
                            }
                        return false;
                    }
                    Direction dir = Navigation.clockwiseSquare(Strategium.HQLocation);
                    if(Strategium.canSafelyMove(dir)){
                        rc.move(dir);
                        return true;
                    }
                    return false;


            }

        }
        /*
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo robot : robots) {
            if(rc.isCurrentlyHoldingUnit()) {
                if (robot != null) if (robot.type == RobotType.HQ && robot.team == Strategium.opponentTeam)
                    for (Direction dir : dir8)
                        if (rc.canDropUnit(dir))
                            if (Navigation.aerialDistance(robot.getLocation(), rc.adjacentLocation(dir)) <= 4 &&
                            !rc.senseFlooding(rc.getLocation().add(dir))) {
                                rc.dropUnit(dir);
                                payload = Payload.POTENTIAL;
                                state = State.PREDATOR;
                                return true;
                            }
            }
        }
         */
        rc.setIndicatorLine(rc.getLocation(), waypoint, 255, 255, 255);
        //System.out.println("FRUSTRATION: " + Navigation.frustration);

        return Navigation.bugPath(waypoint);

    }

    private static boolean climb() throws GameActionException {
        switch (state) {
            case SWARMER:
                for (Direction dir : dir8)
                    if (rc.canDropUnit(dir))
                        if (goodSwarmingLandingSpot(rc.adjacentLocation(dir)))
                        {
                            rc.dropUnit(dir);
                            state = State.SWARMER;
                            payload = Payload.POTENTIAL;
                            crunchComplete = true;
                            //System.out.println("Svarmovao");
                            return true;
                        }
                if (crunchComplete) {
                    rc.disintegrate();
                }
                return patrol();
            case PREDATOR:
            case SENTRY:
            case TAXI:
                for (Direction dir : dir8)
                    if (rc.canDropUnit(dir))
                    if (Navigation.goodLandingSpot(rc.adjacentLocation(dir)))
                         {
                            rc.dropUnit(dir);
                            state = State.PREDATOR;
                            payload = Payload.POTENTIAL;
                            return true;
                        }
                return patrol();
        }
        return false;
    }

    private static boolean remove() throws GameActionException {
        for (Direction dir : dir8)
            if (rc.canDropUnit(dir))
                if (Navigation.aerialDistance(Strategium.HQLocation, rc.adjacentLocation(dir)) > 3) {
                    rc.dropUnit(dir);
                    state = State.PREDATOR;
                    payload = Payload.POTENTIAL;
                    return true;
                }

        return patrol();
    }
}

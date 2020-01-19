package Mark5.robots;

import Mark5.sensors.DroneSensor;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import Mark5.utils.Wall;
import battlecode.common.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Drone {

    public enum State {
        SENTRY,
        TAXI,
        PREDATOR,
        SWARMER
    }

    private enum Payload {
        POTENTIAL,
        FRIENDLY_LANDSCAPER,
        RUSH_MINER,
        BIOLOGICAL,
        ENEMY
    }

    public static State state = State.PREDATOR;

    private static Payload payload = Payload.POTENTIAL;

    private static int patrolRange = 3;

    private static int patrolWaypointIndex = 1;
    private static MapLocation waypoint;

    private static Iterator<MapLocation> target = null;

    private static MapLocation getTarget(List<MapLocation> list) {
        if (target != null) if (target.hasNext()) return target.next();
        if (!list.isEmpty()) {
            target = list.iterator();
            return target.next();
        }
        return null;
    }


    public static void run() throws GameActionException {

        Strategium.gatherInfo();

        patrolRange = 3 + (Strategium.numDronesMet - 24) / 8;
        switch (state) {
            case SENTRY:
                if ((Strategium.dronesMetWithLowerID - 4) * 5 > Strategium.numDronesMet) state = State.PREDATOR;
                break;
            case PREDATOR:
                if (Strategium.numDronesMet > 50 || rc.getRoundNum() > 2000) state = State.SWARMER;
                break;
            case SWARMER:
                //if (Strategium.dronesMetWithLowerID >= Strategium.numDronesMet * 9 / 10) state = State.TAXI;
                break;
        }
        System.out.println(state);
        System.out.println(payload);
        //System.out.println(state);

        if (!rc.isReady()) return;
        //System.out.println(payload);

        switch (payload) {
            case ENEMY:
            case BIOLOGICAL:
                drown();
                break;
            case POTENTIAL:
                if(DroneSensor.potentialTaxiPayload != null) {
                    if(attack(DroneSensor.potentialTaxiPayload))
                        return;
                }
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
                //if (Strategium.blockedUnit != null) if (attack(Strategium.blockedUnit)) break;
                patrol();
                break;
            case RUSH_MINER:
            case FRIENDLY_LANDSCAPER:
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
                        payload = Payload.RUSH_MINER;
                        state = State.TAXI;
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

        if (waypoint == null || rc.getLocation().equals(waypoint) || Navigation.frustration >= 100) {
            Navigation.frustration = 0;
            patrolWaypointIndex = (patrolWaypointIndex + 1) % 4;
            if (Strategium.rand.nextInt(100) > 90) patrolWaypointIndex = (patrolWaypointIndex + 1) % 4;

            switch (state) {
                case SENTRY:
                    switch (patrolWaypointIndex) {
                        case 0:
                            waypoint = Strategium.HQLocation.translate(patrolRange, patrolRange);
                            break;
                        case 1:
                            waypoint = Strategium.HQLocation.translate(-patrolRange, patrolRange);
                            break;
                        case 2:
                            waypoint = Strategium.HQLocation.translate(-patrolRange, -patrolRange);
                            break;
                        default:
                            waypoint = Strategium.HQLocation.translate(patrolRange, -patrolRange);
                            break;
                    }

                    boolean changed = false;

                    do {
                        changed = false;
                        for (MapLocation gun : Strategium.enemyNetGuns)
                            while (waypoint.isWithinDistanceSquared(
                                    gun, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
                                switch (patrolWaypointIndex) {
                                    case 0:
                                        waypoint = waypoint.translate(0, -1);
                                        break;
                                    case 1:
                                        waypoint = waypoint.translate(1, 0);
                                        break;
                                    case 2:
                                        waypoint = waypoint.translate(0, 1);
                                        break;
                                    default:
                                        waypoint = waypoint.translate(-1, 0);
                                        break;
                                }
                                changed = true;
                            }

                        for (MapLocation building : Strategium.enemyBuildings) {
                            if (building.equals(waypoint)) {
                                switch (patrolWaypointIndex) {
                                    case 0:
                                        waypoint = waypoint.translate(0, -1);
                                        break;
                                    case 1:
                                        waypoint = waypoint.translate(1, 0);
                                        break;
                                    case 2:
                                        waypoint = waypoint.translate(0, 1);
                                        break;
                                    default:
                                        waypoint = waypoint.translate(-1, 0);
                                        break;
                                }
                                changed = true;
                            }
                        }
                    } while (changed);
                    waypoint = Navigation.clamp(waypoint);
                    break;
                case PREDATOR:
                    waypoint = null;
                    if (Strategium.rand.nextInt(100) > 50 || rc.isCurrentlyHoldingUnit()) {
                        if (Strategium.enemyHQLocation != null) waypoint = Strategium.enemyHQLocation;
                        else if (!Strategium.potentialEnemyHQLocations.isEmpty())
                            waypoint = Strategium.potentialEnemyHQLocations.get(
                                    Strategium.rand.nextInt(Strategium.potentialEnemyHQLocations.size()));
                    }

                    if (waypoint == null)
                        waypoint = getTarget(Strategium.enemyBuildings);
                    if (waypoint == null) waypoint = new MapLocation(
                            Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));


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

            }

        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo robot : robots) {
            if(rc.isCurrentlyHoldingUnit()) {
                if (robot != null) if (robot.type == RobotType.HQ && robot.team == Strategium.opponentTeam)
                    for (Direction dir : dir8)
                        if (rc.canDropUnit(dir))
                            if (Navigation.aerialDistance(robot.getLocation(), rc.adjacentLocation(dir)) <= 4) {
                                rc.dropUnit(dir);
                                payload = Payload.POTENTIAL;
                                state = State.PREDATOR;
                                return true;
                            }
            }
        }
        return Navigation.bugPath(waypoint);

    }

    private static boolean climb() throws GameActionException {
        switch (state) {
            case SENTRY:
                for (Direction dir : dir8)
                    if (rc.canDropUnit(dir))
                        if (Wall.isOnWall(dir) &&
                                !rc.adjacentLocation(dir).equals(Strategium.HQLocation.translate(-2, -2))) {
                        rc.dropUnit(dir);
                        payload = Payload.POTENTIAL;
                        return true;
                    }
                if (Navigation.frustration >= 100) {
                    Navigation.frustration = 0;
                }
                return Navigation.bugPath(Strategium.HQLocation);
            case SWARMER:
                state = State.TAXI;
            case PREDATOR:
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

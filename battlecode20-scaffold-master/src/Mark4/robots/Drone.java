package Mark4.robots;

import Mark4.utils.Navigation;
import Mark4.utils.Strategium;
import Mark4.utils.Wall;
import battlecode.common.*;

import java.util.Iterator;
import java.util.Set;

import static Mark4.RobotPlayer.dir8;
import static Mark4.RobotPlayer.rc;

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
        UNRULY_MINER,
        BIOLOGICAL,
        ENEMY
    }

    public static State state = State.SENTRY;

    private static Payload payload = Payload.POTENTIAL;

    private static int patrolRange = 3;

    private static int patrolWaypointIndex = 1;
    private static MapLocation waypoint;

    private static Iterator<MapLocation> target = null;

    private static MapLocation getTarget(Set<MapLocation> set) {
        if (target != null) if (target.hasNext()) return target.next();
        if (!set.isEmpty()) {
            target = set.iterator();
            return target.next();
        }
        return null;
    }


    public static void run() throws GameActionException {

        Strategium.gatherInfo();

        patrolRange = 3 + rc.getRobotCount() / 8;
        switch (state) {
            case SENTRY:
                if ((Strategium.dronesMetWithLowerID - 4) * 5 > Strategium.numDronesMet) state = State.PREDATOR;
                break;
            case PREDATOR:
                if (Strategium.numDronesMet > 50 || rc.getRoundNum() > 2000) state = State.SWARMER;
                break;
            case SWARMER:
                if (Strategium.dronesMetWithLowerID >= Strategium.numDronesMet * 9 / 10) state = State.TAXI;
                break;
        }

        System.out.println(state);

        if (!rc.isReady()) return;
        System.out.println(payload);

        switch (payload) {
            case ENEMY:
            case BIOLOGICAL:
                drown();
                break;
            case POTENTIAL:
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
                if (Strategium.nearestEnemyUnit != null) if (attack(Strategium.nearestEnemyUnit)) break;
                if (Strategium.blockingUnit != null) if (attack(Strategium.blockingUnit)) break;
                if (Strategium.blockedUnit != null) if (attack(Strategium.blockedUnit)) break;
                patrol();
                break;
            case UNRULY_MINER:
                remove();
                break;
            case FRIENDLY_LANDSCAPER:
                climb();
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
                        payload = Payload.UNRULY_MINER;
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

        System.out.println("DROWN:" + Strategium.nearestWater);

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
                        for (MapLocation gun : Strategium.enemyNetGuns.keySet())
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

                        for (MapLocation building : Strategium.enemyBuildings.keySet()) {
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
                        waypoint = getTarget(Strategium.enemyBuildings.keySet());
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
                        waypoint = getTarget(Strategium.enemyBuildings.keySet());

                    if (waypoint == null) waypoint = new MapLocation(
                            Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));


                    break;
                case TAXI:
                    waypoint = null;

                    if(rc.isCurrentlyHoldingUnit()){
                        if (Strategium.enemyHQLocation != null) waypoint = Strategium.enemyHQLocation;
                        else if (!Strategium.potentialEnemyHQLocations.isEmpty())
                            waypoint = Strategium.potentialEnemyHQLocations.get(
                                    Strategium.rand.nextInt(Strategium.potentialEnemyHQLocations.size()));


                        if (waypoint == null)
                            waypoint = getTarget(Strategium.enemyBuildings.keySet());

                        if (waypoint == null) waypoint = new MapLocation(
                                Strategium.rand.nextInt(rc.getMapWidth()), Strategium.rand.nextInt(rc.getMapHeight()));

                    } else waypoint = Strategium.HQLocation;
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
                    payload = Payload.POTENTIAL;
                    return true;
                }

        return patrol();
    }
}

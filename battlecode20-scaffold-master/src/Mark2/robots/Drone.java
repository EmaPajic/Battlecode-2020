package Mark2.robots;

import Mark2.utils.Navigation;
import Mark2.utils.Strategium;
import Mark2.utils.Wall;
import battlecode.common.*;

import static Mark2.RobotPlayer.dir8;
import static Mark2.RobotPlayer.rc;

public class Drone {

    private enum State {
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
    private static State state = State.SENTRY;

    private static Payload payload = Payload.POTENTIAL;

    private static int patrolRange = 3;

    private static int patrolWaypointIndex = 1;
    private static MapLocation waypoint;


    public static void run() throws GameActionException {

        Strategium.gatherInfo();

        patrolRange = 3 + rc.getRobotCount() / 8;

        if (!rc.isReady()) return; else System.out.println("NOT READY");
        System.out.println(payload);

        switch (payload) {
            case ENEMY:
            case BIOLOGICAL:
                drown();
            break;
            case POTENTIAL:
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
            if(target.team == Strategium.myTeam) {
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

        System.out.println("PATROL:" + waypoint);


        if (Strategium.HQLocation == null) return false;

        if(waypoint == null || rc.getLocation().equals(waypoint) || Navigation.frustration >= 100) {
            Navigation.frustration = 0;
            patrolWaypointIndex = (patrolWaypointIndex + 1) % 4;
            if(Strategium.rand.nextInt(100) > 90) patrolWaypointIndex = (patrolWaypointIndex + 1) % 4;


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
        }

        return Navigation.bugPath(waypoint);

    }

    private static boolean climb() throws GameActionException {
        for (Direction dir : dir8) if(rc.canDropUnit(dir)) if(Wall.isOnWall(dir)) {
            rc.dropUnit(dir);
            payload = Payload.POTENTIAL;
            return true;
        }
        if(Navigation.frustration >= 100){
            Navigation.frustration = 0;
            state = State.PREDATOR;
        }
        return Navigation.bugPath(Strategium.HQLocation);
    }

    private static boolean remove() throws GameActionException {
        for (Direction dir : dir8) if(rc.canDropUnit(dir))
            if(Navigation.aerialDistance(Strategium.HQLocation, rc.adjacentLocation(dir)) > 3) {
            rc.dropUnit(dir);
            payload = Payload.POTENTIAL;
            return true;
        }

        return patrol();
    }
}

package Mark2.robots;

import Mark2.utils.Navigation;
import Mark2.utils.Strategium;
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

    private static State state = State.SENTRY;

    private static int patrolRange = 3;

    private static int patrolWaypointIndex = 0;
    private static MapLocation waypoint;

    public static void run() throws GameActionException {

        Strategium.gatherInfo();
        System.out.println(Clock.getBytecodeNum());

        patrolRange = 3 + rc.getRobotCount() / 8;

        if (!rc.isReady()) return;

        switch (state) {
            case SENTRY:
                if (rc.isCurrentlyHoldingUnit()) {
                    drown();
                } else if (Strategium.nearestEnemyUnit != null) {
                    if (!attack(Strategium.nearestEnemyUnit)) patrol();
                } else patrol();
            case TAXI: break;

        }


    }

    private static boolean attack(RobotInfo target) throws GameActionException {
        if (rc.canPickUpUnit(target.ID)) {
            rc.pickUpUnit(target.ID);
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
                    return true;
                }
        }

        if (Strategium.nearestWater != null) {
            System.out.println(Strategium.nearestWater);
            if (Strategium.nearestWater.equals(rc.getLocation())) {
                for (Direction dir : Navigation.moveAwayFrom(Strategium.HQLocation))
                    if (rc.canMove(dir)) {
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

        if(waypoint == null || rc.getLocation().equals(waypoint)) {
            patrolWaypointIndex = (patrolWaypointIndex + 1) % 4;


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
}

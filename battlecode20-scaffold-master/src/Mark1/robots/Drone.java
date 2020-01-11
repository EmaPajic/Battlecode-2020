package Mark1.robots;

import Mark1.utils.Navigation;
import Mark1.utils.Strategium;
import battlecode.common.*;

import static Mark1.RobotPlayer.dir8;
import static Mark1.RobotPlayer.rc;

public class Drone {

    private enum State {
        SENTRY,
        TAXI,
        PREDATOR,
        SWARMER
    }

    private static State state = State.SENTRY;

    private static int patrolRange = 3;

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
        if (rc.canMove(Navigation.moveTowards(target.location))) {
            rc.move(Navigation.moveTowards(target.location));
            return true;
        }
        return false;
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
        if (Navigation.aerialDistance(Strategium.HQLocation) > patrolRange)
            return Navigation.bugPath(Strategium.HQLocation);


        if (Navigation.aerialDistance(Strategium.HQLocation) < patrolRange) {
            for (Direction dir : Navigation.moveAwayFrom(Strategium.HQLocation))
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return true;
                }
            return false;
        }

        if (rc.canMove(Navigation.clockwiseSquare(Strategium.HQLocation))) {
            rc.move(Navigation.clockwiseSquare(Strategium.HQLocation));
            return true;
        }

        return false;

    }
}

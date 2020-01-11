package Mark1.robots;

import Mark1.utils.Navigation;
import Mark1.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

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

    public static void run() throws GameActionException {

        Strategium.gatherInfo();

        if (!rc.isReady()) return;

        switch (state) {
            case SENTRY:
                if (rc.isCurrentlyHoldingUnit()) {
                    if (!drown()) patrol();
                } else if (Strategium.nearestEnemyUnit != null) {
                    if (!attack(Strategium.nearestEnemyUnit)) patrol();
                } else patrol();

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
            if (Strategium.water[adj.x][adj.y]) if (rc.canDropUnit(dir)) {
                rc.dropUnit(dir);
                return true;
            }
        }
        if (Strategium.nearestWater != null) {
            if (Strategium.nearestWater.equals(rc.getLocation())) {
                for (Direction dir : Navigation.moveAwayFrom(Strategium.HQLocation))
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        return true;
                    }
                return true;
            } else if (rc.canMove(Navigation.moveTowards(Strategium.nearestWater))) {
                rc.move(Navigation.moveTowards(Strategium.nearestWater));
                return true;
            } else return false;

        }

        return false;
    }

    private static boolean patrol() throws GameActionException {
        if (Strategium.HQLocation == null) return false;
        if (Navigation.aerialDistance(Strategium.HQLocation) > 3) {
            if (rc.canMove(Navigation.moveTowards(Strategium.HQLocation))) {
                rc.move(Navigation.moveTowards(Strategium.HQLocation));
                return true;
            } else return false;
        }

        if (Navigation.aerialDistance(Strategium.HQLocation) < 3) {
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

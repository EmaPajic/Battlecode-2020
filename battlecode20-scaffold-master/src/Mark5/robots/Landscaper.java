package Mark5.robots;

import Mark5.utils.Lattice;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Landscaper {

    public static void run() throws GameActionException {
        Strategium.gatherInfo();

        if (!rc.isReady()) return;

        if (!Strategium.overlapLocations.contains(rc.getLocation()))
            for (MapLocation location : Strategium.overlapLocations) {
                if (rc.getLocation().isAdjacentTo(location))
                    if (rc.canMove(Navigation.moveTowards(location))) {
                        rc.move(Navigation.moveTowards(location));
                        return;
                    }
            }

        for (Direction dir : dir8) {
            if (rc.adjacentLocation(dir).equals(Strategium.nearestBuriedFriendlyBuilding))
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                    return;
                }

            if (rc.adjacentLocation(dir).equals(Strategium.nearestEnemyBuilding))
                if (rc.canDepositDirt(dir)) {
                    rc.depositDirt(dir);
                    return;
                }

        }

        if (Strategium.nearestBuriedFriendlyBuilding != null)
           if(defend(Strategium.nearestBuriedFriendlyBuilding)) return;

        if (Strategium.nearestEnemyBuilding != null)
           if(attack(Strategium.nearestEnemyBuilding)) return;

        if(Strategium.nearestWater != null)
            if(drain(Strategium.nearestWater)) return;


    }

    private static boolean defend(MapLocation location) throws GameActionException {
        if (rc.getLocation().isAdjacentTo(location)) {

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.maxDeposit(rc.adjacentLocation(dir)) > 0 && !Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDepositDirt(dir)) {
                            rc.depositDirt(dir);
                            return true;
                        }

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDepositDirt(dir)) {
                            rc.depositDirt(dir);
                            return true;
                        }

        }
        return Navigation.bugPath(location);
    }

    private static boolean attack(MapLocation location) throws GameActionException {
        if (rc.getLocation().isAdjacentTo(location)) {

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                            return true;
                        }

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (rc.canDigDirt(dir)) {
                        rc.digDirt(dir);
                        return true;
                    }

        }
        return Navigation.bugPath(location);
    }

    private static boolean drain(MapLocation location) throws GameActionException {
        int waterLevel = (int) GameConstants.getWaterLevel(rc.getRoundNum() + 100);
        if(!rc.getLocation().isAdjacentTo(location)) return Navigation.bugPath(location);
        if(waterLevel - Strategium.elevation[location.x][location.y] < 50) {
            if(rc.canDepositDirt(rc.getLocation().directionTo(location))){
                rc.depositDirt(rc.getLocation().directionTo(location));
                return true;
            }
            Direction dir = Lattice.bestDigDirection();
            if(dir == null) return false;
            if (rc.canDigDirt(dir)){
                rc.digDirt(dir);
                return true;
            }
            return false;
        }
        if(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            Direction dir = Lattice.bestDigDirection();
            if(dir != null)
            if (rc.canDigDirt(dir)){
                rc.digDirt(dir);
                return true;
            }
        }
        Direction dir = Lattice.bestDepositDirection();
        if (rc.canDepositDirt(dir)){
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

}

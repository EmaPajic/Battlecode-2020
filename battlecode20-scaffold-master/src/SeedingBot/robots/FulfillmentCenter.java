package SeedingBot.robots;

import SeedingBot.sensors.FulfillmentCenterSensor;
import SeedingBot.utils.Navigation;
import SeedingBot.utils.Strategium;
import SeedingBot.RobotPlayer;
import battlecode.common.*;

import java.util.List;

import static SeedingBot.RobotPlayer.*;

public class FulfillmentCenter {

    public enum DroneBuildingImportance {
        TAXI_NEEDED,
        PERIODIC_BUILDING
    }

    public static DroneBuildingImportance droneBuildingImportance = DroneBuildingImportance.PERIODIC_BUILDING;

    public static int numDrones = 0;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        switch(droneBuildingImportance) {
            case TAXI_NEEDED:
                Direction dirToRush = rc.getLocation().directionTo(FulfillmentCenterSensor.nearestRushMinerLocation);
                List<Direction> towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToRush.opposite()));
                for(Direction dir : towards)
                    if(tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                        ++numDrones;
                        droneBuildingImportance = DroneBuildingImportance.PERIODIC_BUILDING;
                        return;
                    }
                for (Direction dir : dir8) {
                    if(tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                        ++numDrones;
                        droneBuildingImportance = DroneBuildingImportance.PERIODIC_BUILDING;
                        return;
                    }
                }
                break;
            case PERIODIC_BUILDING:
                if (numDrones < 5 && (rc.getTeamSoup() > 650 ||
                        FulfillmentCenterSensor.enemyLandscapersNearby) &&
                        !FulfillmentCenterSensor.enemyNetGunsNearby) {
                    if (FulfillmentCenterSensor.nearestEnemyLandscaperLocation != null) {
                        Direction dirToEnemy =
                                rc.getLocation().directionTo(FulfillmentCenterSensor.nearestEnemyLandscaperLocation);
                        towards = Navigation.moveAwayFrom(rc.getLocation().add(dirToEnemy.opposite()));
                        for (Direction dir : towards)
                            if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                                ++numDrones;
                                return;
                            }
                    }
                    for (Direction dir : dir8) {
                        if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                            ++numDrones;
                            return;
                        }
                    }
                }
                break;
        }
    }
}

package Mark5.robots;

import Mark5.sensors.FulfillmentCenterSensor;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import Mark5.RobotPlayer;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

import static Mark5.RobotPlayer.*;

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
                if(rc.getRoundNum() > 1200){ // nemamo polje koje cuva koliko neprijateljskih lendskejpera imamo u okolini

                    if(!FulfillmentCenterSensor.dirToBuild.isEmpty()) {
                        for (Direction dir : FulfillmentCenterSensor.dirToBuild) {
                            if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                                ++numDrones;
                                return;
                            }
                        }
                    }else if(!FulfillmentCenterSensor.enemyNetGunsNearby){
                        if(tryBuild(RobotType.DELIVERY_DRONE, rc.getLocation().directionTo(Strategium.enemyHQLocation))){
                            ++numDrones;
                            return;
                        }
                        for(Direction dir : dir8){
                            if(tryBuild(RobotType.DELIVERY_DRONE, dir)){
                                ++numDrones;
                                return;
                            }
                        }

                    }

                }
                else if (numDrones < 5 || numDrones < FulfillmentCenterSensor.importantEnemyUnitsNum) {
                    if ((rc.getTeamSoup() > 650 ||
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
                }
                break;
        }
    }
}

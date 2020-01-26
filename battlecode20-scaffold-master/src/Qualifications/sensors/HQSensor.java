package Qualifications.sensors;

import Qualifications.utils.Navigation;
import Qualifications.utils.Strategium;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Qualifications.RobotPlayer.myFun;
import static Qualifications.RobotPlayer.rc;
import static Qualifications.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class HQSensor {
    public static int totalMiners = 0;

    public static void init(){
        soup = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    }

    public static void sense() throws GameActionException {
        if(rc.getRoundNum() == 1) {
            int xMin = rc.getLocation().x - 6;
            int yMin = rc.getLocation().y - 6;
            int xMax = rc.getLocation().x + 6;
            int yMax = rc.getLocation().y + 6;
            for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
                for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                    MapLocation location = new MapLocation(i, j);
                    if (rc.canSenseLocation(location)) {
                        knownSoup += rc.senseSoup(location);
                        if (rc.senseSoup(location) > 0) {
                            if (Navigation.aerialDistance(rc.getLocation(), i, j) <
                                    Navigation.aerialDistance(rc.getLocation(), nearestSoup))
                                nearestSoup = new MapLocation(i, j);
                        }
                    }
                }
        }
        totalMiners = min(3 + knownSoup/700, 10) +
                leastAmountOfSoup/500;
        if(rc.getRoundNum() > 550) totalMiners += 3;
    }
}

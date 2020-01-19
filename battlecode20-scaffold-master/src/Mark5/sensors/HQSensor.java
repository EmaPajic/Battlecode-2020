package Mark5.sensors;

import Mark5.utils.Navigation;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.rc;
import static Mark5.utils.Strategium.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class HQSensor {
    public static int totalMiners = 0;

    public static void init(){



    }

    public static void sense() throws GameActionException {
        if (rc.getRoundNum() == 1) {
            int xMin = rc.getLocation().x - 6;
            int yMin = rc.getLocation().y - 6;
            int xMax = rc.getLocation().x + 6;
            int yMax = rc.getLocation().y + 6;
            for (int i = max(0, xMin); i <= min(xMax, rc.getMapWidth() - 1); i++)
                for (int j = max(0, yMin); j <= min(yMax, rc.getMapHeight() - 1); j++) {

                    MapLocation location = new MapLocation(i, j);
                    if (rc.canSenseLocation(location)) {
                        knownSoup += rc.senseSoup(location);

                    }

                }
            totalMiners = max(3 + knownSoup/700, 10) + max((rc.getRoundNum()/200 - 1), 0);
        }
    }
}

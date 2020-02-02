package Mark5.robots;

import Mark5.RobotPlayer;
import Mark5.utils.Blockchain;
import Mark5.utils.Strategium;
import battlecode.common.GameActionException;

public class Refinery {

    public static void run() throws GameActionException {
        Strategium.gatherInfo();
        if(RobotPlayer.turnCount == 1)
            Blockchain.reportRefineryLocation(1);
    }
}

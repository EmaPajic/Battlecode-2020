package Mark1;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

import static Mark1.RobotPlayer.rc;

public class Strategium {

    static boolean upToDate = false;
    static ArrayList<Transaction> transactions = new ArrayList<>();
    static MapLocation HQLocation = null;
    static Team myTeam, opponentTeam;

    static void init() {
        myTeam = rc.getTeam();
        opponentTeam = myTeam == Team.A ? Team.B : Team.A;
    }

    static void gatherInfo() throws GameActionException {
        gatherInfo(0);
    }

    static void gatherInfo(int bytecodesReq) throws GameActionException {

        upToDate = false;

        do {

            parseTransactions(BlockchainUtils.parseBlockchain(transactions));

        } while (Clock.getBytecodesLeft() > bytecodesReq && !upToDate);
    }

    static private void parseTransactions(List<Transaction> transactions) {
        if (transactions == null) {
            upToDate = true;
            return;
        }

        while (!transactions.isEmpty()) {

            int[] message = transactions.get(0).getMessage();

            switch (BlockchainUtils.getType(message)) {
                case 73:
                    if (HQLocation != null) break;
                    HQLocation = new MapLocation(message[0], message[1]);
                    System.out.println("HQ Located at: (" + HQLocation.x + ", " + HQLocation.y + ")");
                    break;
                default:
                    break;
            }

            transactions.remove(0);
        }

    }
}

package Mark1;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Strategium {

    static boolean upToDate = false;
    static ArrayList<Transaction> transactions = new ArrayList<>();
    static MapLocation HQLocation = null;

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

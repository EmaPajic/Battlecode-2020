package Mark3.utils;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Transaction;
import static Mark3.RobotPlayer.rc;

import java.util.ArrayList;
import java.util.List;

public class Blockchain {

    static final int[] acceptedTypes = {73};
    static private int parsingProgress = 1;
    static int opponentTransactionCosts = 0;
    static int opponentTransactions = 0;
    static int opponentTransactionMinFee = 1000;
    static int opponentTransactionMaxFee = 0;

    static int getType(Transaction transaction){
        return (transaction.getMessage()[4] >> 24) & 255;
    }

    static int getType(int[] message){
        return (message[4] >> 24) & 255;
    }

    static int getSender(Transaction transaction){
        return transaction.getMessage()[3];
    }

    static void addAuth(int[] message, int type){
        message[3] = rc.getID();
        message[4] = (rc.getRoundNum() & ((1 << 16) - 1)) | (type << 24);
        int checksum = 0;
        for (int i = 0; i < 7; i++) if (i != 4){
            checksum ^= message[i] & 255;
            checksum ^= (message[i] >> 8) & 255;
            checksum ^= (message[i] >> 16) & 255;
            checksum ^= (message[i] >> 24) & 255;
        }
        message[4] |= (~(checksum << 16)) & (255 << 16);
    }

    static private boolean checkAuth(Transaction transaction){
        int[] message = transaction.getMessage();
        if(message.length != 7) return false;
        if((message[4] & ((1 << 16) - 1)) > parsingProgress) return false;
        int checksum = 0;
        for (int i = 0; i < 7; i++) if (i != 4){
            checksum ^= message[i] & 255;
            checksum ^= (message[i] >> 8) & 255;
            checksum ^= (message[i] >> 16) & 255;
            checksum ^= (message[i] >> 24) & 255;
        }
        for (int type : acceptedTypes) if(((message[4] >> 24) & 255) == type)
            return (message[4] & (255 << 16)) == (~(checksum << 16) & (255 << 16));

        opponentTransactionCosts += transaction.getCost();
        opponentTransactions++;
        if (transaction.getCost() > opponentTransactionMaxFee) opponentTransactionMaxFee = transaction.getCost();
        if (transaction.getCost() < opponentTransactionMinFee) opponentTransactionMinFee = transaction.getCost();

        return false;
    }

    public static boolean reportHQLocation(int fee) throws GameActionException {
        int[] message = new int[7];
        message[0] = rc.getLocation().x;
        message[1] = rc.getLocation().y;
        addAuth(message, 73);
        if(!rc.canSubmitTransaction(message, fee)) { System.out.println("FEJL"); return false; }
        rc.submitTransaction(message, fee);
        return true;
    }

    static List<Transaction> parseBlockchain(List<Transaction> target) throws GameActionException {
        if (parsingProgress < rc.getRoundNum()) {
            if(target == null) target = new ArrayList<>();
            Transaction[] block = rc.getBlock(parsingProgress);
            for(Transaction transaction : block) if(checkAuth(transaction)) target.add(transaction);
            parsingProgress++;
            return target;
        }
        return null;
    }
}

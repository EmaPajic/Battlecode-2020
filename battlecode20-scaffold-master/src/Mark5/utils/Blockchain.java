package Mark5.utils;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Team;
import battlecode.common.Transaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static Mark5.RobotPlayer.rc;
import static java.lang.Integer.min;

public class Blockchain {

    static final int[] acceptedTypes = {73, 42};
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
        message[3] = rc.getID() + (rc.getTeam() == Team.B ? GameConstants.MAX_ROBOT_ID : 0);
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
        if(message[3] > GameConstants.MAX_ROBOT_ID ^ rc.getTeam() == Team.B) return false;
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

    public static boolean reportRefineryLocation(int fee) throws GameActionException {
        int[] message = new int[7];
        message[5] = rc.getLocation().x;
        message[6] = rc.getLocation().y;
        addAuth(message, 42);
        if(!rc.canSubmitTransaction(message, fee)) { return false; }
        rc.submitTransaction(message, fee);
        return true;
    }
    public static void setBlockchainPointer(int nextMsgPointerVal){
        parsingProgress = nextMsgPointerVal;
    }
    public static int getBlockchainPointer(){
        return parsingProgress;
    }
    public static void parseBlockchain(LinkedList<Transaction> transactions) throws GameActionException {
        if (parsingProgress < rc.getRoundNum()) {
            //System.out.println("here?");
            Transaction[] block = rc.getBlock(parsingProgress);
            for(Transaction transaction : block) if(checkAuth(transaction)) transactions.add(transaction);
            parsingProgress++;
        }
    }
}

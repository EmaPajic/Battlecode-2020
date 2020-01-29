package Mark5.utils;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static Mark5.RobotPlayer.rc;
import static java.lang.Integer.min;

public class Blockchain {
    static final int[] acceptedTypes = {73, 42, 17, 98};
    public static int parsingProgress = 1;
    static int opponentTransactionCosts = 0;
    static int opponentTransactions = 0;
    static int opponentTransactionMinFee = 1000;
    static int opponentTransactionMaxFee = 0;
    static int[] lastTurnSent;
    public static int[][] falseMessages = {{-6333, 9396173, 0, 64824852, 0, 0, 8495},
            {1793301902, 0, 0, 0, 0, 0, 0}, {2012808, 1, 12345, 0, 0, 0, 111105},
            {274216, 2624708, 0, 0, 0, 0, 2140403},
            {1539306196, -1216614434, 1861738430, -571490483, -1142980880, 2008993856, -276956202},
            {51351235, 59, 7, 5, 0, 1543200769, -699082112}, {2, 17, 19, 25, 19, 4, -14976},
            {-999777234, 0, 2949, 272, 7311469, 9014476, 4968225}, {444444444, 982, 49, 46, 0, 0, 0},
            {18537659, 3, 55, 0, 0, 0, 0},
            {483608781, 1381610858, 33213801, 157067759, 1704169077, 1285648416, 1172763438},
            {7079, 56160260, 34414615, 1041039905, 537723415, 0, 0},
            {-1995934416, 1683562844, 420546096, -781956804, 708194569, -898951878, 304908421}};
    public static int[] false2ndturn = {-841228799, -507634211, 1558560349, 1892830, 0, 0, 1903101830};
    public static int[] false3rdTurn = {374789, 33554431, 0, 0, 0, 0, 4534654};

    public static void init() {
         lastTurnSent = new int[GameConstants.MAX_ROBOT_ID + 1];
    }
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
        if((message[4] & ((1 << 16) - 1)) <= lastTurnSent[message[3] % GameConstants.MAX_ROBOT_ID]){
            System.out.println("NICE TRY");
            System.out.println(message[4] & ((1 << 16) - 1));
            System.out.println(lastTurnSent[message[3] % GameConstants.MAX_ROBOT_ID]);
            return false;
        }
        int checksum = 0;
        for (int i = 0; i < 7; i++) if (i != 4){
            checksum ^= message[i] & 255;
            checksum ^= (message[i] >> 8) & 255;
            checksum ^= (message[i] >> 16) & 255;
            checksum ^= (message[i] >> 24) & 255;
        }
        for (int type : acceptedTypes) if(((message[4] >> 24) & 255) == type)
            if ((message[4] & (255 << 16)) == (~(checksum << 16) & (255 << 16))){
                lastTurnSent[message[3] % GameConstants.MAX_ROBOT_ID] = message[4] & (1 << 16 - 1);
                return true;
            }

        //opponentTransactionCosts += transaction.getCost();
        //opponentTransactions++;
        //if (transaction.getCost() > opponentTransactionMaxFee) opponentTransactionMaxFee = transaction.getCost();
        //if (transaction.getCost() < opponentTransactionMinFee) opponentTransactionMinFee = transaction.getCost();

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

    public static boolean reportEnemyHQLocation(int fee) throws GameActionException {
        int[] message = new int[7];
        message[0] = Strategium.enemyHQLocation.x;
        message[1] = Strategium.enemyHQLocation.y;
        addAuth(message, 17);
        if(!rc.canSubmitTransaction(message, fee)) { System.out.println("FEJL"); return false; }
        rc.submitTransaction(message, fee);
        return true;
    }

    public static boolean reportEnemyNetGun(int fee) throws GameActionException {
        int[] message = new int[7];
        message[0] = Strategium.lastEnemyNetGunSeen.location.x;
        message[1] = Strategium.lastEnemyNetGunSeen.location.y;
        message[2] = Strategium.lastEnemyNetGunSeen.id;
        message[5] = Strategium.lastEnemyNetGunSeen.readyOnRound;
        addAuth(message, 98);
        if(!rc.canSubmitTransaction(message, fee)) { System.out.println("FEJL"); return false; }
        rc.submitTransaction(message, fee);
        return true;
    }

    public static int reportLocalSoup() throws GameActionException{
        MapLocation[] soupLocations  =  rc.senseNearbySoup();
        int totalSoup = 0;
        for(MapLocation loc : soupLocations){
            totalSoup += rc.senseSoup(loc);
        }
        return totalSoup;
    }
    public static boolean reportRefineryLocation(int fee) throws GameActionException {
        int[] message = new int[7];
        message[2] = Math.min(reportLocalSoup(), 2500);
        message[5] = rc.getLocation().x;
        message[6] = rc.getLocation().y;
        addAuth(message, 42);
        if(!rc.canSubmitTransaction(message, fee)) { return false; }
        rc.submitTransaction(message, fee);
        return true;
    }

    public static void parseBlockchain(LinkedList<Transaction> transactions) throws GameActionException {
        if (parsingProgress < rc.getRoundNum()) {
            //System.out.println("here?");
            Transaction[] block = rc.getBlock(parsingProgress);
            for(Transaction transaction : block) if(checkAuth(transaction)) transactions.add(transaction);
            parsingProgress++;
        }
    }

    public static boolean sendFalseMessage(int[] message, int fee) throws GameActionException {
        if(!rc.canSubmitTransaction(message, fee)) { System.out.println("FEJL"); return false; }
        rc.submitTransaction(message, fee);
        return true;
    }
}

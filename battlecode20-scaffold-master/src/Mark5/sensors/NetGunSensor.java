package Mark5.sensors;

import Mark5.RobotPlayer;
import Mark5.robots.FulfillmentCenter;
import Mark5.utils.Navigation;
import Mark5.utils.Strategium;
import Mark5.utils.Wall;
import battlecode.common.*;

import javax.naming.directory.DirContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static Mark5.RobotPlayer.*;
import static Mark5.RobotPlayer.hqLocation;
import static Mark5.utils.Strategium.*;
public class NetGunSensor {

    static class LocationComparator implements Comparator<RobotInfo> {
        @Override
        public int compare(RobotInfo rbA, RobotInfo rbB) {
            return Integer.compare(
                    Navigation.aerialDistance(rbA.location), Navigation.aerialDistance(rbB.location));
        }
    }

    static class teamComparator implements Comparator<RobotInfo> {
        @Override
        public int compare(RobotInfo rbA, RobotInfo rbB) {
            if(rbA.team == myTeam && rbB.team == opponentTeam)
                    return 1;
            else if(rbA.team == opponentTeam && rbB.team == myTeam)
                return -1;
            else return 0;
        }
    }

    // probably this is unnecessary as one attack per turn is possible?
    public static ArrayList<RobotInfo> tpLocToAttack = new ArrayList<>();
    public static ArrayList<RobotInfo> lpLocToAttack = new ArrayList<>();


    public static void senseNearbyUnits() throws GameActionException {
        tpLocToAttack.clear();
        lpLocToAttack.clear();


        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, Strategium.opponentTeam);
        for (RobotInfo robot : robots){
            if(robot.type == RobotType.DELIVERY_DRONE) {
                    if(robot.currentlyHoldingUnit){
                            tpLocToAttack.add(robot);
                    }else lpLocToAttack.add(robot);

            }
        }
        tpLocToAttack.sort(new teamComparator());
//        System.out.println(tpLocToAttack.get(0));
        tpLocToAttack.sort(new LocationComparator());
        lpLocToAttack.sort(new LocationComparator());
        tpLocToAttack.addAll(lpLocToAttack);

    }
    public static void sense() throws GameActionException{
        senseNearbyUnits();
    }
}

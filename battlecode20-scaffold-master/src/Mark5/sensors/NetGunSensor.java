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
import static java.lang.Math.max;
import static java.lang.Math.min;

public class NetGunSensor {

    static class LocationComparator implements Comparator<RobotInfo> {
        @Override
        public int compare(RobotInfo rbA, RobotInfo rbB) {
            return Integer.compare(
                    Navigation.aerialDistance(rbA.location), Navigation.aerialDistance(rbB.location));
        }
    }

    static class attackPriorityComparator implements Comparator<RobotInfo>  {
        @Override
        public int compare(RobotInfo rbA, RobotInfo rbB) {
            RobotInfo rbACarry  = null;
            RobotInfo rbBCarry = null;

            try {
                rbACarry = rc.senseRobot(rbA.heldUnitID);
                rbBCarry = rc.senseRobot(rbB.heldUnitID);

                if(rbACarry == null && rbBCarry != null)
                    return 1;
                else if(rbACarry != null && rbBCarry == null)
                    return -1;
                else if(rbACarry == null && rbBCarry == null) return 0;
            } catch (GameActionException e) {
                e.printStackTrace();
            }


            if(rbACarry.type == RobotType.MINER && rbBCarry.type != RobotType.MINER )
                return -1;
            else if(rbACarry.type != RobotType.MINER && rbBCarry.type == RobotType.MINER )
                return 1;
            else { // ako nose istu stvar, prioritizuj onog ko je iznad vode
                boolean onWaterA = false;
                boolean onWaterB = false;
                try{
                    onWaterA = rc.senseFlooding(rbA.location);
                    onWaterB = rc.senseFlooding(rbB.location);

                    if(onWaterA && !onWaterB){
                        return -1;
                    }
                    else if(!onWaterA && onWaterB){
                        return 1;
                    }
                } catch (GameActionException e){
                    e.printStackTrace();
                }
                // izmedju rc.location.x i y +- 2
                if (Navigation.aerialDistance(rc.getLocation(), rbA.location) > Navigation.aerialDistance(rc.getLocation(), rbB.location)) {
                    return 1;
                } else if(Navigation.aerialDistance(rc.getLocation(), rbA.location) < Navigation.aerialDistance(rc.getLocation(), rbB.location)){
                    return -1;
                } else return 0;
            }

        }
    }

    // probably this is unnecessary as one attack per turn is possible?
    public static ArrayList<RobotInfo> tpLocToAttack = new ArrayList<>();
    public static ArrayList<RobotInfo> lpLocToAttack = new ArrayList<>();

    public static boolean notAttackDrones = true;

    public static void senseNearbyUnits() throws GameActionException {
        tpLocToAttack.clear();
        lpLocToAttack.clear();
        // PRIORITET :
        // gadjaj prvo dronove sa minerima, ako su iznad vode
        // gadjaj prvo dronove sa lendskejperima, ako su iznad vode
        // gadjaj prvo dronove koji su prazni, pod uslovom da i dalje ima nasih lendskejpera i minera
        // ne gadjaj ako je neprijateljski dron iznad naseg zida ili praznog neprijateljskog drona ako je prazan nas zid

        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED);
        for (RobotInfo robot : robots){
            if(robot.team == myTeam) {
                switch(rc.getType()) {
                    case HQ:
                        if (robot.type == RobotType.LANDSCAPER || robot.type == RobotType.MINER) {
                            notAttackDrones = false; // ako se ovaj deo odnosi samo da ih nema na zidu, onda treba dodati samo jos taj uslov u if iznad
                        }
                        break;
                }
            } else {
                if (robot.type == RobotType.DELIVERY_DRONE) {
                    switch (rc.getType()) {
                        case HQ:
                            if (robot.location.isAdjacentTo(rc.getLocation()) && !robot.currentlyHoldingUnit) {

                            } else if (robot.currentlyHoldingUnit) {
                                tpLocToAttack.add(robot);
                            } else lpLocToAttack.add(robot);
                            break;
                        case NET_GUN:
                            if (robot.currentlyHoldingUnit) {
                                tpLocToAttack.add(robot);
                            } else lpLocToAttack.add(robot);
                            break;
                    }
                }
            }
        }

        tpLocToAttack.sort(new attackPriorityComparator());
//        System.out.println(tpLocToAttack.get(0));
//        tpLocToAttack.sort(new LocationComparator());
        if(notAttackDrones && rc.getType() == RobotType.HQ){
            // moze clear lpLocToAttack, ali ne mora

        } else{
            lpLocToAttack.sort(new LocationComparator());
            tpLocToAttack.addAll(lpLocToAttack);
            notAttackDrones = true;
        }



    }
    public static void sense() throws GameActionException{
        senseNearbyUnits();
    }
}

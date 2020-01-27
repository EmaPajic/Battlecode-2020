package Mark5.utils;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static Mark5.RobotPlayer.rc;
import static Mark5.sensors.NetGunSensor.sense;
import static Mark5.sensors.NetGunSensor.tpLocToAttack;


// netgunovi da priorituzuju protivnicke dronove koji nose protivnicke unite i iznad vode
public class NetGun {
    public MapLocation location;
    public int readyOnRound;
    public int id;

    public NetGun(RobotInfo robot){
        location = robot.location;
        id = robot.ID;
        readyOnRound = rc.getRoundNum() + (int) (robot.getCooldownTurns());
    }

    public NetGun(MapLocation location, int id, int readyOnRound){
        this.location = location;
        this.id = id;
        this.readyOnRound = readyOnRound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetGun netGun = (NetGun) o;
        return id == netGun.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static void run() throws GameActionException {
        sense();
        RobotInfo bestTarget = null;
        if(!tpLocToAttack.isEmpty()) {
            bestTarget = tpLocToAttack.get(0);
        }


        if (bestTarget != null) rc.shootUnit(bestTarget.ID);
    }
}

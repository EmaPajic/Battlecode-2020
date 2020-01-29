package Mark5.utils;

import battlecode.common.*;

import static Mark5.RobotPlayer.rc;
import static Mark5.sensors.NetGunSensor.sense;
import static Mark5.sensors.NetGunSensor.tpLocToAttack;
import static java.lang.Math.max;
import static java.lang.Math.min;


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
            if (rc.getType() == RobotType.HQ) {
                for (RobotInfo tpLoc : tpLocToAttack) {
                    if (!rc.senseFlooding(tpLoc.location)) {
                        if (tpLoc.currentlyHoldingUnit) {
                            if ((max(rc.getLocation().x - 2, 0) <= tpLoc.location.x && tpLoc.location.x <= min(rc.getLocation().x + 2, rc.getMapWidth() - 1))
                                        && (max(rc.getLocation().y - 2, 0) <= tpLoc.location.y && tpLoc.location.y <= min(rc.getLocation().y + 2, rc.getMapHeight() - 1))) {
                                    bestTarget = tpLoc;
                                    break;
                            }
                        }
                    } else {
                        bestTarget = tpLoc;
                        break;
                    }
                }
            } else { // maybe if not in HQ vicinity should be added
                bestTarget = tpLocToAttack.get(0);
            }
        }

        if (bestTarget != null) rc.shootUnit(bestTarget.ID);
    }
}

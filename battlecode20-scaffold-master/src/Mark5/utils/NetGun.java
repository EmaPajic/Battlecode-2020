package Mark5.utils;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static Mark5.RobotPlayer.rc;

public class NetGun {
    public MapLocation location;
    public int readyOnRound;
    public int id;

    public NetGun(RobotInfo robot){
        location = robot.location;
        id = robot.ID;
        readyOnRound = rc.getRoundNum() + 9;
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
}

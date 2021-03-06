package Mark5.utils;

import Mark5.sensors.DroneSensor;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Siege {
    public static MapLocation dropSite() throws GameActionException {
        System.out.println("DRONOVA: " + Strategium.enemyDrones.size());

        if(Navigation.aerialDistance(Strategium.enemyHQLocation) > 8 || Strategium.enemyDrones.size() < 4) return null;
        int waterLevel = (int) GameConstants.getWaterLevel(rc.getRoundNum() + 200) - 24;
        if(Strategium.enemyHQLocation == null) return null;

        if(Math.abs(Strategium.enemyHQLocation.x - rc.getLocation().x) <= 4) {
            if(rc.getLocation().x < Strategium.enemyHQLocation.x) {
                int xMin = Math.max(rc.getLocation().x - 4, 0);
                int xMax = Math.min(rc.getLocation().x + 4, Strategium.enemyHQLocation.x);
                for (int i = xMax; i-- > xMin; ) {
                    MapLocation location = new MapLocation(i, rc.getLocation().y);
                    if (!rc.canSenseLocation(location)) continue;
                    if (rc.senseElevation(location) < waterLevel ||
                            Strategium.occupied[i][rc.getLocation().y]) continue;
                    if(Strategium.nearestEnemyDrone.location.isAdjacentTo(location)) continue;
                    if (!rc.senseFlooding(location)) return suitableAdjacentSpot(location);
                    if(suitableAdjacentSpot(location) != null) return location;
                }
            }

            if(rc.getLocation().x > Strategium.enemyHQLocation.x) {
                int xMin = Math.max(rc.getLocation().x - 4, Strategium.enemyHQLocation.x);
                int xMax = Math.min(rc.getLocation().x + 4, rc.getMapWidth() - 1);
                for (int i = xMin; i++ < xMax; ) {
                    MapLocation location = new MapLocation(i, rc.getLocation().y);
                    if (!rc.canSenseLocation(location)) continue;
                    if (rc.senseElevation(location) < waterLevel ||
                            Strategium.occupied[i][rc.getLocation().y]) continue;
                    if(Strategium.nearestEnemyDrone.location.isAdjacentTo(location)) continue;
                    if (!rc.senseFlooding(location)) return suitableAdjacentSpot(location);
                    if(suitableAdjacentSpot(location) != null) return location;
                }
            }

        }
        if(Math.abs(Strategium.enemyHQLocation.y - rc.getLocation().y) <= 4) {
            if(rc.getLocation().y < Strategium.enemyHQLocation.y) {
                int yMin = Math.max(rc.getLocation().y - 4, 0);
                int yMax = Math.min(rc.getLocation().y + 4, Strategium.enemyHQLocation.y);
                for (int i = yMax; i-- > yMin; ) {
                    MapLocation location = new MapLocation(i, rc.getLocation().y);
                    if (!rc.canSenseLocation(location)) continue;
                    if (rc.senseElevation(location) < waterLevel ||
                            Strategium.occupied[i][rc.getLocation().y]) continue;
                    if(Strategium.nearestEnemyDrone.location.isAdjacentTo(location)) continue;
                    if (!rc.senseFlooding(location)) return suitableAdjacentSpot(location);
                    if(suitableAdjacentSpot(location) != null) return location;
                }
            }

            if(rc.getLocation().y > Strategium.enemyHQLocation.y) {
                int yMin = Math.max(rc.getLocation().x - 4, Strategium.enemyHQLocation.y);
                int yMax = Math.min(rc.getLocation().x + 4, rc.getMapWidth() - 1);
                for (int i = yMin; i++ < yMax; ) {
                    MapLocation location = new MapLocation(i, rc.getLocation().y);
                    if (!rc.canSenseLocation(location)) continue;
                    if (rc.senseElevation(location) < waterLevel ||
                            Strategium.occupied[i][rc.getLocation().y]) continue;
                    if(Strategium.nearestEnemyDrone.location.isAdjacentTo(location)) continue;
                    if (!rc.senseFlooding(location)) return suitableAdjacentSpot(location);
                    if(suitableAdjacentSpot(location) != null) return location;
                }
            }
        }

        return null;

    }

    public static MapLocation suitableAdjacentSpot(MapLocation netGunSite) throws GameActionException {
        for(Direction dir : dir8){
            MapLocation location = netGunSite.add(dir);
            if(Strategium.occupied[location.x][location.y] || !rc.canSenseLocation(location)) continue;
            if((Math.abs(rc.senseElevation(location) -
                    rc.senseElevation(netGunSite)) <= 3 &&
                    rc.senseFlooding(location) == rc.senseFlooding(netGunSite)) ||
                    (Math.abs(rc.senseElevation(location) + 24 -
                            rc.senseElevation(netGunSite)) <= 3 &&
                             !rc.senseFlooding(netGunSite)) ||
                    (Math.abs(rc.senseElevation(location) -
                            rc.senseElevation(netGunSite) - 24) <= 3 &&
                            !rc.senseFlooding(location))) return location;
        }
        return null;
    }

    public static Direction minerDropDir() throws GameActionException {
        System.out.println("DROP CHECK");
        if(Navigation.aerialDistance(Strategium.enemyHQLocation) > 8 || Strategium.enemyDrones.size() < 4) return null;
        if(Strategium.enemyHQLocation == null) return null;
        if(rc.getTeamSoup() < 400) return null;
        for(Direction dir : dir8){
            System.out.println("dir: " + dir);
            MapLocation location = rc.getLocation().add(dir);
            if(!rc.canSenseLocation(location)) continue;
            if(rc.senseFlooding(location)) continue;
            if(location.isAdjacentTo(Strategium.nearestEnemyDrone.location)) continue;

            for(Direction dir2 : dir8){
                MapLocation netGunSite = location.add(dir2);
                if(rc.getLocation().equals(netGunSite)) continue;
                if(!rc.canSenseLocation(netGunSite)) continue;
                if(rc.senseFlooding(netGunSite)) continue;
                if(Math.abs(rc.senseElevation(netGunSite) - rc.senseElevation(location)) <= 3) return dir;
            }
        }
        return null;
    }
}

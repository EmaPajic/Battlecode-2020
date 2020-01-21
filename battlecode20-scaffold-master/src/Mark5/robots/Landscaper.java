package Mark5.robots;

import Mark5.sensors.LandscaperSensor;
import Mark5.utils.*;
import battlecode.common.*;

import static Mark5.RobotPlayer.dir8;
import static Mark5.RobotPlayer.rc;

public class Landscaper {
    private static MapLocation waypoint = null;

    public static void run() throws GameActionException {
        Strategium.gatherInfo();

        if (!rc.isReady()) return;

        if (!Strategium.overlapLocations.contains(rc.getLocation()))
            for (MapLocation location : Strategium.overlapLocations) {
                if (rc.getLocation().isAdjacentTo(location))
                    if (Strategium.canSafelyMove(Navigation.moveTowards(location))) {
                        rc.move(Navigation.moveTowards(location));
                        return;
                    }
            }


        for (Direction dir : dir8) {
            if (rc.adjacentLocation(dir).equals(Strategium.nearestBuriedFriendlyBuilding)) {
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                    return;
                }
            }

            if (rc.adjacentLocation(dir).equals(Strategium.nearestEnemyBuilding)) {
                if (rc.canDepositDirt(dir)) {
                    rc.depositDirt(dir);
                    return;
                }
            }

        }

        if (Strategium.nearestBuriedFriendlyBuilding != null)
            if (defend(Strategium.nearestBuriedFriendlyBuilding)) return;

        if (Strategium.nearestEnemyBuilding != null) {
//            RobotInfo[] robots = rc.senseNearbyRobots();
//            for (RobotInfo robot : robots) {
//                if(robot.team != Strategium.myTeam && robot.type == RobotType.HQ) {
//                    if(attack(robot.location)) return;
//                    if(Navigation.fuzzyNav(robot.location)) return;
//                }
//            }
            // Strategium decides which building should be prioritized, if not in building perimeter
            // try to reach it
            if(attack(Strategium.nearestEnemyBuilding)) return;
        }


        if (rc.getLocation().isAdjacentTo(Strategium.HQLocation) && rc.getRoundNum() > 300) {
            System.out.println("HQ: " + Strategium.HQLocation);
            if (buildTheWall()) return;
        }

        if (LandscaperSensor.combatWaypoint != null) {
                Direction dir = BFS.step(LandscaperSensor.combatWaypoint);
                if (Strategium.canSafelyMove(dir)) {
                    rc.move(dir);
                    return;
                }

        }

        //System.out.println("DREIN");

        if (Strategium.nearestWater != null)
            if (drain(Strategium.nearestWater)) return;

        //System.out.println("PATROL");

        patrol();

    }

    private static boolean buildTheWall() throws GameActionException {
        if(Wall.reposition()) return true;
        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            Direction dir = Lattice.bestDigDirection();
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
                return true;
            }
        }
        Direction dir = rc.getLocation().directionTo(Wall.buildSpot());
        if (rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    private static boolean defend(MapLocation location) throws GameActionException {
        if (rc.getLocation().isAdjacentTo(location)) {

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.maxDeposit(rc.adjacentLocation(dir)) > 0 && !Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDepositDirt(dir)) {
                            rc.depositDirt(dir);
                            return true;
                        }

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDepositDirt(dir)) {
                            rc.depositDirt(dir);
                            return true;
                        }
            return true;

        }
        return false;
    }

    private static boolean attack(MapLocation location) throws GameActionException {
        if (rc.getLocation().isAdjacentTo(location)) {

            if (Strategium.enemyHQLocation != null) {
                if (Navigation.aerialDistance(rc.getLocation(), Strategium.enemyHQLocation) == 1) {
                    if (rc.canDigDirt(Direction.CENTER)) {
                        rc.digDirt(Direction.CENTER);
                        return true;
                    }
                }
            }

            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (Lattice.isPit(rc.adjacentLocation(dir)))
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                            return true;
                        }
            for (Direction dir : Direction.allDirections())
                if (!rc.adjacentLocation(dir).equals(location))
                    if (rc.canDigDirt(dir)) {
                        rc.digDirt(dir);
                        return true;
                    }

            return true;

        }
        return false;
    }

    private static boolean drain(MapLocation location) throws GameActionException {
        int waterLevel = (int) GameConstants.getWaterLevel(rc.getRoundNum() + 100);
        if (!rc.getLocation().isAdjacentTo(location)) return false;
        if (waterLevel - Strategium.elevation[location.x][location.y] < 50) {
            if (rc.canDepositDirt(rc.getLocation().directionTo(location))) {
                rc.depositDirt(rc.getLocation().directionTo(location));
                rc.setIndicatorDot(location, 255, 0, 0);
                return true;
            }
            Direction dir = Lattice.bestDigDirection();
            if (dir == null) return false;
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
                rc.setIndicatorDot(rc.adjacentLocation(dir), 0, 0, 255);
                return true;
            }
            return false;
        }
        return false;
    }

    private static boolean patrol() throws GameActionException {
        if (Lattice.isPit(rc.getLocation()) && waypoint != null) {
            if (Navigation.bugPath(waypoint)) return true;
        }

        int waterLevel = (int) GameConstants.getWaterLevel(
                rc.getRoundNum() + 1000
        );
        if (waterLevel > 25) waterLevel = 25;

        if (rc.getRoundNum() > 2000) waterLevel = 1000;

        if (waterLevel > Strategium.elevation[rc.getLocation().x][rc.getLocation().y] &&
                !Lattice.isPit(rc.getLocation())) {
            if (rc.canDepositDirt(Direction.CENTER)) {
                rc.depositDirt(Direction.CENTER);
                return true;
            }
            Direction dir = Lattice.bestDigDirection();
            if(dir != null)
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
                return true;
            }
        }

        for (Direction dir : dir8) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (Lattice.isPath(location))
                if (waterLevel > Strategium.elevation[location.x][location.y]) {
                    rc.setIndicatorDot(location, 255, 255, 0);
                    if (rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                        return true;
                    }
                }
        }


        for (Direction dir : dir8) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (Lattice.isBuildingSite(location) && !Strategium.occupied[location.x][location.y])
                if (waterLevel > Strategium.elevation[location.x][location.y] ||
                        Lattice.maxDeposit(location) > 0) {
                    if (rc.canDepositDirt(dir)) {

                        rc.depositDirt(dir);
                        return true;
                    }
                }
        }
        if (!Lattice.isPit(rc.getLocation()))
            for (Direction dir : dir8) {
                MapLocation location = rc.adjacentLocation(dir);
                if (!rc.onTheMap(location)) continue;
                if ((Lattice.isPath(location) && !location.isAdjacentTo(Strategium.HQLocation) ||
                        (Lattice.isBuildingSite(location) && !Strategium.occupied[location.x][location.y])) ||
                        Navigation.aerialDistance(location, Strategium.enemyHQLocation) < 3)
                    if (Strategium.elevation[location.x][location.y] >
                            3 + Strategium.elevation[rc.getLocation().x][rc.getLocation().y])
                        if (!Lattice.isAdjacentToWater(location)) {
                            if (rc.canDigDirt(dir)) {

                                rc.digDirt(dir);
                                return true;
                            }
                        }
            }

        if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
            Direction dir = Lattice.bestDepositDirection();
            if(dir != null)
            if (rc.canDepositDirt(dir)) {
                rc.depositDirt(dir);
                return true;
            }
        }

        if (rc.getDirtCarrying() == 0) {
            Direction dir = Lattice.bestDigDirection();
            if(dir != null)
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
                return true;
            }
        }

        MapLocation bestWaypoint = null;
        if (rc.getLocation().equals(waypoint) || Navigation.frustration > 20) waypoint = null;

        for (Direction dir : dir8) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (Lattice.isPath(location))
                if (!Lattice.isEven(location, waterLevel)) {
                    if (rc.canMove(dir)) {
                        if (Strategium.HQLocation != null) {
                            if (Navigation.aerialDistance(Strategium.HQLocation, bestWaypoint) >=
                                    Navigation.aerialDistance(Strategium.HQLocation, location))
                                bestWaypoint = location;
                        } else {
                            rc.move(dir);
                            return true;
                        }

                    }
                }
        }

        waypoint = LandscaperSensor.combatWaypoint;

        if (rc.canSenseLocation(Strategium.HQLocation))
            waypoint = Wall.freeSpot();

        if (waypoint == null) waypoint = bestWaypoint;


        if (waypoint == null) {
            waypoint = Strategium.currentEnemyHQTarget;
        }

        if (waypoint == null) {
            waypoint = new MapLocation(
                    Strategium.rand.nextInt(rc.getMapWidth() / 2) * 2 + 1,
                    Strategium.rand.nextInt(rc.getMapHeight() / 2) * 2 + 1);
        }

        rc.setIndicatorLine(rc.getLocation(), waypoint, 255, 255, 255);
        return Navigation.bugPath(waypoint);

    }

}

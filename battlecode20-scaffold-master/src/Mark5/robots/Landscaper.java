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

        if (rc.getRoundNum() > 1497 && rc.getRoundNum() < 1500) {
            if (Navigation.aerialDistance(Strategium.enemyHQLocation) <= 5 &&
                    Navigation.aerialDistance(Strategium.enemyHQLocation) > 2) {
                rc.disintegrate();
            }
            if (Navigation.aerialDistance(Strategium.enemyHQLocation) == 2 &&
                    rc.senseElevation(rc.getLocation()) < 300) {
                rc.disintegrate();
            }
        }


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
            if (attack(Strategium.nearestEnemyBuilding)) return;
        }

        if (LandscaperSensor.numLandscapersMetWithLowerID < 3 &&
                rc.getRoundNum() > 300 && rc.getRoundNum() < 400 && !rc.canSenseLocation(Strategium.HQLocation)) {
            Navigation.fuzzyNav(Strategium.HQLocation);
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

        if (Strategium.nearestEnemyDrone != null &&
                LandscaperSensor.nearestNetGun.distanceSquaredTo(rc.getLocation()) >= 8)
            if (Navigation.fleeToSafety(Strategium.nearestEnemyDrone.location, LandscaperSensor.nearestNetGun)) return;

        //System.out.println("DREIN");

        if (Strategium.nearestWater != null)
            if (drain(Strategium.nearestWater)) return;

        System.out.println("PATROL");

        patrol();

    }

    private static boolean buildTheWall() throws GameActionException {
        if (Wall.reposition()) return true;
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
                if (Navigation.aerialDistance(rc.getLocation(), Strategium.enemyHQLocation) == 2 &&
                        Math.abs(rc.senseElevation(rc.getLocation().add(Navigation.moveTowards(Strategium.enemyHQLocation))) -
                                rc.senseElevation(rc.getLocation())) > 3) {
                    if (rc.canDigDirt(Navigation.moveTowards(Strategium.enemyHQLocation))) {
                        rc.digDirt(Navigation.moveTowards(Strategium.enemyHQLocation));
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
            //Direction dir = Lattice.bestDigDirection();
            for (Direction dir : dir8) {
                MapLocation digLocation = rc.adjacentLocation(dir);
                if (digLocation.equals(location)) continue;
                if (Lattice.isPit(digLocation) && (!Lattice.isAdjacentToWater(digLocation) ||
                        rc.senseFlooding(digLocation)))
                    if (rc.canDigDirt(dir)) {
                        rc.digDirt(dir);
                        rc.setIndicatorDot(digLocation, 0, 0, 255);
                        return true;
                    }
            }

            for (Direction dir : dir8) {
                MapLocation digLocation = rc.adjacentLocation(dir);
                if (digLocation.equals(location)) continue;
                if (!Lattice.isAdjacentToWater(digLocation))
                    if (rc.canDigDirt(dir)) {
                        rc.digDirt(dir);
                        rc.setIndicatorDot(digLocation, 0, 0, 255);
                        return true;
                    }
            }

            return Navigation.bugPath(Strategium.HQLocation);

        }
        return false;
    }

    private static void patrol() throws GameActionException {
        if (Lattice.isPit(rc.getLocation()) && waypoint != null) {
            if (Navigation.bugPath(waypoint)) return;
        }

        /*int waterLevel = (int) GameConstants.getWaterLevel(
                rc.getRoundNum() + 1000
        );
        if (waterLevel > 25) waterLevel = 25;

        if (rc.getRoundNum() > 2000) waterLevel = 1000;*/
        System.out.println("Kopajjj ");
        int waterLevel = 8;
        if (rc.getRoundNum() > 1600) waterLevel = 10000;
        else if (rc.getRoundNum() < 300) waterLevel = 5;

        if (waterLevel > Strategium.elevation[rc.getLocation().x][rc.getLocation().y] &&
                !Lattice.isPit(rc.getLocation())) {
            Direction dir = Lattice.bestDigDirection();
            if (dir != null)
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                    return;
                }
            if (rc.canDepositDirt(Direction.CENTER)) {
                rc.depositDirt(Direction.CENTER);
                return;
            }
        }


        if (rc.getRoundNum() > 1000 && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit - 1) {
            System.out.println("treba da kopam");
            if (Strategium.enemyHQLocation != null) {
                if (Navigation.aerialDistance(Strategium.enemyHQLocation) > 5) {
                    //System.out.println("Cmoncmon kopaj");
                    Direction dir = Lattice.bestDigDirection();
                    if (dir != null)
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                            return;
                        }
                }
            } else {
                //System.out.println("Cmoncmon kopaj");
                Direction dir = Lattice.bestDigDirection();
                if (dir != null)
                    if (rc.canDigDirt(dir)) {
                        rc.digDirt(dir);
                        return;
                    }
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
                        return;
                    }
                }
        }


        for (Direction dir : dir8) {
            MapLocation location = rc.adjacentLocation(dir);
            if (!rc.onTheMap(location)) continue;
            if (Lattice.isBuildingSite(location) && !Strategium.occupied[location.x][location.y])
                if (waterLevel > Strategium.elevation[location.x][location.y]) {
                    if (rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                        return;
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
                            3 + Strategium.elevation[rc.getLocation().x][rc.getLocation().y]) {
                        if (rc.canDigDirt(dir)) {

                            rc.digDirt(dir);
                            return;
                        }
                    }
            }

        if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
            Direction dir = Lattice.bestDepositDirection();
            if (dir != null)
                if (rc.canDepositDirt(dir)) {
                    rc.depositDirt(dir);
                    return;
                }
        }

        if (rc.getDirtCarrying() == 0) {
            Direction dir = Lattice.bestDigDirection();
            if (dir != null)
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                    return;
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
                            return;
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
        Navigation.bugPath(waypoint);

    }

}

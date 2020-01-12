package Mark2.utils;

import Mark2.utils.Navigation;
import Mark2.utils.Strategium;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import java.util.ArrayList;

import static Mark2.RobotPlayer.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class OneMinerController{
    enum Activty{
        ROAMING, MINING, AVODING;
    }
    
    static ArrayList<MapLocation> searchRoute;
    static Activity currentActivity;
    static int currentTargetIndex;
    static MapLocation currTarget;
    static MapLocation refTarget;
    static int totalSoup;
    
    MapLocation xCurrBordMax;
    MapLocation xCurrBordMin;
    MapLocation yCurrBordMax;
    MapLocation yCurrBordMin;
    int turnsToReturn;
    int turnsToRefine;
    
    final int MINER_COST = 150;
    final int SOUP_PER_TURN = 7; // soup that miner can extract per turn
    boolean alreadyMining;

    static void findRoute(MapLocation rcLoc){
        
        this.xCurrBordMax = rcLoc.x + 5;
        this.xCurrBordMin = rcLoc.x - 5;
        this.yCurrBordMax = rcLoc.y + 5;
        this.yCurrBordMin = rcLoc.y - 5;

        for(int currX = max(0, xCurrBordMin); currX <= min(xCurrBordMax, rc.getMapWidth() - 1);currX++){
            if(currX % 5 == 0){
                for(int currY = max(0,yCurrBordMin); currY <= min(yCurrBordMax, rc.getMapHeight); currY++){
                    if(currY % 5 == 0){
                        searchRoute.add(new MapLocation(currX, currY));
                    }
                
                }
            }
    } 
    }

    public static void init(){
        currActivity = Activity.ROAMING;
        refTarget = hq.getLocation();
        findRoute();
        currentTargetIndex = 0;
        totalSoup = 0;
        turnsToReturn = 0;
        alreadyMining = false;
        turnsToRefine = 5;
        currTarget = rc.getLocation();

    }
    
    public static void control() throws  GameActionException{
        MapLocation rcLoc = rc.getLocation();
        
        if(!searchRoute.isEmpty() && currTarget != null){
            int minDist = Navigation.aerialDistance(rcLoc, searchRoute.get(0));
            currTarget = searchRoute[0];
            for(MapLocation loc : searchRoute){
                int tmpMinDist = Navigation.aerialDistance(rcLoc, loc);
                if(minDist < tmpMinDist){
                    currTarget = loc;
                } 
            }
            searchRoute.remove(currTarget);
        }
        if(currTarget != rcLoc){
            if(rc.getSoupCarrying() < RobotType.MINER.soupLimit){
                for(Direction dir : dir8){
                    this.alreadyMining = rc.canMineSoup(dir);                                            
                    if(this.alreadyMining){
                        tryMine(dir);    
                    
                        MapLocation loc = new MapLocation(rcLoc + dir.getDeltaX, rcLoc.y + dir.getDeltaY);
                        totalSoup += rc.senseSoup(loc);
                        
                    }
                    return;
                }
            }else{
                if(currActivity == ROAMING){
                    
                    if(calcRefineryRentability(rcLoc)){
                        for(Direction dir2 :dir8){
                            if(tryBuild(RobotType.REFINERY, dir2)){                                
                                refineryList.add(MapLocation(rcLoc.x + dir2.getDeltaX, rcLoc.y + dir2.getDeltaY);
                                totalSoup = 0; // should be made memorized for patch 5x5 or more patches?
                            }
                        }
                    }else{
                        currActivity = REFINING;
                    }
                }else{
                    if(this.refining)
                    else{
                        Navigation.bugPath(refTarget); // default for refTarget is HQ, but in calcRefineryRentability can be changed to nearest refinery
                    }
                        for(Direction dir : dir8){
                            if(rcLoc.x + dir.getDeltaX == refTarget.x && rcLoc.y + dir.getDeltaY == refTarget.y){
                                tryRefine(dir);
                                this.refining = true;
                                this.turnsToReturn = 0;
                            }
                    }
                    
                }
                
            }                   
            if(!this.alreadyMining){
                Navigation.bugPath(currTarget);
                this.turnsToReturn += 1;
            }
            
        
        
       
            
        }else{
            //pastTarget = currTarget;
            findRoute(rcLoc);
        }
                
        
        
        
        
        
    }
    public static boolean calcRefineryRentability(MapLocation rcLoc){  // change of refTarget should be added and turns to return with additional variable to refinery when created 
        for(MapLocation loc : refineryList){
            if(Navigation.aerialDistanceTo(refTarget, rcLoc) > Navigation.aerialDistanceTo(loc, rcLoc)){
                refTarget = loc;
            }
        }
        return totalSoup - (this.turnsToReturn - this.turnsToRefine)*SOUP_PER_TURN > MINER_COST ? true : false; //if this does not pays one miner, don't create Refinery
    }
}



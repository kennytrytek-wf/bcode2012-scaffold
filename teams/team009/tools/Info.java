package team009.tools;

import java.lang.Class;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Info {
    public static int round;
    public static MapLocation[] allNodes;
    public static MapLocation enemyCore;
    public static MapLocation myCore;
    public static Team myTeam;
    public static Team opponent;

    public Info(RobotController rc) throws GameActionException {
        this.round = Clock.getRoundNum() - 1;
        this.allNodes = rc.senseCapturablePowerNodes();
        this.enemyCore = null;
        this.myCore = rc.sensePowerCore().getLocation();
        this.myTeam = rc.getTeam();
        this.opponent = this.myTeam.opponent();
    }

    public void update(RobotController rc) {
        this.round += 1;
        this.allNodes = rc.senseCapturablePowerNodes();
    }

    public static int distance(MapLocation start, MapLocation end) {
        int x1 = start.x;
        int x2 = end.x;
        int y1 = start.y;
        int y2 = end.y;
        return ((int) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)));
    }

    public MapLocation getClosest(MapLocation myLoc, MapLocation[] locs) {
        int minDistance = 999999;
        MapLocation minLoc = null;
        for (int i=0; i < locs.length; i++) {
            int distance = Info.distance(myLoc, locs[i]);
            if (distance < minDistance) {
                minDistance = distance;
                minLoc = locs[i];
            }
        }
        return minLoc;
    }

    public MapLocation senseNearestRobot(RobotController rc, MapLocation myLoc, RobotType robotType, Team team) throws GameActionException {
        Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
        int minDistance = 999999;
        MapLocation minLoc = null;
        for (int i=0; i < robotsAround.length; i++) {
            Robot r = robotsAround[i];
            if (r.getTeam() == team) {
                RobotInfo rInfo = rc.senseRobotInfo(r);
                if (robotType != null && rInfo.type != robotType) {
                    continue;
                }
                int rDist = Info.distance(myLoc, rInfo.location);
                if (rDist < minDistance) {
                    minDistance = rDist;
                    minLoc = rInfo.location;
                }
            }
        }
        return minLoc;
    }
}

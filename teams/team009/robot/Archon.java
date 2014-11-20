package team009.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import team009.interfaces.Manager;
import team009.tools.Info;
import team009.tools.Move;

public class Archon extends Manager {
    Info info;
    Direction myDir;
    MapLocation myLoc;
    MapLocation nextLoc;

    public Archon(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myDir = null;
        this.myLoc = null;
        this.nextLoc = null;
    }

    public void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.myDir = rc.getDirection();
        this.myLoc = rc.getLocation();
        this.nextLoc = this.myLoc.add(this.myDir);
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isMovementActive()) {
            return;
        }
        if (!transferFlux(rc)) {
            return;
        }
        if (this.spawn(rc)) {
            return;
        }
        if (rc.getFlux() < rc.getType().moveCost * 10.0) {
            return;
        }
        if (this.moveHome(rc)) {
            return;
        }
        if (this.moveToCapturePoint(rc)) {
            return;
        }
    }

    private boolean transferFlux(RobotController rc) throws GameActionException {
        double totalFlux = rc.getFlux();
        Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
        for (int i=0; i < robotsAround.length; i++) {
            Robot r = robotsAround[i];
            if (r.getTeam() != this.info.myTeam) {
                continue;
            }
            MapLocation rLoc = rc.senseLocationOf(r);
            if (Info.distance(this.myLoc, rLoc) != 1) {
                continue;
            }
            RobotInfo rInfo = rc.senseRobotInfo(r);
            if (rInfo.type == RobotType.TOWER) {
                continue;
            }
            if (rInfo.flux < 10) {
                rc.setIndicatorString(0, "Wanted flux: " + rInfo.flux + "; My total flux: " + totalFlux + "; Transfer to : " + rLoc);
                if (totalFlux < 20) {
                    return false;
                }
                rc.transferFlux(rLoc, r.getRobotLevel(), 20.0);
                totalFlux -= 20;
            }
        }
        return true;
    }

    private boolean spawn(RobotController rc) throws GameActionException {
        RobotType spawnMe = this.getSpawnType(rc);
        if (spawnMe != null) {
            rc.spawn(spawnMe);
            return true;
        }
        return false;
    }

    private RobotType getSpawnType(RobotController rc) throws GameActionException {
        if (rc.getFlux() > RobotType.SOLDIER.spawnCost) {
            if (Move.canMove(rc, this.myDir)) {
                return RobotType.SOLDIER;
            }
        }
        return null;
    }

    private boolean moveHome(RobotController rc) throws GameActionException {
        return Move.moveTo(rc, this.myLoc, this.info.myCore, this.myDir);
    }

    private boolean moveToCapturePoint(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.getClosest(this.myLoc, this.info.allNodes);
        return Move.moveTo(rc, this.myLoc, nearest, this.myDir);
    }
}

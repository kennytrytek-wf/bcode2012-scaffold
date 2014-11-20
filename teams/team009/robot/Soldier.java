package team009.robot;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

import team009.interfaces.Manager;
import team009.tools.Info;
import team009.tools.Move;

public class Soldier extends Manager {
    Info info;
    Direction myDir;
    MapLocation myLoc;
    MapLocation nextLoc;

    public Soldier(RobotController rc) throws GameActionException {
        rc.setIndicatorString(0, "Initializing...");
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
        if (rc.isMovementActive() || rc.isAttackActive()) {
            return;
        }
        if (this.attack(rc)) {
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

    private boolean moveToCapturePoint(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.getClosest(this.myLoc, this.info.allNodes);
        return Move.moveTo(rc, this.myLoc, nearest, this.myDir);
    }

    private boolean moveHome(RobotController rc) throws GameActionException {
        return Move.moveTo(rc, this.myLoc, this.info.myCore, this.myDir);
    }

    private boolean attack(RobotController rc) throws GameActionException {
        if (rc.getFlux() < 10) {
            return false;
        }
        Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
        int minDistance = 999999;
        MapLocation minLoc = null;
        RobotLevel minLevel = null;
        for (int i=0; i < robotsAround.length; i++) {
            Robot r = robotsAround[i];
            if (r.getTeam() != this.info.myTeam) {
                MapLocation rLoc = rc.senseLocationOf(r);
                int rDist = Info.distance(this.myLoc, rLoc);
                if (rDist < minDistance) {
                    minDistance = rDist;
                    minLoc = rLoc;
                    minLevel = r.getRobotLevel();
                }
            }
        }
        if (minLoc == null) {
            return false;
        }
        if (rc.canAttackSquare(minLoc)) {
            rc.attackSquare(minLoc, minLevel);
        } else {
            Move.moveTo(rc, this.myLoc, minLoc, this.myDir);
        }
        return true;
    }
}

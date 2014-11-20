package team009.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
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
        rc.setIndicatorString(0, "Updating...");
        this.info.update(rc);
        this.myDir = rc.getDirection();
        this.myLoc = rc.getLocation();
        this.nextLoc = this.myLoc.add(this.myDir);
    }

    public void move(RobotController rc) throws GameActionException {
        rc.setIndicatorString(0, "move");
        if (rc.isMovementActive()) {
            rc.setIndicatorString(0, "Can't move.");
            return;
        }
        if (this.moveToCapturePoint(rc)) {
            rc.setIndicatorString(0, "Moving...");
            return;
        }
        rc.setIndicatorString(0, "Failed to do anything.");
    }

    private boolean moveToCapturePoint(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.getClosest(this.myLoc, this.info.allNodes);
        return Move.moveTo(rc, this.myLoc, nearest, this.myDir);
    }
}

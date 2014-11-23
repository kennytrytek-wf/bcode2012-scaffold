package team009.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;

import team009.interfaces.Manager;
import team009.tools.Info;
import team009.tools.Move;

public class Scout extends Manager {
    Info info;
    Direction myDir;
    MapLocation myLoc;
    MapLocation nextLoc;

    public Scout(RobotController rc) throws GameActionException {
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
        if (this.regenerate(rc)) {
            return;
        }
        if (this.followArchon(rc)) {
            return;
        }
        if (this.moveToCapturePoint(rc)) {
            return;
        }
        if (this.moveHome(rc)) {
            return;
        }
    }

    private boolean followArchon(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.senseNearestRobot(rc, this.myLoc, RobotType.ARCHON, this.info.myTeam);
        if (nearest != null) {
            int followDistance = 2;
            if (rc.getFlux() < 10) {
                followDistance = 0;
            }
            return Move.moveTo(rc, this.myLoc, nearest, this.myDir, followDistance);
        }
        return false;
    }

    private boolean moveToCapturePoint(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.getClosest(this.myLoc, this.info.allNodes);
        return Move.moveTo(rc, this.myLoc, nearest, this.myDir);
    }

    private boolean moveHome(RobotController rc) throws GameActionException {
        return Move.moveTo(rc, this.myLoc, this.info.myCore, this.myDir);
    }

    private boolean regenerate(RobotController rc) throws GameActionException {
        if (rc.getFlux() < 10) {
            return false;
        }
        Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
        double minEnergonPercent = 0.95;
        MapLocation minLoc = null;
        RobotLevel minLevel = null;
        for (int i=0; i < robotsAround.length; i++) {
            Robot r = robotsAround[i];
            if (r.getTeam() == this.info.myTeam) {
                RobotInfo rInfo = rc.senseRobotInfo(r);
                double rEnergon = rInfo.energon;
                double energonPercent = rEnergon / rInfo.type.maxEnergon;
                if (energonPercent < minEnergonPercent) {
                    minEnergonPercent = minEnergonPercent;
                    minLoc = rInfo.location;
                    minLevel = r.getRobotLevel();
                }
            }
        }
        if (minLoc == null) {
            return false;
        }
        if (Info.distance(this.myLoc, minLoc) <= 2) {
            rc.regenerate();
        } else {
            Move.moveTo(rc, this.myLoc, minLoc, this.myDir, 0);
        }
        return true;
    }
}

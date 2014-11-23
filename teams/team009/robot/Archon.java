package team009.robot;

import java.util.Arrays;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

import team009.common.MessageTypes;
import team009.interfaces.Manager;
import team009.tools.Info;
import team009.tools.Move;

public class Archon extends Manager {
    Info info;
    Direction myDir;
    MapLocation myLoc;
    MapLocation nextLoc;
    MapLocation[] previousLocs;
    MapLocation capPoint;
    Message[] messages;
    boolean stuck;
    Direction stuckDir;

    public Archon(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myDir = null;
        this.myLoc = null;
        this.nextLoc = null;
        this.previousLocs = new MapLocation[5];
        this.capPoint = this.initCapturePoint(rc);
        this.messages = new Message[0];
        this.stuck = false;
        this.stuckDir = null;
    }

    public void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.previousLocs = this.getPreviousLocs(rc);
        this.myDir = rc.getDirection();
        this.myLoc = rc.getLocation();
        this.nextLoc = this.myLoc.add(this.myDir);
        rc.setIndicatorString(2, "Next Loc: " + this.nextLoc);
        this.messages = rc.getAllMessages();
        this.capPoint = this.getCapturePoint(rc);
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isMovementActive()) {
            return;
        }
        if (!transferFlux(rc)) {
            return;
        }
        if (this.getUnstuck(rc)) {
            return;
        }
        if (this.capture(rc)) {
            return;
        }
        if (this.spawn(rc)) {
            return;
        }
        if (rc.getFlux() < rc.getType().moveCost * 10.0) {
            return;
        }
        if (this.moveToCapturePoint(rc)) {
            return;
        }
        if (this.moveHome(rc)) {
            return;
        }
    }

    private MapLocation initCapturePoint(RobotController rc) throws GameActionException {
        Message[] messages = rc.getAllMessages();
        int capPointIndex = 0;
        for (int i=0; i < messages.length; i++) {
            if (messages[i].ints[0] == MessageTypes.ARCHON_CAPTURE_POINT) {
                capPointIndex += 1;
            }
        }
        Message msg = new Message();
        msg.ints = new int[]{MessageTypes.ARCHON_CAPTURE_POINT};
        rc.broadcast(msg);
        return this.info.sortedNodes[capPointIndex % this.info.sortedNodes.length];
    }

    private MapLocation getCapturePoint(RobotController rc) throws GameActionException {
        if (Arrays.asList(this.info.allNodes).indexOf(this.capPoint) >= 0) {
            return this.capPoint;
        }
        return this.info.getClosest(this.myLoc, this.info.allNodes);
    }

    private MapLocation[] getPreviousLocs(RobotController rc) throws GameActionException {
        MapLocation[] newPreviousLocs = new MapLocation[5];
        for (int i=0; i < 4; i++) {
            newPreviousLocs[i + 1] = this.previousLocs[i];
        }
        newPreviousLocs[0] = this.myLoc;
        return newPreviousLocs;
    }

    private boolean getUnstuck(RobotController rc) throws GameActionException {
        rc.setIndicatorString(0, "Stuck: " + this.stuck);
        if (!this.stuck) {
            this.stuck = rc.senseTerrainTile(this.nextLoc) != TerrainTile.LAND;
            if (!this.stuck) {
                rc.setIndicatorString(1, "Still not stuck.");
                return false;
            }
            this.stuckDir = this.myDir;
            Move.setDirection(rc, this.myDir.rotateLeft());
            rc.setIndicatorString(1, "I'm stuck! Stuck dir: " + this.stuckDir + ". New dir: " + this.myDir.rotateLeft());
            return true;
        }
        //if facing stuckDir, unstick
        if (this.myDir == this.stuckDir) {
            rc.setIndicatorString(1, "No longer stuck! Facing " + this.myDir + ", which is the same as " + this.stuckDir);
            this.stuck = false;
            this.stuckDir = null;
            return false;
        }
        //check the right for a wall. If can move right, do it
        Direction rightDir = this.myDir.rotateRight();
        if (rc.senseTerrainTile(this.myLoc.add(rightDir)) == TerrainTile.LAND) {
            if (Move.canMove(rc, rightDir)) {
                rc.setIndicatorString(1, "Nothing on the right. Setting dir to " + rightDir + ". Stuck dir: " + this.stuckDir);
                Move.setDirection(rc, rightDir);
            } else {
                rc.setIndicatorString(1, "Nothing on right, but can't move. Waiting...");
            }
            return true;
        }
        //move straight ahead. If can't move, turn left.
        if (rc.senseTerrainTile(this.nextLoc) == TerrainTile.LAND) {
            if (Move.canMove(rc, this.myDir)) {
                rc.setIndicatorString(1, "Can move straight ahead. Moving...");
                rc.moveForward();
            } else {
                rc.setIndicatorString(1, "Nothing straight ahead, but can't move. Waiting...");
            }
            return true;
        } else {
            rc.setIndicatorString(1, "Can't move ahead. Setting dir to " + this.myDir.rotateLeft() + ". Stuck dir: " + this.stuckDir);
            Move.setDirection(rc, this.myDir.rotateLeft());
            return true;
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
            if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
                continue;
            }
            if (rInfo.flux < 10) {
                //rc.setIndicatorString(0, "Wanted flux: " + rInfo.flux + "; My total flux: " + totalFlux + "; Transfer to : " + rLoc);
                if (totalFlux < 20) {
                    return false;
                }
                rc.transferFlux(rLoc, r.getRobotLevel(), 20.0);
                totalFlux -= 20;
            }
        }
        return true;
    }

    private MapLocation[] plotCourseAroundWalls(RobotController rc) throws GameActionException {
        if (this.previousLocs.length == 5) {
            MapLocation[] backtrack = {
                this.previousLocs[4],
                this.previousLocs[3],
                this.info.myCore
            };
            return backtrack;
        }
        MapLocation[] backtrack = {this.info.myCore};
        return backtrack;
    }

    private boolean spawn(RobotController rc) throws GameActionException {
        return this.spawn(rc, RobotType.SOLDIER);
    }

    private boolean spawn(RobotController rc, RobotType type) throws GameActionException {
        if (rc.getFlux() > type.spawnCost) {
            if (Move.canMove(rc, this.myDir)) {
                rc.spawn(type);
                return true;
            }
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
        return Move.moveTo(rc, this.myLoc, this.capPoint, this.myDir);
    }

    private boolean capture(RobotController rc) throws GameActionException {
        if (Info.distance(this.myLoc, this.capPoint) == 1) {
            Direction toCapPoint = this.myLoc.directionTo(this.capPoint);
            if (this.myDir == toCapPoint) {
                this.spawn(rc, RobotType.TOWER);
            } else {
                Move.setDirection(rc, toCapPoint);
            }
            return true;
        }
        return this.moveToCapturePoint(rc);
    }
}

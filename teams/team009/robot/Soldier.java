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

import team009.common.MessageTypes;
import team009.interfaces.Manager;
import team009.tools.Info;
import team009.tools.Move;

public class Soldier extends Manager {
    Info info;
    Direction myDir;
    MapLocation myLoc;
    MapLocation nextLoc;
    MapLocation previousLoc;
    int previousLocCount;
    Message[] messages;
    MapLocation[] archonLocs;


    public Soldier(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myDir = null;
        this.myLoc = null;
        this.nextLoc = null;
        this.previousLoc = null;
        this.previousLocCount = 0;
        this.messages = null;
        this.archonLocs = new MapLocation[0];
    }

    public void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.myDir = rc.getDirection();
        this.previousLocCount = (this.myLoc == this.previousLoc) ? this.previousLocCount + 1 : 0;
        this.previousLoc = this.myLoc;
        this.myLoc = rc.getLocation();
        this.nextLoc = this.myLoc.add(this.myDir);
        this.messages = rc.getAllMessages();
        this.archonLocs = this.updateArchonLocs(rc);
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isMovementActive() || rc.isAttackActive()) {
            return;
        }
        if (this.attack(rc)) {
            return;
        }
        if (rc.getFlux() < RobotType.SOLDIER.moveCost) {
            return;
        }
        if (this.getOutTheWay(rc)) {
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

    private MapLocation[] updateArchonLocs(RobotController rc) throws GameActionException {
        MapLocation nearest = this.info.senseNearestRobot(rc, this.myLoc, new RobotType[]{RobotType.ARCHON}, this.info.myTeam);
        if ((nearest != null) && (Arrays.asList(this.archonLocs).indexOf(nearest) != 0)) {
            if (this.archonLocs.length < 3) {
                return new MapLocation[]{nearest, nearest, nearest};
            }
            return new MapLocation[]{nearest, this.archonLocs[0], this.archonLocs[1]};
        }
        return this.archonLocs;
    }

    private boolean followArchon(RobotController rc) throws GameActionException {
        if (Arrays.asList(this.info.allNodes).indexOf(this.myLoc) >= 0) {
            Direction somewhereElse = Move.getSpawnDirection(rc, this.myDir);
            if (somewhereElse != null) {
                return Move.moveTo(rc, this.myLoc, this.myLoc.add(somewhereElse), this.myDir, 0);
            }
        }
        MapLocation nearest = this.info.senseNearestRobot(rc, this.myLoc, new RobotType[]{RobotType.ARCHON}, this.info.myTeam);
        if (nearest != null) {
            return Move.moveTo(rc, this.myLoc, nearest, this.myDir, 0);
        }
        return false;
    }

    private boolean getOutTheWay(RobotController rc) throws GameActionException {
        if (this.previousLocCount > 50) {
            Direction spawnDirection = Move.getSpawnDirection(rc, this.myDir);
            if (spawnDirection == this.myDir) {
                rc.moveForward();
                return true;
            } else if (spawnDirection != null) {
                Move.setDirection(rc, spawnDirection);
                return true;
            }
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

    private boolean attack(RobotController rc) throws GameActionException {
        MapLocation dangerLoc = null;
        for (int i=0; i < this.messages.length; i++) {
            Message msg = this.messages[i];
            if (MessageTypes.getMessageType(msg) == MessageTypes.ENEMY) {
                dangerLoc = msg.locations[0];
                break;
            }
        }
        if ((dangerLoc == null) && (rc.getFlux() < 10)) {
            return false;
        }
        GameObject dangerBot = null;
        if (dangerLoc != null) {
            dangerBot = rc.canSenseSquare(dangerLoc) ? rc.senseObjectAtLocation(dangerLoc, RobotLevel.ON_GROUND) : null;
        }
        MapLocation minLoc = dangerLoc;
        RobotLevel minLevel = RobotLevel.ON_GROUND;
        boolean isArchon = false;
        if (dangerBot == null) {
            Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
            int minDistance = 999999;
            for (int i=0; i < robotsAround.length; i++) {
                Robot r = robotsAround[i];
                if (r.getTeam() != this.info.myTeam) {
                    RobotInfo rInfo = rc.senseRobotInfo(r);
                    if (rInfo.type == RobotType.ARCHON) {
                        isArchon = true;
                    } else if (rInfo.type == RobotType.TOWER) {
                        if (Arrays.asList(this.info.allNodes).indexOf(rInfo.location) < 0) {
                            continue;
                        }
                    }
                    if (!isArchon || (isArchon && rInfo.type == RobotType.ARCHON)) {
                        int rDist = Info.distance(this.myLoc, rInfo.location);
                        if (rDist < minDistance) {
                            minDistance = rDist;
                            minLoc = rInfo.location;
                            minLevel = r.getRobotLevel();
                        }
                    }
                }
            }
        }
        if (minLoc == null) {
            return false;
        }
        if (rc.getFlux() < RobotType.SOLDIER.moveCost) {
            return true;
        }
        if (rc.canAttackSquare(minLoc)) {
            Message msg = MessageTypes.createMessage(MessageTypes.ENEMY, new int[0], new MapLocation[]{minLoc}, null);
            rc.broadcast(msg);
            rc.attackSquare(minLoc, minLevel);
        } else {
            Move.moveTo(rc, this.myLoc, minLoc, this.myDir);
        }
        return true;
    }
}

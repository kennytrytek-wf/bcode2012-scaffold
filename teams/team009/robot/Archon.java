package team009.robot;

import java.util.Arrays;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.PowerNode;
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
    MapLocation previousLoc;
    int previousLocCount;
    MapLocation capPoint;
    Message[] messages;
    boolean stuck;
    Direction stuckDir;
    boolean stuckTurnLeft;
    int stuckRounds;
    Random random;
    int archonNumber;

    public Archon(RobotController rc) throws GameActionException {
        this.info = new Info(rc);
        this.myDir = null;
        this.myLoc = null;
        this.nextLoc = null;
        this.previousLoc = null;
        this.previousLocCount = 0;
        this.capPoint = this.initCapturePoint(rc);
        this.messages = new Message[0];
        this.stuck = false;
        this.stuckDir = null;
        this.stuckTurnLeft = true;
        this.stuckRounds = 0;
        this.random = new Random(rc.getRobot().getID());
    }

    public void update(RobotController rc) throws GameActionException {
        this.info.update(rc);
        this.myDir = rc.getDirection();
        this.myLoc = rc.getLocation();
        this.nextLoc = this.myLoc.add(this.myDir);
        this.updatePreviousLocCount();
        this.messages = rc.getAllMessages();
        this.capPoint = this.getCapturePoint(rc);
        this.stuckRounds += this.stuck ? 1 : 0;
    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isMovementActive()) {
            return;
        }
        this.transferFlux(rc);
        if (this.danger(rc)) {
            return;
        }
        if (this.getUnstuck(rc)) {
            return;
        }
        if (this.buildEntourage(rc)) {
            return;
        }
        if (this.capture(rc)) {
            return;
        }
    }

    private MapLocation initCapturePoint(RobotController rc) throws GameActionException {
        Message[] messages = rc.getAllMessages();
        int capPointIndex = 0;
        for (int i=0; i < messages.length; i++) {
            if (MessageTypes.getMessageType(messages[i]) == MessageTypes.ARCHON_CAPTURE_POINT) {
                capPointIndex += 1;
            }
        }
        Message msg = MessageTypes.createMessage(MessageTypes.ARCHON_CAPTURE_POINT, new int[0], null, null);
        rc.broadcast(msg);
        this.archonNumber = capPointIndex;
        MapLocation capPoint = this.info.sortedNodes[capPointIndex % this.info.sortedNodes.length];
        return capPoint;
    }

    private void updatePreviousLocCount() {
        if (this.previousLoc == this.myLoc) {
            this.previousLocCount += 1;
        } else {
            this.previousLoc = this.myLoc;
            this.previousLocCount = 0;
        }
    }

    private MapLocation getCapturePoint(RobotController rc) throws GameActionException {
        if (Arrays.asList(this.info.allNodes).indexOf(this.capPoint) >= 0) {
            return this.capPoint;
        }
        PowerNode pc = rc.sensePowerCore();
        MapLocation[] pcNeighbors = pc.neighbors();
        PowerNode[] allied = rc.senseAlliedPowerNodes();
        MapLocation[] alliedLocs = new MapLocation[allied.length];
        for (int i=0; i < allied.length; i++) {
            alliedLocs[i] = allied[i].getLocation();
        }
        for (int i=0; i < pcNeighbors.length; i++) {
            if (Arrays.asList(alliedLocs).indexOf(pcNeighbors[i]) < 0) {
                return pcNeighbors[i];
            }
        }
        MapLocation[] sortedNodes = this.info.sortLocs(rc, this.info.allNodes, this.info.myCore);
        MapLocation capPoint = sortedNodes[this.archonNumber % this.info.allNodes.length];
        return capPoint;
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
            int distLeft = Info.distance(this.capPoint, this.myLoc.add(this.myDir.rotateLeft()));
            int distRight = Info.distance(this.capPoint, this.myLoc.add(this.myDir.rotateRight()));
            if (distLeft < distRight) {
                Move.setDirection(rc, this.myDir.rotateLeft());
                this.stuckTurnLeft = true;
            } else {
                Move.setDirection(rc, this.myDir.rotateRight());
                this.stuckTurnLeft = false;
            }
            rc.setIndicatorString(1, "I'm stuck! Stuck dir: " + this.stuckDir + ". New dir: " + this.myDir.rotateLeft());
            return true;
        }
        //if in the same place for 15 rounds, do something about it
        if (this.previousLocCount >= 15) {
            rc.setIndicatorString(1, "No longer stuck! Stuck in same place for 5 rounds, so try again.");
            this.stuck = false;
            this.stuckDir = null;
            this.stuckRounds = 0;
            return false;
        }
        //if facing stuckDir, unstick
        if ((this.myDir == this.stuckDir) && Move.canMove(rc, this.myDir)) {
            rc.setIndicatorString(1, "No longer stuck! Facing " + this.myDir + ", which is the same as " + this.stuckDir);
            this.stuck = false;
            this.stuckDir = null;
            this.stuckRounds = 0;
            return false;
        }
        //check the right for a wall. If can move right, do it
        Direction dir = this.stuckTurnLeft ? this.myDir.rotateRight() : this.myDir.rotateLeft();
        if (Move.canMove(rc, dir)) {
            rc.setIndicatorString(1, "Nothing on the right. Setting dir to " + dir + ". Stuck dir: " + this.stuckDir);
            Move.setDirection(rc, dir);
            return true;
        }
        //move straight ahead. If can't move, turn left.
        if (Move.canMove(rc, this.myDir)) {
            rc.setIndicatorString(1, "Can move straight ahead. Moving...");
            rc.moveForward();
            return true;
        } else {
            rc.setIndicatorString(1, "Can't move ahead. Setting dir to " + this.myDir.rotateLeft() + ". Stuck dir: " + this.stuckDir);
            Move.setDirection(rc, (this.stuckTurnLeft ? this.myDir.rotateLeft() : this.myDir.rotateRight()));
            return true;
        }
    }

    private boolean buildEntourage(RobotController rc) throws GameActionException {
        if (rc.getFlux() > 299.0) {
            return this.spawn(rc, RobotType.SOLDIER);
        }
        //If getting a high-priority node, just get it
        PowerNode pc = rc.sensePowerCore();
        MapLocation[] pcNeighbors = pc.neighbors();
        if (Arrays.asList(pcNeighbors).indexOf(this.capPoint) >= 0) {
            return false;
        }
        //Check around me for two soldiers and one scout
        int numSoldiers = 0;
        int numScouts = 0;
        Robot[] robotsAround = rc.senseNearbyGameObjects(Robot.class);
        for (int i=0; i < robotsAround.length; i++) {
            Robot r = robotsAround[i];
            if (r.getTeam() == this.info.myTeam) {
                if (rc.canSenseObject(r)) {
                    RobotInfo rInfo = rc.senseRobotInfo(r);
                    if (rInfo.type == RobotType.SOLDIER) {
                        numSoldiers += 1;
                    } else if (rInfo.type == RobotType.SCOUT) {
                        numScouts += 1;
                    }
                }
            }
        }
        //If need something, make it
        if (numSoldiers < 2) {
            return this.spawn(rc, RobotType.SOLDIER);
        } else if (numScouts < 1) {
            return this.spawn(rc, RobotType.SCOUT);
        }
        return false;
    }

    private boolean transferFlux(RobotController rc) throws GameActionException {
        return this.transferFlux(rc, 10.0, 10.0, 35.0, 10.0);
    }

    private boolean transferFlux(RobotController rc, double myMin, double otherMin, double otherMax, double transferAmt) throws GameActionException {
        boolean fluxTransferred = false;
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
            if ((rInfo.flux < otherMin) || (rInfo.flux < otherMax)) {
                double actualTransferAmt = transferAmt;
                if (rInfo.flux > otherMin) {
                    actualTransferAmt = otherMax - rInfo.flux;
                }
                if (totalFlux < actualTransferAmt) {
                    return false;
                }
                rc.transferFlux(rLoc, r.getRobotLevel(), actualTransferAmt);
                totalFlux -= actualTransferAmt;
                fluxTransferred = true;
            }
        }
        return fluxTransferred;
    }

    private boolean danger(RobotController rc) throws GameActionException {
        MapLocation nearest = null;
        for (int i=0; i < this.messages.length; i++) {
            Message msg = this.messages[i];
            if (MessageTypes.getMessageType(msg) == MessageTypes.ENEMY) {
                nearest = msg.locations[0];
                 break;
            }
        }
        RobotType[] scaryRobots = new RobotType[]{RobotType.ARCHON, RobotType.SOLDIER, RobotType.SCORCHER, RobotType.DISRUPTER};
        MapLocation sensedNearest = this.info.senseNearestRobot(rc, this.myLoc, scaryRobots, this.info.opponent);
        if (nearest == null) {
            nearest = sensedNearest;
        }
        MapLocation closestFriend = this.info.senseNearestRobot(rc, this.myLoc, new RobotType[]{RobotType.SOLDIER}, this.info.myTeam);
        if (nearest != null) {
            this.stuck = false;
            this.stuckDir = null;
            this.stuckRounds = 0;
            if (sensedNearest != null) {
                Message msg = MessageTypes.createMessage(MessageTypes.ENEMY, new int[0], new MapLocation[]{sensedNearest}, null);
                rc.broadcast(msg);
            }
            if ((closestFriend == null) || (Info.distance(this.myLoc, nearest) < 3)) {
                Direction runAwayDir = this.myLoc.directionTo(nearest).opposite();
                return Move.moveTo(rc, this.myLoc, this.myLoc.add(runAwayDir), this.myDir, 0);
            } else {
                if ((closestFriend != null) && (Info.distance(this.myLoc, closestFriend) > 1)) {
                    return Move.moveTo(rc, this.myLoc, closestFriend, this.myDir, 1);
                } else if (this.spawn(rc, RobotType.SOLDIER)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean spawn(RobotController rc, RobotType type) throws GameActionException {
        if (rc.getFlux() > type.spawnCost) {
            Direction spawnDirection = Move.getSpawnDirection(rc, this.myDir);
            if (spawnDirection == this.myDir) {
                rc.spawn(type);
                return true;
            } else if (spawnDirection != null) {
                Move.setDirection(rc, spawnDirection);
                return true;
            }
        }
        return false;
    }

    private boolean moveToCapturePoint(RobotController rc) throws GameActionException {
        return Move.moveTo(rc, this.myLoc, this.capPoint, this.myDir);
    }

    private boolean capture(RobotController rc) throws GameActionException {
        if (this.myLoc == this.capPoint) {
            Direction somewhereElse = Move.getSpawnDirection(rc, this.myDir);
            if (somewhereElse != null) {
                return Move.moveTo(rc, this.myLoc, this.myLoc.add(somewhereElse), this.myDir, 0);
            }
        }
        if (Info.distance(this.myLoc, this.capPoint) == 1) {
            GameObject node = rc.senseObjectAtLocation(this.capPoint, RobotLevel.POWER_NODE);
            rc.setIndicatorString(2, "node: " + node + ", team: " + (node == null ? null : node.getTeam()));
            if (node.getTeam() == this.info.opponent) {
                return this.spawn(rc, RobotType.SOLDIER);
            }
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

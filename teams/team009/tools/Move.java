package team009.tools;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Move {
    public static boolean canMove(RobotController rc, Direction d) throws GameActionException {
        if (d == Direction.OMNI || d == Direction.NONE) {
            return false;
        }
        return rc.canMove(d);
    }

    public static boolean setDirection(RobotController rc, Direction d) throws GameActionException {
        if (d == Direction.OMNI || d == Direction.NONE) {
            return false;
        }
        rc.setDirection(d);
        return true;
    }

    public static boolean moveTo(RobotController rc, MapLocation myLoc, MapLocation destination, Direction origDir) throws GameActionException {
        if (Info.distance(myLoc, destination) <= 2) {
            return true;
        }
        Direction desiredDir = myLoc.directionTo(destination);
        if (Move.canMove(rc, desiredDir) && (desiredDir == origDir)) {
            rc.moveForward();
            return true;
        }
        Direction[] dirOptions = {
            desiredDir,
            desiredDir.rotateRight(),
            desiredDir.rotateLeft(),
            desiredDir.rotateRight().rotateRight(),
            desiredDir.rotateLeft().rotateLeft()
        };
        for (int i=0; i < dirOptions.length; i++) {
            Direction newDir = dirOptions[i];
            if (Move.canMove(rc, newDir)) {
                if (newDir == origDir) {
                    rc.moveForward();
                } else {
                    Move.setDirection(rc, newDir);
                }
                return true;
            }
        }
        return false;
    }
}

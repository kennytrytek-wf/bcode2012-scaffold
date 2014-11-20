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
        Direction desiredDir = myLoc.directionTo(destination);
        if (Move.canMove(rc, desiredDir) && (desiredDir == origDir)) {
            rc.moveForward();
            return true;
        }
        Direction newDir = desiredDir.rotateRight();
        while (!Move.canMove(rc, newDir) && !(newDir == desiredDir)) {
            newDir = newDir.rotateRight();
        }
        if (Move.canMove(rc, newDir)) {
            if (newDir == origDir) {
                rc.moveForward();
            } else {
                Move.setDirection(rc, newDir);
            }
            return true;
        }
        return false;
    }
}

package team009.interfaces;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class Manager {
    public abstract void move(RobotController rc) throws GameActionException;
    public abstract void update(RobotController rc) throws GameActionException;
}

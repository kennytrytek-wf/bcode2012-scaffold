package team009.robot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import team009.interfaces.Manager;

public class Soldier extends Manager {
    public Soldier(RobotController rc) {

    }

    public void move(RobotController rc) throws GameActionException {
        if (rc.isMovementActive()) {
        }
    }
}

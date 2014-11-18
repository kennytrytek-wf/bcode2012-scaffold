package team009;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import team009.interfaces.Manager;
import team009.robot.Archon;
import team009.robot.Disrupter;
import team009.robot.Scorcher;
import team009.robot.Scout;
import team009.robot.Soldier;
import team009.robot.Tower;

public class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
	    RobotType type = rc.getType();
        if (type == RobotType.ARCHON) {
            RobotPlayer.move(new Archon(rc), rc);
        } else if (type == RobotType.DISRUPTER) {
            RobotPlayer.move(new Disrupter(rc), rc);
        } else if (type == RobotType.SCORCHER) {
            RobotPlayer.move(new Scorcher(rc), rc);
        } else if (type == RobotType.SCOUT) {
            RobotPlayer.move(new Scout(rc), rc);
        } else if (type == RobotType.SOLDIER) {
            RobotPlayer.move(new Soldier(rc), rc);
        } else if (type == RobotType.TOWER) {
            RobotPlayer.move(new Tower(rc), rc);
    }
}

    private static void move(Manager m, RobotController rc) {
        while(true) {
            try {
                m.move(rc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            rc.yield();
        }
    }
}


package team009.common;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class MessageTypes {
    public static final int ARCHON_CAPTURE_POINT = 0;
    public static final int ENEMY = 1;

    public static Message createMessage(int messageType, int[] ints, MapLocation[] locations, String[] strings) throws GameActionException {
        Message msg = new Message();
        int[] intsCopy = new int[ints.length + 1];
        for (int i=0; i < ints.length; i++) {
            intsCopy[i + 1] = ints[i];
        }
        intsCopy[0] = messageType;
        msg.ints = intsCopy;
        msg.locations = locations;
        msg.strings = strings;
        return msg;
    }

    public static int getMessageType(Message msg) {
        return msg.ints[0];
    }
}

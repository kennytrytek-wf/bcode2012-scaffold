package team009.common;

import battlecode.common.MapLocation;

public class EnumeratedMapLocation {
    public MapLocation loc;
    public int distance;

    public EnumeratedMapLocation(MapLocation loc, int distance) {
        this.loc = loc;
        this.distance = distance;
    }
}

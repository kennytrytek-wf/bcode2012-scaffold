package team009.common;

import java.util.Comparator;

public class EnumeratedMapLocationComparator implements Comparator<EnumeratedMapLocation> {
    public int compare(EnumeratedMapLocation a, EnumeratedMapLocation b) {
        //Return negative if a < b
        if (a.distance < b.distance) {
            return -1;
        }
        //Return zero if a == b
        if (a.distance == b.distance) {
            return 0;
        }
        //Return positive if a > b
        return 1;
    }
}

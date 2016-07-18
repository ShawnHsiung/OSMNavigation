package shawn.thesis.osmnavigation;

import android.location.Location;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public interface WayPointListener {

    void onTargetReached(Location location);
//    void onWaypointPassed(List<GeoPoint> waypointsLeft);
}

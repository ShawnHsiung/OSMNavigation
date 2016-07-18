package shawn.thesis.osmnavigation;

/**
 * Map State Manager
 * to keep parameters
 * Created by Shawn on 2016/4/11.
 */
public interface MapStateManager {

    public static final String PREFS_LONGITUDE = "longitude";
    public static final String PREFS_LATITUDE = "latitude";
    public static final String PREFS_TRACKING = "tracking";
    public static final String PREFS_ZOOM = "zoom";

    public static final String PREFS_COMMAND_RIGHT = "right";
    public static final String PREFS_COMMAND_LEFT = "left";
    public static final String PREFS_COMMAND_FORWARD = "forward";
    public static final String PREFS_COMMAND_STOP = "stop";
    public static final Boolean PREFS_COMMAND_IS_BINARY = true;

    public static final String PREFS_NAME = "osm.prefs";
}
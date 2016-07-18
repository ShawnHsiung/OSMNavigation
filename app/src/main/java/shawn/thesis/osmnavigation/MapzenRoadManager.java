package shawn.thesis.osmnavigation;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Mapzen Routing Services
 * Created by Shawn on 2016/4/22.
 */

public class MapzenRoadManager extends RoadManager {

    static final String MAPZEN_ROUTING_SERVICE = "http://valhalla.mapzen.com/route?";
    protected String mApiKey;

    private String mCosting = null;
    private String mLang = null;
    private String mUnits = null;

    /** mapping from Mapzen Routing to MapQuest maneuver IDs: */
    static final HashMap<Integer, Integer> MANEUVERS;
    static {
        MANEUVERS = new HashMap<Integer, Integer>();

        MANEUVERS.put(0, 0); //None
        MANEUVERS.put(1, 0); //Start
        MANEUVERS.put(2, 0); //StartRight
        MANEUVERS.put(3, 0); //StartLeft
        MANEUVERS.put(4, 24); //Destination
        MANEUVERS.put(5, 25); //DestinationRight
        MANEUVERS.put(6, 26); //DestinationLeft
        MANEUVERS.put(7, 2); //kBecomes
        MANEUVERS.put(8, 1); //kContinue
        MANEUVERS.put(9, 6);  //kSlightRight
        MANEUVERS.put(10, 7); //kRight
        MANEUVERS.put(11, 8); //kSharpRight
        MANEUVERS.put(12, 14); //kUturnRight
        MANEUVERS.put(13, 13); //kUturnLeft
        MANEUVERS.put(14, 5); //kSharpLeft
        MANEUVERS.put(15, 4); //kLeft
        MANEUVERS.put(16, 3); //kSlightLeft
        MANEUVERS.put(17, 19); //kRampStraight
        MANEUVERS.put(18, 18); //kRampRight
        MANEUVERS.put(19, 17); //kRampLeft
        MANEUVERS.put(20, 16); //kExitRight
        MANEUVERS.put(21, 15); //kExitLeft
        MANEUVERS.put(22, 11); //kStayStraight
        MANEUVERS.put(23, 10); //kStayRight
        MANEUVERS.put(24, 9); //kStayLeft
        MANEUVERS.put(25, 22); //kMerge
        MANEUVERS.put(26, 27); //kRoundaboutEnter
        MANEUVERS.put(27, 27); //kRoundaboutExit
        MANEUVERS.put(28, 37); //kFerryEnter
        MANEUVERS.put(29, 38); //kFerryExit
    }

    public MapzenRoadManager(String apiKey){
        super();
        mApiKey = apiKey;
    }

    /**
     * parameters(mandatory or options) required to make a request
     * https://mapzen.com/documentation/turn-by-turn/api-reference
     *
     * */
    public void addOptionParams(JSONObject options){
        mLang = options.optString("language");
        mUnits = options.optString("unites");
    }
    /**
     *
     * format
     * valhalla.mapzen.com/route?
     * json={
     *     "locations":[
     *         {"lat":42.358528,"lon":-83.271400,"street":"Appleton"...},
     *         {"lat":42.996613,"lon":-78.749855,"street":"Ranch Trail"...}]
     *      "costing":"auto"
     *      "direction_options":{"language:"", "units":""}}
     * &api_key=valhalla-xxxxxx
     */
    protected String getUrl(ArrayList<GeoPoint> waypoints) {

        StringBuffer urlString = new StringBuffer(MAPZEN_ROUTING_SERVICE);
        JSONObject jsonObject = new JSONObject();
        JSONArray locations = new JSONArray();
        JSONObject direction_option = new JSONObject();
        boolean tag = false;
        try {
            for (int i = 0; i < waypoints.size(); i++) {
                JSONObject item = new JSONObject();
                item.put("lat",waypoints.get(i).getLatitude());
                item.put("lon",waypoints.get(i).getLongitude());
                item.put("type","break");
                locations.put(item);
            }
            jsonObject.put("locations", locations);
            if (mCosting != null){
                jsonObject.put("costing",mCosting);
            }
            if (mLang != null){
                direction_option.put("language",mLang);
                tag =true;
            }
            if(mUnits != null){
                direction_option.put("units",mUnits);
            }
            if (tag)
                jsonObject.put("directions_options",direction_option);

        }catch (JSONException e){
            e.printStackTrace();
        }
        urlString.append("json="+jsonObject.toString());
        urlString.append("&api_key="+mApiKey);
        return urlString.toString();
    }

    @Override
    public void addRequestOption(String requestOption) {
        mCosting = requestOption;
    }

    @Override
    public Road getRoad(ArrayList<GeoPoint> waypoints) {
        String urlString = getUrl(waypoints);
        Log.d(BonusPackHelper.LOG_TAG, "MapzenRoadManager.getRoute:"+urlString);
        String jString = BonusPackHelper.requestStringFromUrl(urlString);
        if (jString == null) {
            return new Road(waypoints);
        }
        Road road = new Road();
        try {
            JSONObject jRoot = new JSONObject(jString);
            JSONObject jTrip = jRoot.getJSONObject("trip");
            JSONArray jLegs = jTrip.optJSONArray("legs");
            //path between two break point
            JSONObject jFirstPath = jLegs.getJSONObject(0);
            //shape
            String shape = jFirstPath.getString("shape");
            road.mRouteHigh = PolylineEncoder.decode(shape, 1, false);
            //maneuvers (instructions)
            JSONArray maneuvers = jFirstPath.getJSONArray("maneuvers");
            int len = maneuvers.length();
            for (int i = 0; i < len; i++){
                JSONObject jInstruction = maneuvers.getJSONObject(i);
                RoadNode node = new RoadNode();
                int positionIndex = jInstruction.getInt("begin_shape_index");
                node.mLocation = road.mRouteHigh.get(positionIndex);
                node.mLength = jInstruction.getDouble("length");
                node.mDuration = jInstruction.getInt("time");
                int direction = jInstruction.getInt("type");
                node.mManeuverType = getManeuverCode(direction);
                node.mInstructions = jInstruction.getString("instruction");
                road.mNodes.add(node);
            }
            //summary {length, time}
            JSONObject summary = jFirstPath.getJSONObject("summary");
            road.mLength = summary.getDouble("length");
            road.mDuration = summary.getInt("time");
            road.mStatus = Road.STATUS_OK;

        }catch (JSONException e){
            road.mStatus = Road.STATUS_TECHNICAL_ISSUE;
            e.printStackTrace();
        }
        if (road.mStatus != Road.STATUS_OK){
            //Create default road:
            int status = road.mStatus;
            road = new Road(waypoints);
            road.mStatus = status;
        } else {
            road.buildLegs(waypoints);
        }
        return road;
    }

    @Override public Road[] getRoads(ArrayList<GeoPoint> waypoints) {
        Road road = getRoad(waypoints);
        Road[] roads = new Road[1];
        roads[0] = road;
        return roads;
    }
    protected int getManeuverCode(int direction){
        Integer code = MANEUVERS.get(direction);
        if (code != null)
            return code;
        else
            return 0;
    }
}




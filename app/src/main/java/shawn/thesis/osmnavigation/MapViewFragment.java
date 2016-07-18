package shawn.thesis.osmnavigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapViewFragment extends Fragment implements MapStateManager, WayPointListener {

    private IMapController mapController;
    private MapView mapView;

    private NewMyLocationOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;

    private Road mRoad = null;
    private Polyline mRoadOverlay = null;
    private FolderOverlay mRoadNodeMarkers;

    private FloatingActionButton fab_gps;
    private FloatingActionButton fab_direction;

    private FrameLayout panel_instruction;
    private TextView txt_instruction;
    private FloatingActionButton fab_close;

    private boolean mTrackingMode = false;
    private boolean isFollowing = false;

    private SharedPreferences mPrefs;
    private OnFragmentMapViewListener mListener;

    private float mTolerance;

    final static float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    final static int LOW_SPEED_DISTANCE_DETECTION = 20; // 20 meters
//    final static int PASSED_NODE_DISTANCE = 1; // 1 meters

    static final HashMap<Integer, Integer> TURN_CONVERT;
    static {
        TURN_CONVERT = new HashMap<Integer, Integer>();
        //left
        TURN_CONVERT.put(3, 1);
        TURN_CONVERT.put(4, 1);
        TURN_CONVERT.put(5, 1);
        TURN_CONVERT.put(13, 1);
        TURN_CONVERT.put(15, 1);
        TURN_CONVERT.put(17, 1);
        //right
        TURN_CONVERT.put(6, 2);
        TURN_CONVERT.put(7, 2);
        TURN_CONVERT.put(8, 2);
        TURN_CONVERT.put(14, 2);
        TURN_CONVERT.put(16, 2);
        TURN_CONVERT.put(18, 2);
        TURN_CONVERT.put(27, 2);
        //forward
        TURN_CONVERT.put(0, 3);
        TURN_CONVERT.put(1, 3);
        TURN_CONVERT.put(2, 3);
        TURN_CONVERT.put(11, 3);
        TURN_CONVERT.put(19, 3);
        TURN_CONVERT.put(22, 3);

        TURN_CONVERT.put(24, 4);
        TURN_CONVERT.put(25, 4);
        TURN_CONVERT.put(26, 4);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        this.mPrefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.initMapView();
        this.addMyLocationOverlay();
        this.addCompassOverlay();
        this.addScaleBarOverlay();
        mRoadNodeMarkers = new FolderOverlay(getActivity());
        mRoadNodeMarkers.setName("Route Steps");
        mapView.getOverlays().add(mRoadNodeMarkers);

        if(savedInstanceState == null){
            this.mLocationOverlay.runOnFirstFix(new Runnable() {
                public void run() {
                    mapController.setCenter(mLocationOverlay.getMyLocation());
                }
            });
        }else{
            //set the position that kept in prefs
            this.mapController.setCenter(
                    new GeoPoint(mPrefs.getFloat(PREFS_LATITUDE,0f),
                            mPrefs.getFloat(PREFS_LONGITUDE, 0f)));
        }

        this.fab_gps = (FloatingActionButton) getActivity().findViewById(R.id.fab_gps);
        this.fab_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isFollowing = !isFollowing;
                if(isFollowing){
                    enableFollowLocation();
                }else {
                    disableFollowLocation();
                }

            }
        });
        this.fab_direction = (FloatingActionButton) getActivity().findViewById(R.id.fab_direction);
        this.fab_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onStartRouteSearchFragment();
            }
        });

        //Navigation Guidance
        this.panel_instruction = (FrameLayout) getActivity().findViewById(R.id.panel_instructions);
        this.txt_instruction = (TextView) getActivity().findViewById(R.id.txt_instruction);
        this.fab_close = (FloatingActionButton) getActivity().findViewById(R.id.fab_close);
        this.fab_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CloseTrackingMode();
            }
        });
    }

    /*
    * init MapView that set tile source provider, zoom, multi touch
    * and data connection
    * */
    public void initMapView(){
        this.mapView = (MapView) getActivity().findViewById(R.id.map);
        if(this.mapView != null){
            this.mapView.setTileSource(TileSourceFactory.MAPNIK);
            this.mapView.setBuiltInZoomControls(true);
            this.mapView.setMultiTouchControls(true);
            this.mapView.setUseDataConnection(true);
            this.mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            //Map Controller
            this.mapController = this.mapView.getController();
            //set default zoom level(max)
            this.mapController.setZoom(mPrefs.getInt(PREFS_ZOOM, 15));

            mTolerance = this.mapView.getProjection().metersToPixels(20);
        }
    }

    /*
    * add my location overlay, and init location services provider with GPS model
    * */
    public void addMyLocationOverlay(){
        //new GPS Location provider
        GpsMyLocationProvider gps = new GpsMyLocationProvider(getActivity());
        //set min distance of updating
        gps.setLocationUpdateMinDistance(MIN_DISTANCE_CHANGE_FOR_UPDATES);
        this.mLocationOverlay = new NewMyLocationOverlay(getActivity(),
                gps, this.mapView);
        this.mLocationOverlay.setDrawAccuracyEnabled(false);
        this.mLocationOverlay.enableMyLocation();
        this.mLocationOverlay.addObserver(this);
        this.mapView.getOverlays().add(this.mLocationOverlay);
    }

    /*
    * add compass overlay to map view
    * */
    public void addCompassOverlay(){
        //compass
        this.mCompassOverlay = new CompassOverlay(getActivity(),
                new InternalCompassOrientationProvider(getActivity()), this.mapView);
        this.mCompassOverlay.enableCompass();
        this.mapView.getOverlays().add(this.mCompassOverlay);
    }
    /*
    * add scale bar to map view, including set its position
    * */
    public void addScaleBarOverlay(){
        //scale bar
        final DisplayMetrics dm = this.getResources().getDisplayMetrics();
        this.mScaleBarOverlay = new ScaleBarOverlay(this.mapView);
        this.mScaleBarOverlay.setCentred(true);
        this.mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 20);
        this.mapView.getOverlays().add(this.mScaleBarOverlay);
    }

    public void enableFollowLocation(){
        this.fab_gps.setColorFilter(Color.rgb(41, 98, 255));
        mLocationOverlay.enableFollowLocation();
//        this.mapController.animateTo(this.mLocationOverlay.getMyLocation());
    }
    public void disableFollowLocation(){
        fab_gps.clearColorFilter();
        mLocationOverlay.disableFollowLocation();
    }

    public void CloseTrackingMode(){
        //TODO:
        List<Overlay> mapOverlays = mapView.getOverlays();
        if (mRoadOverlay != null){
            mapOverlays.remove(mRoadOverlay);
            mRoadOverlay = null;
        }
        mRoadNodeMarkers.closeAllInfoWindows();
        mRoadNodeMarkers.getItems().clear();
        mRoad = null;
        txt_instruction.setText("");
        hidePanelInstruction();
        mTrackingMode = false;
        mListener.onTurnCommandToBLE(0);//off
    }
    public void addNodeMaker(){
        Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
        Drawable nodeIcon_start = getResources().getDrawable(R.drawable.departure);
        Drawable nodeIcon_end = getResources().getDrawable(R.drawable.destination);

        MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
        TypedArray iconIds = getResources().obtainTypedArray(R.array.direction_icons);

        ArrayList<RoadNode> mNodes = mRoad.mNodes;
        int n = mNodes.size();
        for (int i=0; i<n; i++){
            RoadNode node = mNodes.get(i);
            String instructions = (node.mInstructions==null ? "" : node.mInstructions);
            Marker nodeMarker = new Marker(mapView);
            nodeMarker.setTitle(getString(R.string.step)+ " " + (i+1));
            nodeMarker.setSnippet(instructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(getActivity(), node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            if(i == 0){
                nodeMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
                nodeMarker.setIcon(nodeIcon_start);
            }else if(i == n-1){
                nodeMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
                nodeMarker.setIcon(nodeIcon_end);
            }else {
                nodeMarker.setIcon(nodeIcon);
            }
            nodeMarker.setInfoWindow(infoWindow); //use a shared infowindow.
            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
            if (iconId != R.drawable.ic_empty){
                Drawable image = getResources().getDrawable(iconId);
                nodeMarker.setImage(image);
            }
            mRoadNodeMarkers.add(nodeMarker);
        }
        iconIds.recycle();
    }

    public void updateRouteLine(Road road){
        if (road == null) return;
        mRoad = road;
        List<Overlay> mapOverlays = mapView.getOverlays();
        if (mRoadOverlay != null){
            mapOverlays.remove(mRoadOverlay);
            mRoadOverlay = null;
        }
        mRoadOverlay = RoadManager.buildRoadOverlay(road, getActivity());
        this.mapView.getOverlays().add(mRoadOverlay);
        this.addNodeMaker();
        this.mapView.invalidate();
        this.showPanelInstruction();
    }

    public void showPanelInstruction(){
        this.fab_direction.setVisibility(View.GONE);
        this.fab_gps.setVisibility(View.GONE);
        this.fab_close.setVisibility(View.VISIBLE);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) panel_instruction.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        panel_instruction.setLayoutParams(params);

        mTrackingMode = true;
    }

    public void hidePanelInstruction(){
        this.fab_direction.setVisibility(View.VISIBLE);
        this.fab_gps.setVisibility(View.VISIBLE);
        this.fab_close.setVisibility(View.GONE);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) panel_instruction.getLayoutParams();
        params.setMargins(0, 0, 0, -200);
        panel_instruction.setLayoutParams(params);
    }

    public GeoPoint postCurrentPosition(){
        GeoPoint point = mLocationOverlay.getMyLocation();
        return point;
    }

    public void onTargetReached(Location location) {
        if(mTrackingMode){
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            //if away from the route, then stop
            if(!mRoadOverlay.isCloseTo(new GeoPoint(lat, lon), mTolerance, mapView)){
                this.txt_instruction.setText("Stop");
                mListener.onTurnCommandToBLE(4);
            }
            closestInstructionNode(location);
        }
    }
    //found the closest point on Road
    //return the index of point
    public int closestDisPoint(Location location){
        int len = mRoad.mRouteHigh.size();
        int minIndex = -1;
        int minDis = 999999;
        for(int i = 0; i < len; i++)
        {
            GeoPoint p = mRoad.mRouteHigh.get(i);
            float[] result = {10000};
            Location.distanceBetween(location.getLatitude(),location.getLongitude(),
                    p.getLatitude(),
                    p.getLongitude(),result);
            if (minDis > Math.round(result[0])){
                minDis = Math.round(result[0]);
                minIndex = i;
            }
        }
        return minIndex;
    }

    //TODO: optimize passed point, reduce the number of calculation
    //TODO: to save high speed biking problem
    //TODO: another solution for point passed detection : using vector
    //TODO: dynamic path generation when user turn wrong
    public void closestInstructionNode(Location location){
        int len = mRoad.mNodes.size();
        int minIndex = -1;
        int minDis = 999999;
        float mSpeed = location.getSpeed();
        for(int i = 0; i < len; i++)
        {
            GeoPoint p = mRoad.mNodes.get(i).mLocation;
            float[] result = {10000};
            Location.distanceBetween(location.getLatitude(),location.getLongitude(),
                    p.getLatitude(),
                    p.getLongitude(),result);
            if (minDis > Math.round(result[0])){
                minDis = Math.round(result[0]);
                minIndex = i;
            }
        }
        int j = mRoad.mRouteHigh.indexOf(mRoad.mNodes.get(minIndex).mLocation);
        int k = closestDisPoint(location);

        if(j < k ){
            // passed
            // forward command
            mListener.onTurnCommandToBLE(3);
            txt_instruction.setText("forward");
        }else{
            //target instruction node
            if(minDis < LOW_SPEED_DISTANCE_DETECTION || (minDis / mSpeed) < 5){
                //turn command
                txt_instruction.setText(mRoad.mNodes.get(minIndex).mInstructions);
                mListener.onTurnCommandToBLE(TURN_CONVERT.get(mRoad.mNodes.get(minIndex).mManeuverType));
            }else {
                //forward command
                mListener.onTurnCommandToBLE(3);
                txt_instruction.setText("forward");
            }
        }
    }

    private void savePrefs(){
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(PREFS_ZOOM, mapView.getZoomLevel());
        GeoPoint c = (GeoPoint) mapView.getMapCenter();
        ed.putFloat(PREFS_LATITUDE, (float)c.getLatitude());
        ed.putFloat(PREFS_LONGITUDE, (float)c.getLongitude());
        ed.putBoolean(PREFS_TRACKING, mTrackingMode);
        ed.commit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentMapViewListener) {
            mListener = (OnFragmentMapViewListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentMapViewListener {
        void onStartRouteSearchFragment();
        void onTurnCommandToBLE(int i);
    }
}

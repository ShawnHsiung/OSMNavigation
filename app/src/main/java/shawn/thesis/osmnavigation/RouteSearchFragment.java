package shawn.thesis.osmnavigation;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.util.ManifestUtil;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class RouteSearchFragment extends Fragment {

    private ImageView mCar;
    private ImageView mBike;
    private ImageView mWalker;
    private RouteModel mRouteModel = null;

    private AutoCompleteTextView wDeparture;
    private AutoCompleteTextView wDestination;

    ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

    private ImageView mBack;
    private Button mBtnSearch;

    private OnFragmentRouteSearchListener mListener;

    static String graphHopperApiKey;
    static String mapQuestApiKey;
    static String MapzenRoutingApiKey;
    static final String userAgent = "OSMRouting/1.0";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_route_search, container, false);

        mBtnSearch = (Button) view.findViewById(R.id.btn_search);
//        mBack = (ImageView) view.findViewById(R.id.back);

        wDeparture = (AutoCompleteTextView) view.findViewById(R.id.departure);
        wDestination = (AutoCompleteTextView) view.findViewById(R.id.destination);

        mCar = (ImageView) view.findViewById(R.id.model_car);
        mBike = (ImageView) view.findViewById(R.id.model_bicycle);
        mWalker = (ImageView) view.findViewById(R.id.model_walker);

//        if (mBack != null) {
//            mBack.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    mListener.onBackToParent();
//                }
//            });
//        }

        if (mBtnSearch != null) {
            mBtnSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    geoCodeingAddress();
                }
            });
        }

        if (mCar != null) {
            mCar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mRouteModel = RouteModel.DRIVING;
                    mCar.setColorFilter(Color.rgb(0, 0, 0));
                    mBike.setColorFilter(Color.rgb(255, 255, 255));
                    mWalker.setColorFilter(Color.rgb(255, 255, 255));
                }
            });
        }
        if (mBike != null) {
            mBike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mRouteModel = RouteModel.BIKING;
                    mCar.setColorFilter(Color.rgb(255, 255, 255));
                    mBike.setColorFilter(Color.rgb(0, 0, 0));
                    mWalker.setColorFilter(Color.rgb(255, 255, 255));
                }
            });
        }
        if (mWalker != null) {
            mWalker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mRouteModel = RouteModel.WALKING;
                    mCar.setColorFilter(Color.rgb(255, 255, 255));
                    mBike.setColorFilter(Color.rgb(255, 255, 255));
                    mWalker.setColorFilter(Color.rgb(0, 0, 0));
                }
            });
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MainActivity) getActivity()).setActionBarTitle("Route Search");

        graphHopperApiKey = ManifestUtil.retrieveKey(getActivity(), "GRAPHHOPPER_API_KEY");
        mapQuestApiKey = ManifestUtil.retrieveKey(getActivity(), "MapQuest.Routing");
        MapzenRoutingApiKey = ManifestUtil.retrieveKey(getActivity(), "Mapzen.Routing");
    }

    public void geoCodeingAddress() {
        if (waypoints.size() > 0)
            waypoints.clear();
        String strStartAdress = wDeparture.getText().toString();
        String strEndAdress = wDestination.getText().toString();
        if (mRouteModel == null) {
            Toast.makeText(getActivity(), "Please choose travel model" +
                    " {driving, biking, walking}", Toast.LENGTH_SHORT).show();
            return;
        }
        if (strStartAdress.equals("")) {
            Toast.makeText(getActivity(), "Please input departure address", Toast.LENGTH_SHORT).show();
            return;
        } else if (strStartAdress.toUpperCase().equals("YOUR LOCATION")) {
            mListener.onMyLocation();
        } else {
            new GeocodingTask().execute(strStartAdress);
        }
        if (strEndAdress.equals("")) {
            Toast.makeText(getActivity(), "Please input destination address", Toast.LENGTH_SHORT).show();
            return;
        } else {
            new GeocodingTask().execute(strEndAdress);
        }
    }

    /*
    * Geocoding address
    * */
    private class GeocodingTask extends AsyncTask<Object, Void, List<Address>> {
        protected List<Address> doInBackground(Object... params) {
            try {
                String startAddress = (String) params[0];
                GeocoderNominatim geocoder = new GeocoderNominatim(getActivity(), userAgent);
                try {
                    List<Address> address = geocoder.getFromLocationName(startAddress, 1);
                    return address;
                } catch (Exception e) {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
        protected void onPostExecute(List<Address> result) {
            if (result == null) {
                Toast.makeText(getActivity(),
                        "address geocoding error", Toast.LENGTH_SHORT).show();
            } else if (result.size() == 0) { //if no address found, display an error
                Toast.makeText(getActivity(),
                        "address not found.", Toast.LENGTH_SHORT).show();
            } else {
                Address address = result.get(0); //get first address
                GeoPoint geoPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                waypoints.add(geoPoint);
                if (waypoints.size() == 2)
                    new GetRouteLineTask().execute();
            }
        }
    }

    /*
   * Async Task to get route
   * */
    private class GetRouteLineTask extends AsyncTask<Object, Void, Road[]> {

        protected Road[] doInBackground(Object... params) {
            Locale locale = Locale.getDefault();
            RoadManager roadManager;
            switch (mRouteModel) {
                case DRIVING:
                    roadManager = new OSRMRoadManager(getActivity());
                    break;
                case BIKING:
//                    roadManager = new GraphHopperRoadManager(graphHopperApiKey);
////                    roadManager.addRequestOption("locale="+locale.getLanguage());
//                    roadManager.addRequestOption("vehicle=bike");
                    roadManager = new MapzenRoadManager(MapzenRoutingApiKey);
                    roadManager.addRequestOption("bicycle");
                    break;
                case WALKING:
//                    roadManager = new MapQuestRoadManager(mapQuestApiKey);
////                    roadManager.addRequestOption("locale="+locale.getLanguage());
//                    roadManager.addRequestOption("routeType=pedestrian");
                    roadManager = new MapzenRoadManager(MapzenRoutingApiKey);
                    roadManager.addRequestOption("pedestrian");
                    break;
                default:
                    roadManager = new OSRMRoadManager(getActivity());
            }
            return roadManager.getRoads(waypoints);
        }

        protected void onPostExecute(Road[] result) {
            if (result == null)
                return;
            if (result[0].mStatus == Road.STATUS_TECHNICAL_ISSUE) {
                Toast.makeText(getActivity(), "Technical issue when getting the route", Toast.LENGTH_SHORT).show();
                return;
            } else if (result[0].mStatus > Road.STATUS_TECHNICAL_ISSUE) {//functional issues
                Toast.makeText(getActivity(), "No possible route here", Toast.LENGTH_SHORT).show();
                return;
            } else {
                mListener.onUpdateRoad(result[0]);
            }
        }
    }
    public void onCurrentPosition(GeoPoint myLocation){
        if(myLocation != null){
            waypoints.add(myLocation);
        }else {
            Toast.makeText(getActivity(), "Can not get current location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentRouteSearchListener) {
            mListener = (OnFragmentRouteSearchListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentRouteSearchListener");
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
    public interface OnFragmentRouteSearchListener {
        void onUpdateRoad(Road road);
        void onMyLocation();
    }
}

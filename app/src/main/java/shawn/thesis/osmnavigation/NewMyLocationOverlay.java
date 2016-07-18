package shawn.thesis.osmnavigation;

import android.content.Context;
import android.location.Location;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;


public class NewMyLocationOverlay extends MyLocationNewOverlay {

    private ArrayList<WayPointListener> observers = new ArrayList<WayPointListener>();

    public NewMyLocationOverlay(Context context, MapView mapView){
        super(context, mapView);
    }

    public NewMyLocationOverlay(Context context, IMyLocationProvider myLocationProvider,
                                MapView mapView) {
        super(myLocationProvider, mapView, new DefaultResourceProxyImpl(context));
    }
    public NewMyLocationOverlay(IMyLocationProvider myLocationProvider, MapView mapView,
                                ResourceProxy resourceProxy) {
        super(myLocationProvider, mapView, resourceProxy);
    }

    public void addObserver(WayPointListener obj){
        this.observers.add(obj);
    }

    @Override
    public void onLocationChanged(final Location location, IMyLocationProvider source) {

        super.onLocationChanged(location,source);

        if(location != null){

            for (WayPointListener obj:this.observers
                 ) {
                obj.onTargetReached(location);
            }
        }
    }
}

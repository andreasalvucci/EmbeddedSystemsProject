package com.example.cameraapp2;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.icu.number.Scale;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MapBottomSheetDialog extends BottomSheetDialogFragment {

    CronetEngine cronetEngine;
    Context context;
    private MapView mapView;
    private List<GeoPoint> punti;
    private List<Integer> codes;
    private TperUtilities tper;
    private ArrayList<OverlayItem> items = new ArrayList<>();
    private ScaleBarOverlay scaleBarOverlay;
    Drawable busStopMarker;
    private ProgressBar progressBar;

    public MapBottomSheetDialog(Context context, List<GeoPoint> coordinate, List<Integer> codici, TperUtilities tper) {
        this.context = context;
        this.punti = coordinate;
        this.codes = codici;
        this.tper = tper;
        CronetEngine.Builder myBuilder = new CronetEngine.Builder(context);
        cronetEngine = myBuilder.build();


        int i = 0;
        for (i = 0; i < coordinate.size(); i++) {
            int codice = codici.get(i);
            GeoPoint punto = coordinate.get(i);
            items.add(new OverlayItem(String.valueOf(codice), "Description", punto));
        }
    }

    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_bottom_sheet_layout,
                container, false);
        busStopMarker = getResources().getDrawable(R.drawable.bus_stop);

        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.INVISIBLE);
        mapView = v.findViewById(R.id.map);
        mapView.setUseDataConnection(true);


        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(20.0);
        GeoPoint startPoint = new GeoPoint(punti.get(0));
        mapController.setCenter(startPoint);

        ItemizedOverlay<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        progressBar.setVisibility(View.VISIBLE);

                        int code = Integer.valueOf(item.getTitle());
                        String stopName = tper.getBusStopByCode(code);
                        item.setMarker(busStopMarker);
                        Executor executor = Executors.newSingleThreadExecutor();
                        String url = "https://tper-backend.herokuapp.com/fermata/" + code;
                        Log.d("LASTRING", url);
                        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(url
                                , new MyUrlRequestCallback(getActivity().getSupportFragmentManager(), stopName, progressBar), executor);
                        UrlRequest request = requestBuilder.build();
                        request.start();
                        //do something
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, getContext());
        for (int i = 0; i < mOverlay.size(); i++) {
            mOverlay.getItem(i).setMarker(busStopMarker);
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 10);
        mapView.getOverlays().add(scaleBarOverlay);

        mapView.getOverlays().add(mOverlay);


        return v;
    }

}

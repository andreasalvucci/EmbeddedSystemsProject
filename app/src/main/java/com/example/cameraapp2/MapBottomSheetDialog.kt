package com.example.cameraapp2;

import static com.example.cameraapp2.tper.proxy.MyUrlRequestCallback.HOSTNAME;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cameraapp2.tper.TperUtilities;
import com.example.cameraapp2.tper.proxy.MyUrlRequestCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MapBottomSheetDialog extends BottomSheetDialogFragment {
    private static final String TAG = MapBottomSheetDialog.class.getSimpleName();

    CronetEngine cronetEngine;
    Context context;
    private final List<GeoPoint> points;

    private final TperUtilities tper;
    private final ArrayList<OverlayItem> items = new ArrayList<>();
    private TextView waitingForTperResponse;
    Drawable busStopMarker;
    private ProgressBar progressBar;

    public MapBottomSheetDialog(Context context, List<GeoPoint> coordinate, List<Integer> codes, TperUtilities tper) {
        this.context = context;
        this.points = coordinate;
        this.tper = tper;

        CronetEngine.Builder myBuilder = new CronetEngine.Builder(context);
        cronetEngine = myBuilder.build();

        for (int i = 0; i < coordinate.size(); i++) {
            int code = codes.get(i);
            GeoPoint point = coordinate.get(i);
            items.add(new OverlayItem(String.valueOf(code), "Description", point));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_bottom_sheet_layout, container, false);
        busStopMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.bus_stop, null);
        waitingForTperResponse = v.findViewById(R.id.waiting_for_tper_response);
        waitingForTperResponse.setVisibility(View.INVISIBLE);


        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.INVISIBLE);
        MapView mapView = v.findViewById(R.id.map);
        mapView.setUseDataConnection(true);

        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(20.0);
        GeoPoint startPoint = new GeoPoint(points.get(0));
        mapController.setCenter(startPoint);

        ItemizedOverlay<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        waitingForTperResponse.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);

                        int code = Integer.parseInt(item.getTitle());
                        String stopName = tper.getBusStopByCode(code);
                        item.setMarker(busStopMarker);
                        Executor executor = Executors.newSingleThreadExecutor();
                        String url = HOSTNAME + "/fermata/" + code;
                        Log.d(TAG, "LASTRING " + url);
                        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(url,
                                new MyUrlRequestCallback(getActivity().getSupportFragmentManager(),
                                        stopName, progressBar, waitingForTperResponse), executor);
                        UrlRequest request = requestBuilder.build();
                        request.start();

                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, requireContext());

        for (int i = 0; i < mOverlay.size(); i++) {
            mOverlay.getItem(i).setMarker(busStopMarker);
        }

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 10);
        mapView.getOverlays().add(scaleBarOverlay);

        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        mLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(mLocationOverlay);

        mapView.getOverlays().add(mOverlay);

        return v;
    }

}
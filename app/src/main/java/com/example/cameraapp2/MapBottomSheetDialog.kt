package com.example.cameraapp2

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.cameraapp2.tper.TperUtilities
import com.example.cameraapp2.tper.proxy.MyUrlRequestCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.chromium.net.CronetEngine
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlay
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MapBottomSheetDialog(
    private val contextMainActivity: Context,
    private val points: List<GeoPoint>,
    codes: List<Int>,
    private val tper: TperUtilities
) : BottomSheetDialogFragment() {
    private var cronetEngine: CronetEngine
    private val items = ArrayList<OverlayItem>()
    private lateinit var waitingForTperResponse: TextView
    private lateinit var progressBar: ProgressBar
    private val executor: Executor = Executors.newSingleThreadExecutor()

    init {
        val myBuilder = CronetEngine.Builder(contextMainActivity)
        cronetEngine = myBuilder.build()
        for (i in points.indices) {
            val code = codes[i]
            val point = points[i]
            items.add(OverlayItem(code.toString(), "Description", point))
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.map_bottom_sheet_layout, container, false)

        val busStopMarker = ResourcesCompat.getDrawable(resources, R.drawable.bus_stop, null)

        waitingForTperResponse = v.findViewById(R.id.waiting_for_tper_response)
        waitingForTperResponse.visibility = View.INVISIBLE
        progressBar = v.findViewById(R.id.progressBar)
        progressBar.isIndeterminate = true
        progressBar.visibility = View.INVISIBLE
        val mapView = v.findViewById<MapView>(R.id.map)
        mapView.setUseDataConnection(true)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(DEFAULT_ZOOM_LEVEL)
        val startPoint = GeoPoint(points[0])
        mapController.setCenter(startPoint)
        val itemizedOverlay: ItemizedOverlay<OverlayItem> = ItemizedOverlayWithFocus(
            items, object : OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    requestStopBusesTimes(item, busStopMarker)
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean = false
            }, requireContext()
        )

        for (i in 0 until itemizedOverlay.size()) {
            itemizedOverlay.getItem(i).setMarker(busStopMarker)
        }

        val displayMetrics = contextMainActivity.resources.displayMetrics
        val scaleBarOverlay = ScaleBarOverlay(mapView)
        scaleBarOverlay.setScaleBarOffset(displayMetrics.widthPixels / 2, 10)
        mapView.overlays.add(scaleBarOverlay)

        val myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(contextMainActivity), mapView
        )
        myLocationOverlay.enableMyLocation()

        mapView.overlays.add(myLocationOverlay)
        mapView.overlays.add(itemizedOverlay)

        return v
    }

    private fun requestStopBusesTimes(
        item: OverlayItem, busStopMarker: Drawable?
    ) {
        waitingForTperResponse.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        val code = item.title.toInt()
        val stopName = tper.getBusStopByCode(code)
        item.setMarker(busStopMarker)
        val url = "${MyUrlRequestCallback.HOSTNAME}/fermata/$code"
        Log.d(TAG, "URL: $url")
        val request = cronetEngine.newUrlRequestBuilder(
            url, MyUrlRequestCallback(
                requireActivity().supportFragmentManager,
                stopName,
                progressBar,
                waitingForTperResponse
            ), executor
        ).build()
        request.start()
    }

    companion object {
        private val TAG = MapBottomSheetDialog::class.java.simpleName
        private const val DEFAULT_ZOOM_LEVEL = 20.0
    }
}
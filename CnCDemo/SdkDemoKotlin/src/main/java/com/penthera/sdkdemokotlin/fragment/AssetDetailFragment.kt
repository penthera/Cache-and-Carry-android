package com.penthera.sdkdemokotlin.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.activity.VideoPlayerActivity
import com.penthera.sdkdemokotlin.catalog.CatalogItemType
import com.penthera.sdkdemokotlin.catalog.ExampleCatalog
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import com.penthera.sdkdemokotlin.catalog.ExampleMetaData
import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder
import com.penthera.virtuososdk.client.database.AssetColumns
import com.penthera.virtuososdk.manager.PermissionManager
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.fragment_asset_detail.*
import org.ocpsoft.prettytime.PrettyTime
import java.lang.Exception
import java.net.URL
import java.util.*

/**
 *
 */
class AssetDetailFragment : Fragment()  {
    companion object {
        fun newInstance(asset: IAsset): AssetDetailFragment {
            var frag =  AssetDetailFragment()

            val args : Bundle = Bundle()
            args.putString("assetID", asset.uuid)
             frag.arguments = args
            return frag
        }

        fun newInstance(catalogItem: ExampleCatalogItem): AssetDetailFragment {
            var frag = AssetDetailFragment()

            val args = Bundle()

            args.putString("catalogID", catalogItem.exampleAssetId)
            frag.arguments = args
            return frag
        }
    }

    var catalogItem: ExampleCatalogItem? = null

    private var asset: IAsset? = null

    private var offlineVideoEngine: OfflineVideoEngine? = null

    private lateinit var exampleCatalog : ExampleCatalog
    private lateinit var queueOserver : AssetQueueObserver;

    private lateinit var rootView : View
    private lateinit var progressBar: ProgressBar



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_asset_detail, container, false)

        return rootView;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        asset?.let{outState.putString("assetID", it.uuid)}
        catalogItem?.let{outState.putString("catalogId", it.exampleAssetId)}


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        offlineVideoEngine = offlineVideoProvider.getOfflineEngine()
        exampleCatalog = ExampleCatalog(context!!)
        queueOserver = AssetQueueObserver(activity)


        var assetid : String?  = null

        arguments?.let { assetid = if(it.containsKey("assetID") ) it.getString("assetID") else null }
        //value in saved instance state will override value in arguments
        savedInstanceState?.let{assetid = if(it.containsKey("assetID") ) it.getString("assetID") else if (assetid != null) assetid else null}

        asset = if(assetid != null) offlineVideoEngine?.getVirtuoso()?.assetManager?.get(assetid) as IAsset else null

        var catalogId : String? = null

        arguments?.let{catalogId = if(it.containsKey("catalogID") ) it.getString("catalogID") else null }
        savedInstanceState?.let{catalogId = if(it.containsKey("catalogID") ) it.getString("catalogID") else if (catalogId != null) catalogId else null}

        catalogItem = if(catalogId != null) exampleCatalog.findItemById(catalogId!!) else null

        setupUI()
    }




    override fun onAttach(context: Context?) {
        super.onAttach(context)

    }

    override fun onPause() {
        super.onPause()
        offlineVideoEngine?.getVirtuoso()?.removeObserver(queueOserver)

    }

    override fun onResume() {
        super.onResume()
        queueOserver.update(this)
        offlineVideoEngine?.getVirtuoso()?.addObserver(queueOserver)
    }


    private fun setupUI(){
        if(catalogItem == null ){
            asset?.let{
                catalogItem = exampleCatalog.findItemById(asset!!.assetId)

            }
        }

        if(asset == null){

            var list : MutableList<IIdentifier>? = offlineVideoEngine?.getVirtuoso()?.assetManager?.getByAssetId(catalogItem?.exampleAssetId)

            list?.let{
                if (it.size > 0)
                    asset = list[0] as IAsset
            }
        }

        if(catalogItem == null && asset == null){
            activity?.onBackPressed()
        }

        txt_title.text = catalogItem?.title

        if(!catalogItem?.contentRating.isNullOrEmpty()){
            txt_parental_rating.text = catalogItem?.contentRating
        }

        Picasso.get()
                .load(catalogItem?.imageUri)
                .into(object : Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        val mutableBitmap = if (bitmap!!.isMutable())
                            bitmap
                        else
                            bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = Canvas(mutableBitmap)
                        val colour = 88 and 0xFF shl 24
                        canvas.drawColor(colour, PorterDuff.Mode.DST_IN)

                        detail_bg_img.background = BitmapDrawable(mutableBitmap)
                    }

                })


        txt_description.text = catalogItem?.description
        txt_duration.text = getDurationString(catalogItem!!.durationSeconds)

        txt_expiry.text = makePretty(getExpiry(), "Never");
        txt_available.visibility = View.GONE;
        txt_description.text = catalogItem?.description

        var assetId : String  = catalogItem!!.exampleAssetId

        btn_download.text = if (isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ) "Delete " else  "Download"

        btn_download.setOnClickListener () {_ ->
            if(isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ){
                showDeleteDialog()
            }
            else{
                downloadItem()
            }
        }

        btn_watch.setOnClickListener() {_ ->
            watchItem();
        }
    }

    /**
     * @return expiration time in milliseconds
     */
    private fun getExpiry(): Long {
        // Use Catalog Value
        var expiry = catalogItem!!.expiryDate
        // Override with SDK value if exists
        if (asset != null) {
            expiry = getExpiration(asset!!)
        }
        if (expiry > 0)
            expiry *= 1000

        return expiry
    }


    /**
     * Used by inbox to print expiration time
     *
     * @param asset the asset
     * @return expiration in seconds, -1 never
     */
    fun getExpiration(asset: IAsset): Long {
        val completionTime = asset.completionTime
        val endWindow = asset.endWindow
        // Not downloaded
        if (completionTime == 0L) {
            return if (endWindow == java.lang.Long.MAX_VALUE) -1 else endWindow
// Downloaded
        } else {
            //here the minimum value is used in the calculation.
            val playTime = asset.firstPlayTime
            var playExpiry = java.lang.Long.MAX_VALUE
            val eap = asset.eap
            val ead = asset.ead
            var expiry = endWindow

            if (playTime > 0 && eap > -1)
                playExpiry = playTime + eap

            expiry = Math.min(expiry, playExpiry)

            if (ead > -1)
                expiry = Math.min(expiry, completionTime + ead)

            return getExpiration(asset.completionTime, asset.endWindow, asset.firstPlayTime, asset.eap, asset.ead)
        }
    }

    fun getExpiration(completionTime: Long, endWindow: Long, firstPlayTime: Long, expiryAfterPlay: Long, expiryAfterDownload: Long): Long {
        // Not downloaded
        if (completionTime == 0L) {
            return if (endWindow == java.lang.Long.MAX_VALUE) -1 else endWindow
// Downloaded
        } else {
            //here the minimum value is used in the calculation.
            var playExpiry = java.lang.Long.MAX_VALUE
            var expiry = endWindow

            if (firstPlayTime > 0 && expiryAfterPlay > -1)
                playExpiry = firstPlayTime + expiryAfterPlay

            expiry = Math.min(expiry, playExpiry)

            if (expiryAfterDownload > -1)
                expiry = Math.min(expiry, completionTime + expiryAfterDownload)

            return if (expiry == java.lang.Long.MAX_VALUE) -1 else expiry
        }
    }

    fun getDurationString(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val secondsVal = seconds % 60
        return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(secondsVal)
    }

    /**
     * Create a two digit time [prepend 0s to the time]
     *
     * @param number the number to pad
     *
     * @return two digit time as string
     */
    fun twoDigitString(number: Int): String {
        if (number == 0) {
            return "00"
        }
        return if (number / 10 == 0) {
            "0$number"
        } else number.toString()
    }


    /**
     * true, item is in Q
     * @param assetId
     * @return true, item is in Q
     */
    private fun isQ(assetId: String): Boolean {
        var ret: Boolean

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.queue?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
            c = null
        }

        return ret
    }

    /**
     * true, the item has been downloaded
     * @param assetId
     * @return true, the item has been downloaded
     */
    private fun isDownloaded(assetId: String): Boolean {

        var ret: Boolean

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.downloaded?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
            c = null
        }

        return ret
    }

    /**
     * true, the item has been downloaded
     * @param assetId
     * @return true, the item has been downloaded
     */
    private fun isExpired(assetId: String): Boolean {

        var ret: Boolean = false

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.expired?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
        }

        return ret;
    }

    /**
     * Displays times like "2 Days from now"
     *
     * @param value timestamp in milliseconds
     *
     * @return Pretty time, never if -1 passeed
     */
    private fun makePretty(value: Long, fallback: String): String {
        if (value < 0) {
            return fallback
        }

        val p = PrettyTime()
        return p.format(Date(value))
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.are_you_sure))
        builder.setMessage(getString(R.string.delete_item))
        builder.setPositiveButton(android.R.string.yes) { dialog, _ ->
            deleteItem()
            dialog!!.dismiss()

        }
        builder.setNegativeButton(android.R.string.no) { dialog, _ ->
            dialog!!.dismiss()

        }
        builder.create().show()
    }

    private fun deleteItem() {

        var assetId : String = catalogItem!!.exampleAssetId
        if(asset == null){
            asset = offlineVideoEngine?.getVirtuosoAsset(assetId)
        }

        offlineVideoEngine?.getVirtuoso()?.assetManager?.delete(asset)
        activity?.runOnUiThread() {
            btn_download.text = if (isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ) "Delete " else  "Download"
        }
    }

    private fun downloadItem(){

        val title = catalogItem?.title ?: "Untitled"
        val image = catalogItem?.imageUri ?: ""
        val metadata = ExampleMetaData(title, image).toJson()

        when(catalogItem?.contentType){

            CatalogItemType.DASH_MANIFEST -> {
                val pdlg = ProgressDialog.show(context, "Processing manifest", "Adding fragments...")
                val contentURL = URL(catalogItem?.contentUri)

                offlineVideoEngine?.getVirtuoso()?.assetManager?.createMPDSegmentedAssetAsync(MPDAssetBuilder().assetId(catalogItem?.exampleAssetId)
                        .manifestUrl(contentURL)
                        .assetObserver(AssetObserver(pdlg, activity!!))
                        .addToQueue(true)
                        .desiredAudioBitrate(0)
                        .desiredVideoBitrate(0)
                        .withMetadata(metadata)
                        .withPermissionObserver(AssetPermissionObserver(activity!!))
                        .build())
            }

            CatalogItemType.HLS_MANIFEST ->{
                val pdlg = ProgressDialog.show(context, "Processing manifest", "Adding fragments...")
                val contentURL = URL(catalogItem?.contentUri)
                offlineVideoEngine?.getVirtuoso()?.assetManager?.createHLSSegmentedAssetAsync(HLSAssetBuilder().assetId(catalogItem?.exampleAssetId)
                        .manifestUrl(contentURL)
                        .assetObserver(AssetObserver(pdlg, activity!!))
                        .addToQueue(true)
                        .desiredVideoBitrate(0)
                        .withMetadata(metadata)
                        .withPermissionObserver(AssetPermissionObserver(activity!!))
                        .build())

            }

            CatalogItemType.FILE -> {

                val manager :IAssetManager = offlineVideoEngine?.getVirtuoso()!!.assetManager;

                val file: IFile = manager.createFileAsset(catalogItem?.contentUri, catalogItem?.exampleAssetId, catalogItem?.mimeType, metadata)
                manager.queue.add(file)

            }

        }

    }

    private fun watchItem(){

        if(asset == null){
            VideoPlayerActivity.playVideoStream(catalogItem!!, context!!)
        }
        else{
            VideoPlayerActivity.playVideoDownload(asset!!,context!!)
        }

    }

    class AssetQueueObserver(activity: Activity?) : Observers.IQueueObserver {


        var lastProgress : Int = -1
        var  mActivity : Activity?

        lateinit var parent: AssetDetailFragment

        init{
            mActivity = activity
        }

        override fun engineStartedDownloadingAsset(aAsset: IIdentifier) {
            lastProgress = -1
            updateItem(aAsset, true)
        }

        override fun enginePerformedProgressUpdateDuringDownload(aAsset: IIdentifier) {
            updateItem(aAsset, true)
        }

        override fun engineCompletedDownloadingAsset(aAsset: IIdentifier) {
            updateItem(aAsset, true)
        }

        override fun engineEncounteredErrorDownloadingAsset(aAsset: IIdentifier) {
            // The base implementation does nothing.  See class documentation.
        }

        override fun engineCompletedDownloadingSegment(aAsset: IIdentifier) {
            updateItem(aAsset, true)
        }

        override fun engineUpdatedQueue() {
            // The base implementation does nothing.  See class documentation.
        }

        override fun engineEncounteredErrorParsingAsset(mAssetId: String) {}

        fun  update(fragment: AssetDetailFragment ){
           parent = fragment

        }
        private fun updateItem(aFile: IIdentifier, forceUpdate: Boolean) {

            val asset = aFile as IAsset
            val assetId = asset.assetId

            // Progress is for catalog item
            if (!TextUtils.isEmpty(assetId) && assetId == parent?.catalogItem?.exampleAssetId) {
                //update our asset status
                mActivity?.runOnUiThread( { updateItemStatus(asset, forceUpdate) })

            }
        }

        private fun updateItemStatus(asset: IAsset?, forceUpdate: Boolean) {
            if (asset != null && asset.assetId == parent.catalogItem?.exampleAssetId) {

                //update our asset reference
                parent.asset = asset

                var progress = (parent.asset!!.getFractionComplete() * 100.0).toInt()
                // Not a repeated progress -- Keep context switches minimal due to frequency of messages, unless forced
                if (forceUpdate || progress != lastProgress) {
                    var assetStatus = ""
                    val fds = parent.asset?.getDownloadStatus()
                    val value: String
                    when (fds) {

                        Common.AssetStatus.DOWNLOADING -> {
                            assetStatus = parent.getString(R.string.status_downloading)
                            value = "downloading"
                        }

                        Common.AssetStatus.DOWNLOAD_COMPLETE -> {
                            assetStatus = parent.getString(R.string.status_downloaded)
                            value = "complete"
                        }

                        Common.AssetStatus.EXPIRED -> {
                            assetStatus = parent.getString(R.string.status_expired)
                            value = "expired"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ASSET -> {
                            assetStatus = "Queued"
                            value = "DENIED : MAD"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> {
                            assetStatus = "Queued"
                            value = "DENIED : MDA"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> {
                            assetStatus = "Queued"
                            value = "DENIED : EXT"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> {
                            assetStatus = "Queued"
                            value = "DENIED :MPD"
                        }

                        Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> {
                            assetStatus = "Queued"
                            value = "AWAITING PERMISSION"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_COPIES -> {
                            assetStatus = "Queued"
                            value = "DENIED : COPIES"
                        }

                        else -> {
                            assetStatus = parent.getString(R.string.status_pending)
                            value = "pending"
                        }
                    }
                    val tv = parent.rootView.findViewById(R.id.txt_assetstatus) as TextView
                    tv.visibility = View.VISIBLE
                    tv.setText(String.format(parent.getString(R.string.asset_status), assetStatus, asset.getErrorCount(), value))

                    lastProgress = progress
                    // Tiny Progress
                    if (progress == 0) {
                        progress = 1
                    }

                    // Progress Bar
                    val pb = parent.rootView.findViewById(R.id.prg) as ProgressBar
                    if (progress > 0 && progress < 100) {
                        pb.progress = progress
                        pb.visibility = View.VISIBLE
                    } else {
                        pb.visibility = View.GONE
                    }
                }
            }
        }
    }

    class AssetPermissionObserver (activity : FragmentActivity ) : IQueue.IQueuedAssetPermissionObserver {

        private var mActivity : FragmentActivity

        init{
            mActivity = activity
        }

        override fun onQueuedWithAssetPermission(aQueued: Boolean, aDownloadPermitted: Boolean, aAsset: IAsset?, aAssetPermissionError: Int) {
            var error_string: String
            val permResponse = aAsset?.lastPermissionResponse
            val assetPerm = if (permResponse?.permission == IAssetPermission.PermissionCode.PERMISSION_DENIED_EXTERNAL_POLICY)
                permResponse?.friendlyName()
            else
                IAssetPermission.PermissionCode.friendlyName(aAssetPermissionError)
            val title: String
            if (!aQueued) {

                title = "Queue Permission Denied"
                error_string = "Not permitted to queue asset [$assetPerm]  response: $permResponse"
                if (aAssetPermissionError == IAssetPermission.PermissionCode.PERMISSON_REQUEST_FAILED) {
                    error_string = "Not permitted to queue asset [$assetPerm]  This could happen if the device is currently offline."


                }

            } else {
                title = "Queue Permission Granted"
                error_string = "Asset " + (if (aDownloadPermitted) "Granted" else "Denied") + " Download Permission [" + assetPerm + "]  response: " + permResponse

            }

            mActivity.runOnUiThread {
                val permDlgBuilder = AlertDialog.Builder(mActivity)
                permDlgBuilder.setTitle(title)
                permDlgBuilder.setMessage(error_string)
                permDlgBuilder.setCancelable(false)
                permDlgBuilder.setPositiveButton("OK"
                ) { dialog, _ -> dialog.cancel() }

                val alert11 = permDlgBuilder.create()
                alert11.show()
            }



        }

    }

    class AssetObserver(progress : ProgressDialog, activity: FragmentActivity) : ISegmentedAssetFromParserObserver  {

        var mProgress : ProgressDialog
        var mActivity : FragmentActivity
        init{
           mProgress = progress
            mActivity = activity
        }
        override fun didParseSegment(segment: ISegment?): String {
           return segment!!.remotePath
        }

        override fun willAddToQueue(aSegmentedAsset: ISegmentedAsset?) {
        }

        override fun complete(aSegmentedAsset: ISegmentedAsset?, aError: Int, addedToQueue: Boolean) {
            try {
                mProgress.dismiss()
            }
            catch (e : Exception){}


            if (aSegmentedAsset == null) {
                val builder1 = AlertDialog.Builder(mActivity)
                builder1.setTitle("Could Not Create Asset")
                builder1.setMessage("Encountered error(" + Integer.toString(aError) + ") while creating asset.  This could happen if the device is currently offline, or if the asset manifest was not accessible.  Please try again later.")
                builder1.setCancelable(false)
                builder1.setPositiveButton("OK"
                ) { dialog, _ -> dialog.cancel() }

                val alert11 = builder1.create()
                alert11.show()
            }
            Log.i("ASSET_DETAIL", "Finished procesing hls file addedToQueue:$addedToQueue error:$aError")



        }
    }
}
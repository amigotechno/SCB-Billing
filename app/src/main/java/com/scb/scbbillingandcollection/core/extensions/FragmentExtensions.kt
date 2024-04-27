package com.scb.scbbillingandcollection.core.extensions

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.databinding.ItemSuccessToastBinding
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


fun Fragment.setToolbarStatus(@StringRes statusString: Int, @ColorRes color: Int) {
    (activity as AppCompatActivity).apply {
        setToolbarStatus(statusString, color)
    }
}

fun <T> Fragment.observerState(state: StateFlow<T>, onLatest: (T) -> Unit) {
    lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.collectLatest {
                onLatest(it)
            }
        }
    }
}

fun <T> Fragment.observerChannelFlow(state: Channel<T>, onLatest: (T) -> Unit) {
    lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.consumeEach {
                onLatest(it)
            }
        }
    }
}

fun <T> Flow<T>.collectOn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    onCollect: (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collectLatest {
                onCollect(it)
            }
        }
    }
}

fun <T> BottomSheetDialogFragment.observerState(state: StateFlow<T>, onLatest: (T) -> Unit) {
    lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.collectLatest {
                onLatest(it)
            }
        }
    }
}

fun <T> Fragment.observerOnResumeState(state: StateFlow<T>, onLatest: (T) -> Unit) {
    lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state.collectLatest {
                onLatest(it)
            }
        }
    }
}

fun <T> Fragment.observerSharedFlow(state: SharedFlow<T>, onLatest: (T) -> Unit) {
    lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.collectLatest {
                onLatest(it)
            }
        }
    }
}

fun Fragment.showCustomToast(
    @DrawableRes icon: Int = -1,
    title: String = "",
    description: String = ""
) {
    try {
        val snackBar = view?.let { Snackbar.make(requireContext(), it, "", Snackbar.LENGTH_LONG) }
        val toastBinding: ItemSuccessToastBinding = ItemSuccessToastBinding.inflate(layoutInflater)
        if (icon != -1) toastBinding.ivIcon.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(), icon
            )
        )
        toastBinding.apply {

            var titleMsg = title
            if (title.contains(Constants.UNABLE_TO_RESOLVE_HOST)) {
                titleMsg = resources.getString(R.string.no_internet_message)
            }
            tvTitle.text = titleMsg
            tvTitle.isVisible = titleMsg.isNotEmpty()
            var descMsg = description
            if (description.contains(Constants.UNABLE_TO_RESOLVE_HOST)) {
                descMsg = resources.getString(R.string.no_internet_message)
            }
            tvDescription.text = descMsg
            tvDescription.isVisible = descMsg.isNotEmpty()
        }
        if (snackBar?.view is CoordinatorLayout) {
            val snackBarLayout = snackBar.view as CoordinatorLayout
            val params = snackBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            params.gravity = Gravity.BOTTOM
            params.marginEnd = convertDpToPixels(requireContext(), 16f).toInt()
            snackBarLayout.setPadding(0, 0, 0, 0)
            snackBarLayout.addView(toastBinding.root)
        } else {
            val snackBarLayout = snackBar?.view as FrameLayout
//            val params = snackBarLayout.layoutParams as FrameLayout.LayoutParams
//            params.gravity = Gravity.BOTTOM
//            params.marginStart = convertDpToPixels(requireContext(), 16f).toInt()
//            params.marginEnd = convertDpToPixels(requireContext(), 16f).toInt()
            snackBarLayout.setPadding(0, 0, 0, 0)
            snackBarLayout.addView(toastBinding.root)
        }
        snackBar.view.translationY = convertDpToPixels(requireContext(), -40f)
        snackBar.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun convertDpToPixels(context: Context, dp: Float): Float {
    return dp * context.resources.displayMetrics.density
}

fun Context.showDialog(
    title: String?,
    description: String,
    titleOfPositiveButton: String? = null,
    titleOfNegativeButton: String? = null,
    note: String? = null,
    @ColorRes positiveTextColor: Int = android.R.color.holo_red_dark,
    positiveButtonFunction: (() -> Unit)? = null,
    negativeButtonFunction: (() -> Unit)? = null
): Dialog {
    val dialog = Dialog(this)
    dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(true)
    dialog.setContentView(R.layout.layout_custom_dialog)
    val dialogTitle = dialog.findViewById(R.id.title) as MaterialTextView
    val dialogDescription = dialog.findViewById(R.id.description) as MaterialTextView
    val dialogPositiveButton = dialog.findViewById(R.id.positiveButton) as MaterialTextView
    val dialogNegativeButton = dialog.findViewById(R.id.negativeButton) as MaterialTextView
    val dialogNoteCardView =
        dialog.findViewById<MaterialCardView>(R.id.custom_dialog_cv_note_layout)
    val dialogNoteTextView = dialog.findViewById<MaterialTextView>(R.id.custom_dialog_tv_note)
    dialogTitle.isVisible = title != null
    dialogNoteCardView.isVisible = note != null
    note?.let {
        dialogNoteTextView.text = it
    }
    dialogPositiveButton.isVisible = titleOfPositiveButton != null
    dialogNegativeButton.isVisible = titleOfNegativeButton != null
    dialogPositiveButton.setTextColor(ContextCompat.getColor(this, positiveTextColor))
    title?.let {
        dialogTitle.text = title
        dialogTitle.visibility = View.VISIBLE
    }
    dialogDescription.text = description
    titleOfPositiveButton?.let {
        dialogPositiveButton.text = it
    }
    titleOfNegativeButton?.let {
        dialogNegativeButton.text = it
    }
    dialogPositiveButton.clickWithDebounce {
        positiveButtonFunction?.invoke()
        dialog.dismiss()
    }
    dialogNegativeButton.clickWithDebounce {
        negativeButtonFunction?.invoke()
        dialog.dismiss()
    }
    dialog.show()
    return dialog
}


fun Fragment.downloadFile(uri: Uri?): Long {
    val downloadManager =
        requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(uri)
    request.setTitle(uri?.let { getFileNameFromURL(it) })
    request.setNotificationVisibility(
        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    )
    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        uri?.let { getFileNameFromURL(it) })
    return downloadManager.enqueue(request)
}

fun getFileNameFromURL(url: Uri): String {
    val fileName: String = url.toString()
    return fileName.substring(fileName.lastIndexOf('/') + 1)
}

fun Fragment.openDownloadFile(downloadID: Long, documentURI: Uri?) {
    try {
        val downloadManager =
            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val pdfUri = downloadManager.getUriForDownloadedFile(downloadID)
        openDownloadedFile(pdfUri)
    } catch (e: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/search?q=" + documentURI?.let {
                getFileType(it)
            } + "&c=apps")))
    }
}

fun Fragment.openDownloadedFile(pdfUri: Uri) {
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    val mime = requireContext().contentResolver.getType(pdfUri)
    intent.setDataAndType(pdfUri, mime)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    requireContext().startActivity(intent)
}

fun Context.openDownloadedFile(pdfUri: Uri) {
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    val mime = this.contentResolver.getType(pdfUri)
    intent.setDataAndType(pdfUri, mime)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    this.startActivity(intent)
}

fun Fragment.getFileType(uri: Uri): String? {
    // Check uri format to avoid null
    val extension: String? = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        // If scheme is a content
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(activity?.contentResolver?.getType(uri))
    } else {
        // If scheme is a File
        // This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
        MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    }
    return extension
}

fun Any.toCustomString(context: Context): String {
    if (this is String) return this
    else if (this is Int) return context.getString(this)
    return ""
}


fun Fragment.isLocationPermissionsEnabled(): Boolean {
    return isAllPermissionsGranted(permissions = getListOfLocationPermissions())
}

fun Fragment.isAllPermissionsGranted(permissions: List<String>): Boolean {
    permissions.forEach { permission ->
        if (ActivityCompat.checkSelfPermission(
                requireContext(), permission
            ) != PackageManager.PERMISSION_GRANTED
        ) return false
    }
    return true
}


fun getListOfLocationPermissions(
): List<String> {
    return listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    )
}

private fun getBackgroundPermissionList(): List<String> {
    return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        listOf(
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        )
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        arrayListOf()
    }
}

fun Context.takeUserToSettingsPage() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", this.packageName, null)
    intent.data = uri
    this.startActivity(intent)
}

fun NavController.isFragmentInBackStack(destinationId: Int) = try {
    getBackStackEntry(destinationId)
    true
} catch (e: Exception) {
    false
}

@Suppress("DEPRECATION")
fun Context.getNetworkType(): String {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = connectivityManager.activeNetwork ?: return "NO_NETWORK"
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return "NO_NETWORK"
        var network = ""
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) network += "TRANSPORT_WIFI, "
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) network += "TRANSPORT_CELLULAR, "
        // for other device how are able to connect with Ethernet
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) network += "TRANSPORT_ETHERNET, "
        // for check internet over Bluetooth
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) network += "TRANSPORT_BLUETOOTH, "
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) network += "TRANSPORT_VPN"
        if (network.isEmpty()) network += "NO_NETWORK"
        return network
    } else {
        return if (connectivityManager.activeNetworkInfo?.isConnected == true) "CONNECTED"
        else "NO_NETWORK"
    }
}

@Suppress("DEPRECATION")
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            // for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            // for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    } else {
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }
}


@Suppress("DEPRECATION")
@WorkerThread
fun Geocoder.getAddress(
    latitude: Double, longitude: Double, address: (Address?) -> Unit
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getFromLocation(latitude, longitude, 1) { address(it.firstOrNull()) }
            return
        }
        address(getFromLocation(latitude, longitude, 1)?.firstOrNull())
    } catch (e: Exception) {
        // will catch if there is an internet problem
        address(null)
    }
}

fun Address?.toAddressString(): String? {
    if (this?.maxAddressLineIndex == 0) return this.getAddressLine(0)
    return null
}

fun AlertDialog.showCompact() {
    if (!this.isShowing) this.show()
}

fun AlertDialog.dismissCompact() {
    if (this.isShowing) this.dismiss()
}

fun Context.isGpsEnabled(): Boolean {
    val mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Checking GPS is enabled
    return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}


fun Fragment.getColor(@ColorRes color: Int): Int {
    return ResourcesCompat.getColor(resources, color, null)
}

fun Context.getFile(fileUrl: Uri): File? {
    return try {
        if (fileUrl.toString().contains("https")) {
            val url = URL(fileUrl.toString())
            val urlConnection = url.openConnection() as HttpURLConnection
            val inputStream = urlConnection.inputStream
            return File(filesDir?.absolutePath + "sample1.pdf").apply {
                if (inputStream != null) {
                    copyInputStreamToFile(inputStream)
                }
            }
        } else {
            val inputStream = fileUrl.let {
                contentResolver?.openInputStream(
                    it
                )
            }
            return File(filesDir?.absolutePath + "sample2.pdf").apply {
                if (inputStream != null) {
                    copyInputStreamToFile(inputStream)
                }
            }
        }
    } catch (e: java.lang.Exception) {
        null
    }

}

fun File.copyInputStreamToFile(inputStream: InputStream) {
    try {
        this.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

}

fun Context.getFileType(uri: Uri): String? {
    // Check uri format to avoid null
    val extension = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        // If scheme is a content
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(contentResolver?.getType(uri))
    } else {
        // If scheme is a File
        // This will replace white spaces with %20 and also other special characters.
        // This will avoid returning null values on file name with spaces
        // and special characters.
        MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    }
    return extension
}


fun NavController.navigateCompact(directions: NavDirections) {
    try {
        this.navigate(directions)
    } catch (e: Exception) {
        //Implementation not required.
    }
}

fun Context.checkIfBackgroundPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.checkIfAllBackgroundPermissionsAreEnabled(): Boolean =
    ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && this.isGpsEnabled()
            && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else true

fun isValidEmail(email: String): Boolean {
    val pattern: Pattern = Patterns.EMAIL_ADDRESS
    return pattern.matcher(email).matches()
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun millisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
    val date = Date(millis)
    return sdf.format(date)
}
fun millisToTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:MM", Locale.getDefault())
    val date = Date(millis)
    return sdf.format(date)
}


package com.scb.scbbillingandcollection.collect_bill

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.scb.scbbillingandcollection.databinding.FragmentQRWebViewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QRWebViewFragment : Fragment() {

    private lateinit var binding: FragmentQRWebViewBinding
    private val args: QRWebViewFragmentArgs by navArgs()

    private val failUrl = "et-status/fail"
    private val successUrl = "get-status/success"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQRWebViewBinding.inflate(layoutInflater, container, false)

        binding.webview.webViewClient =
            WebViewClient()  // to open links clicked by the user within the WebView
        binding.webview.loadUrl(args.url)
        Log.d("TAG", "onCreateView: " + args.url)

        // Enable JavaScript (if the website requires it)
        val webSettings = binding.webview.settings
        webSettings.javaScriptEnabled = true

        binding.apply {
            webview.webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        val url = request?.url.toString()
                        if (url.contains(successUrl)) {
                            val number = url.substringAfterLast("/")
                            println(number)
                            view?.stopLoading()
                            return true
                        } else if (url.contains(failUrl)) {

                        }
                    }
                    return false
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                        if (url?.contains(successUrl) == true) {
                            val number = url.substringAfterLast("/")
                            println(number)
                            view?.stopLoading()
                            return true
                        } else if (url?.contains(failUrl) == true) {

                        }
                        view?.stopLoading()
                        return true
                    }

                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.isVisible = false
                }
            }
        }
        binding.progressBar.isVisible = true
        binding.webview.loadUrl(args.url)
        return binding.root
    }

}
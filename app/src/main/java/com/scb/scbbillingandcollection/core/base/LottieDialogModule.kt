package com.scb.scbbillingandcollection.core.base

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.keka.xhr.core.app.di.CustomDialogQualifier
import com.scb.scbbillingandcollection.databinding.ItemLoadingBinding
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
class LottieDialogModule {

    @Provides
    @ActivityScoped
    @CustomDialogQualifier
    fun provideLottieDialog(@ActivityContext context: Context): AlertDialog {
        val binding = ItemLoadingBinding.inflate(context.layoutInflater)
        val dialogBuilder = AlertDialog.Builder(context).run {
            setView(binding.root)
        }
        return dialogBuilder.create().apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    inline val Context.layoutInflater: LayoutInflater
        get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
}

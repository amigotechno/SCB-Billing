package com.scb.scbbillingandcollection.core.base

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.intuit.sdp.BuildConfig
import com.scb.scbbillingandcollection.auth.data.repository.AuthRepository
import com.scb.scbbillingandcollection.auth.data.repository.AuthRepositoryImpl
import com.scb.scbbillingandcollection.core.retrofit.ApiInterface
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepository
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): BaseApplication {
        return app as BaseApplication
    }

    @Provides
    @Singleton
    fun provideMasterKey(
        @ApplicationContext context: Context
    ): MasterKey {
        return MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    @Singleton
    @Provides
    fun providesOkHttpClient(appPreferences: AppPreferences): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()

        interceptor.apply {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("scbAuthKey", appPreferences.token)
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return if (BuildConfig.DEBUG) {
            Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.DE).client(okHttpClient).build()
        } else {
            Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.PROD_BASE_URL).client(okHttpClient).build()
        }

    }

    @Provides
    @Singleton
    fun provideSharedPreference(
        @ApplicationContext context: Context, masterKey: MasterKey, app: Application
    ): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        AppPreferences.ENCRYPTED_FILE_NAME, // fileName
        masterKey, // masterKeyAlias
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )


    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiInterface =
        retrofit.create(ApiInterface::class.java)

    @Provides
    @Singleton
    fun provideAuthRepo(
        apiInterface: ApiInterface
    ): AuthRepository = AuthRepositoryImpl(apiInterface)

    @Provides
    @Singleton
    fun provideGenerateBillRepo(
        apiInterface: ApiInterface
    ): GenerateBillRepository = GenerateBillRepositoryImpl(apiInterface)

}

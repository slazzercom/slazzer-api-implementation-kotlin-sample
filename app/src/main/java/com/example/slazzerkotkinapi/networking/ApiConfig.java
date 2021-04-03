package com.example.slazzerkotkinapi.networking;

/**
 * Created by delaroy on 6/7/18.
 */

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;


public interface ApiConfig {

    @Multipart
    @POST("v1/remove_image_background")
    Call<ResponseBody> upload(
            @Header("API-KEY") String authorization,
            @PartMap Map<String, RequestBody> map
    );
}

package com.gcyh.douyin.http.retrofit;


import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface ApiStores {
    /**
     * 首页
     *
     * @return
     */
    @GET
    Observable<String> getHtmlData(@Url String url);
    @GET
    Observable<ResponseBody> downloadFile(@Url String fileUrl);





}
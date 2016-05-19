package com.dqs.shangri.wxpay;

import okhttp3.Request;

/**
 * Created by Shangri on 2016/5/17.
 */
public class ProtoMaster {
    private  int a = 0;
    Request request = new Request.Builder()
            .url("http://publicobject.com/helloworld.txt")
            .build();
}

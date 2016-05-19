package com.cornapp.coolplay.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.cornapp.coolplay.pay.WXPayConfig;
import com.cornapp.coolplay.pay.WXPayEvent;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import de.greenrobot.event.EventBus;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final String TAG = "WXPayEntryActivity";

    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, WXPayConfig.WXPAY_APP_ID);
        api.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {

    }

    @Override
    public void onResp(BaseResp resp) {
        Log.d(TAG, "onPayFinish, errCode = " + resp.errCode);
        int type = resp.getType();
        if (type == ConstantsAPI.COMMAND_PAY_BY_WX) {
            WXPayEvent result = new WXPayEvent();
            if (resp.errCode == 0) {
                result.payResultCode = WXPayEvent.PAY_RESULT_SUCCESS;
            }
            if (resp.errCode == -1) {
                result.payResultCode = WXPayEvent.PAY_RESULT_FAIL;
                result.payResultMessage = resp.errStr;
            }
            if (resp.errCode == -2) {
                result.payResultCode = WXPayEvent.PAY_RESULT_CANCEL;
            }
            EventBus.getDefault().post(result);
        }
        finish();
    }
}
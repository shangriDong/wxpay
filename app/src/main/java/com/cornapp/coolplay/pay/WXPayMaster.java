package com.cornapp.coolplay.pay;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.cornapp.coolplay.R;
import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/*import org.apache.http.NameValuePair;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.message.BasicNameValuePair;*/
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.greenrobot.event.EventBus;

/**
 * 微信支付
 */
public class WXPayMaster {
    private static final String TAG = "WXPAY";
    private static final String WX_PAY_TYPE = "APP";

    private static final String WX_PREPAY_RETURN_CODE = "return_code";
    private static final String WX_PREPAY_RETURN_MSG = "return_msg";

    private static final String WX_PREPAY_RESULT_CODE = "result_code";

    private static final String WX_PREPAY_ERROR_CODE = "err_code";
    private static final String WX_PREPAY_ERROR_DES = "err_code_des";

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAIL";

    private Context m_Context;
    private IWXAPI m_WXapi;

    private String m_TotalFee;
    private String m_Summary;

    private Handler handler = new Handler();

    public WXPayMaster(Context context) {
        super();
        m_Context = context;
    }

    /**
     * 微信支付
     *
     * @param totalFee 订单总金额，单位为分
     * @param summary  商品或支付单简要描述[Ipad mini]
     */
    public void wxPay(long totalFee, String summary) {
        m_TotalFee = String.valueOf(totalFee);
        m_Summary = summary;
        m_WXapi = WXAPIFactory.createWXAPI(m_Context, WXPayConfig.WXPAY_APP_ID);

        GetPrepayIdTask getPrepayId = new GetPrepayIdTask();
        getPrepayId.execute();
    }

    private class GetPrepayIdTask extends AsyncTask<Void, Void, Map<String, String>> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Map<String, String> doInBackground(Void... params) {
            // 模拟服务端生成的预支付订单号和签名
            String prePayParams = buildPrePayParams();
            if (prePayParams == null) {
                Log.e("shangri", "prePayParams == null");
            }
            byte[] buf = WXPayUtil.httpPost(WXPayConfig.WXPAY_URL_PREPAYID, prePayParams);
            String prePayResult = new String(buf);
            Log.e("Shangri", "" + prePayResult);
            return decodeXml(prePayResult);
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            if (result == null) {
                cancelWXPay(R.string.wxpay_err_prepay_result_parser);
                return;
            }

            String returnCode = result.get(WX_PREPAY_RETURN_CODE);
            if (RESULT_FAILURE.equals(returnCode)) {
                String resultMsg = result.get(WX_PREPAY_RETURN_MSG);
                if (TextUtils.isEmpty(resultMsg)) {
                    cancelWXPay(R.string.wxpay_err_prepay_sign);
                    Log.e("shangri", "1");
                } else {
                    cancelWXPay(resultMsg);
                    Log.e("shangri", "2");
                }
                return;
            }

            String resultCode = result.get(WX_PREPAY_RESULT_CODE);
            if (RESULT_SUCCESS.equals(returnCode) && RESULT_FAILURE.equals(resultCode)) {
                String errCode = result.get(WX_PREPAY_ERROR_CODE);
                String errDesc = result.get(WX_PREPAY_ERROR_DES);
                if (TextUtils.isEmpty(errDesc)) {
                    String errorCodeTemplate = m_Context.getResources().getString(R.string.wxpay_err_prepay_code);
                    String errorCodeContent = String.format(errorCodeTemplate, errCode);
                    cancelWXPay(errorCodeContent);
                    Log.e("shangri", "3");
                } else {
                    cancelWXPay(errDesc);
                    Log.e("shangri", "4");
                }
                return;
            }

            if (RESULT_SUCCESS.equals(returnCode) && RESULT_SUCCESS.equals(resultCode)) {
                PayReq req = new PayReq();
                req.appId = WXPayConfig.WXPAY_APP_ID;
                req.partnerId = WXPayConfig.WXPAY_MIC_ID;
                req.prepayId = result.get("prepay_id");
                req.packageValue = "Sign=WXPay";
                req.nonceStr = buildNonceStr();
                req.timeStamp = String.valueOf(buildTimeStamp());

                /*List<NameValuePair> signParams = new LinkedList<NameValuePair>();
                signParams.add(new BasicNameValuePair("appid", req.appId));
                signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
                signParams.add(new BasicNameValuePair("package", req.packageValue));
                signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
                signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
                signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));*/
                ContentValues signParams = new ContentValues();
                signParams.put("appid", req.appId);
                signParams.put("noncestr", req.nonceStr);
                signParams.put("package", req.packageValue);
                signParams.put("partnerid", req.partnerId);
                signParams.put("prepayid", req.prepayId);
                signParams.put("timestamp", req.timeStamp);

                //signParams.add(signParams);

                req.sign = buildPrePaySign(signParams);

                m_WXapi.registerApp(WXPayConfig.WXPAY_APP_ID);
                //调起微信支付
                m_WXapi.sendReq(req);
                m_WXapi.unregisterApp();
                m_WXapi.detach();
            }
        }
    }

    private long buildTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }

    private String buildPrePayParams() {
        //List<ContentValues> packageParams = new LinkedList<ContentValues>();
        //List<ContentValues> signParams = new LinkedList<ContentValues>();

        ContentValues packageParams = new ContentValues();
        packageParams.put("appid", WXPayConfig.WXPAY_APP_ID);
        packageParams.put("body", m_Summary); //商品名
        packageParams.put("mch_id", WXPayConfig.WXPAY_MIC_ID);
        packageParams.put("nonce_str", buildNonceStr());
        packageParams.put("notify_url", WXPayConfig.WXPAY_URL_CALLBACK);
        packageParams.put("out_trade_no", buildOrderNo());
        packageParams.put("spbill_create_ip", getLocalIPv4Address());
        packageParams.put("total_fee", m_TotalFee);
        packageParams.put("trade_type", WX_PAY_TYPE);
        packageParams.put("sign", buildPrePaySign(packageParams));
        /*packageParams.add(new BasicNameValuePair("appid", WXPayConfig.WXPAY_APP_ID));
        packageParams.add(new BasicNameValuePair("body", m_Summary));
        packageParams.add(new BasicNameValuePair("mch_id", WXPayConfig.WXPAY_MIC_ID));
        packageParams.add(new BasicNameValuePair("nonce_str", buildNonceStr()));
        packageParams.add(new BasicNameValuePair("notify_url", WXPayConfig.WXPAY_URL_CALLBACK));
        packageParams.add(new BasicNameValuePair("out_trade_no", buildOrderNo()));
        packageParams.add(new BasicNameValuePair("spbill_create_ip", getLocalIPv4Address()));
        packageParams.add(new BasicNameValuePair("total_fee", m_TotalFee));
        packageParams.add(new BasicNameValuePair("trade_type", WX_PAY_TYPE));
        packageParams.add(new BasicNameValuePair("sign", buildPrePaySign(packageParams)));*/

        String xmlstring = buildXml(packageParams);
        try {
            return new String(xmlstring.toString().getBytes(), "ISO8859-1");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> decodeXml(String content) {
        try {
            Map<String, String> result = new HashMap<String, String>();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(content));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String nodeName = parser.getName();
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if ("xml".equals(nodeName) == false) {
                            result.put(nodeName, parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                event = parser.next();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildNonceStr() {
        Random random = new Random();
        return MD5.getMessageDigest(String.valueOf(random.nextInt(10000)).getBytes());
    }

    private String buildPrePaySign(ContentValues params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> item : params.valueSet()) {
            /*sb.append(params.get(i).getName());
            sb.append('=');
            sb.append(params.get(i).getValue());
            sb.append('&');*/
            sb.append(item.getKey());
            sb.append('=');
            sb.append(item.getValue().toString());
            sb.append('&');
        }


        Log.d("SING", "参数拼接:" + sb.toString());
        sb.append("key=");
        sb.append(WXPayConfig.WXPAY_APP_KEY);
        Log.d("SING", "参数拼接APPKEY:" + sb.toString());
        String packageSign = MD5.getMessageDigest(sb.toString().getBytes()).toUpperCase();
        Log.d("SING", "MD5:" + packageSign.toString());
        return packageSign;
    }

    private String buildXml(ContentValues params) {
        Log.e("shangri", params.size() + "");
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (Map.Entry<String, Object> item : params.valueSet()) {
            sb.append("<" + item.getKey() + ">");
            sb.append(item.getValue().toString());
            sb.append("</" + item.getKey() + ">");
        }
        sb.append("</xml>");
        Log.e("shangri", sb.toString());
        return sb.toString();
    }

    private boolean isWXAppInstalled(Context context, IWXAPI api) {
        return api.isWXAppInstalled();
    }

    private boolean isWXPaySupported(Context context, IWXAPI api) {
        return api.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
    }

    private String buildOrderNo() {
        return PayActivity.ORDER_NO;
//        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public String getLocalIPv4Address() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address/*&& Inet4Address.isIPv4Address(inetAddress.getHostAddress())*/) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public void showToast(String content) {
        Toast.makeText(m_Context, content, Toast.LENGTH_LONG).show();
    }

    private void cancelWXPay(String message) {
        WXPayEvent result = new WXPayEvent();
        result.payResultCode = WXPayEvent.PAY_RESULT_FAIL;
        if (!TextUtils.isEmpty(message)) {
            result.payResultMessage = message;
        }
        EventBus.getDefault().post(result);
    }

    private void cancelWXPay(int messageId) {
        String string = m_Context.getResources().getString(messageId);
        cancelWXPay(string);
    }
}

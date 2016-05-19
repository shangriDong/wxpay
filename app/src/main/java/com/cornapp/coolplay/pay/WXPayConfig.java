package com.cornapp.coolplay.pay;

public class WXPayConfig {
    // 微信官网apk下载链接
    public static final String URL_DOWNLOAD_WECHAT = "http://weixin.qq.com/cgi-bin/download302?check=false&uin=&stype=&promote=&fr=www.baidu.com&lang=zh_CN&ADTAG=&url=android16";
    // 预支付账单号
    public static final String WXPAY_URL_PREPAYID = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    // 支付结果通知后台回调接口
    public static final String WXPAY_URL_CALLBACK = "http://www.weixin.qq.com/wxpay/pay.php";
    // 微信开放平台 　　支付APP_ID
    public static final String WXPAY_APP_ID = "wx1e329d68c9d14d04";
    // 微信开放平台 　　支付APP_KEY
    public static final String WXPAY_APP_KEY = "bjnckjyxgsflqxwyhlbzqcgjhyhtzy12";
    // 微信开放平台 　　分配商户id
    public static final String WXPAY_MIC_ID = "1249594601";
}

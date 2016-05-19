package com.cornapp.coolplay.pay;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cornapp.coolplay.R;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.util.UUID;

import de.greenrobot.event.EventBus;

public class PayActivity extends Activity {

    private Button appayBtn;
    private Button orderBtn;

    private Context context;
    private Button verNotSupportBtn;
    private Button notInstallBtn;

    static boolean isWXSupportPay = true;
    static boolean isWXInstall = true;
    private EditText et_title;
    private EditText et_fee;
    private EditText order_no;

    private WXPayMaster wxPayMaster;

    public static String ORDER_NO;
    private Dialog systemTipDialog;
    private Dialog waitingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay);
        EventBus.getDefault().register(this);
        context = this;

        wxPayMaster = new WXPayMaster(getApplicationContext());

        initView();
        bindView();

        initOrderNo();
    }

    private void initOrderNo() {
        ORDER_NO = UUID.randomUUID().toString().replace("-", "");
        Log.d("WXPAY", ORDER_NO);
        order_no.setText(ORDER_NO);
    }

    private void initView() {
        et_title = (EditText) findViewById(R.id.et_title);
        order_no = (EditText) findViewById(R.id.order_no);
        et_fee = (EditText) findViewById(R.id.et_fee);
        verNotSupportBtn = (Button) findViewById(R.id.ver_not_support_btn);
        notInstallBtn = (Button) findViewById(R.id.not_install_btn);
        appayBtn = (Button) findViewById(R.id.appay_btn);
        orderBtn = (Button) findViewById(R.id.order_btn);
    }

    private void bindView() {
        orderBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ORDER_NO = UUID.randomUUID().toString().replace("-", "");
                Log.d("WXPAY", ORDER_NO);
                order_no.setText(ORDER_NO);
            }
        });

        verNotSupportBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isWXSupportPay) {
                    verNotSupportBtn.setText("模拟版本 -- 不支持支付");
                    isWXSupportPay = false;
                } else {
                    verNotSupportBtn.setText("模拟版本 -- 支持支付");
                    isWXSupportPay = true;
                }
            }
        });

        notInstallBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isWXInstall) {
                    notInstallBtn.setText("模拟安装 -- 未安装");
                    isWXInstall = false;
                } else {
                    notInstallBtn.setText("模拟安装 -- 已安装");
                    isWXInstall = true;
                }
            }
        });

        appayBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String title = et_title.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    showToast(R.string.wxpay_err_pay_info_empty);
                    return;
                }

                String fee = et_fee.getText().toString().trim();
                if (TextUtils.isEmpty(fee)) {
                    showToast(R.string.wxpay_err_pay_fee_empty);
                    return;
                }

                long totalFee = Long.parseLong(fee);
                if (totalFee <= 0) {
                    showToast(R.string.wxpay_err_total_fee);
                    return;
                }

                String m_TotalFee = String.valueOf(totalFee);
                if (TextUtils.isEmpty(m_TotalFee)) {
                    showToast(R.string.wxpay_err_pay_info_empty);
                    return;
                }

                String localIp = wxPayMaster.getLocalIPv4Address();
                if (TextUtils.isEmpty(localIp)) {
                    showToast(R.string.wxpay_err_net_disconn);
                    return;
                }

                // if (!isWXAppInstalled(m_Context, m_WXapi)) {
                if (!PayActivity.isWXInstall) {
                    showNotInstalledDialog();
                    return;
                }

                // if (!isWXPaySupported(m_Context, m_WXapi)) {
                if (!PayActivity.isWXSupportPay) {
                    showVersionNotSupportDialog();
                    return;
                }

                showWaitingDialog();
                wxPayMaster.wxPay(totalFee, title);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(WXPayEvent result) {
        hideWaitingDialog();
        Log.d("WXPayEvent", result.toString());
        if (result.payResultCode == WXPayEvent.PAY_RESULT_SUCCESS) {
            showToast("支付成功");
            return;
        }
        if (result.payResultCode == WXPayEvent.PAY_RESULT_CANCEL) {
            showToast("取消支付");
            return;
        }
        if (result.payResultCode == WXPayEvent.PAY_RESULT_FAIL) {
            if (!TextUtils.isEmpty(result.payResultMessage)) {
                Toast.makeText(context, result.payResultMessage, Toast.LENGTH_SHORT).show();
            } else {
                showToast("系统错误,请重试");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideWaitingDialog();
    }

    OnClickListener confirmListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent downloadWXIntent = new Intent(Intent.ACTION_VIEW);
            downloadWXIntent.setData(Uri.parse(WXPayConfig.URL_DOWNLOAD_WECHAT));
            startActivity(downloadWXIntent);
            dialogDismiss(systemTipDialog);
        }
    };

    private void showNotInstalledDialog() {
        String content = getResources().getString(R.string.wxpay_err_not_install_wechat);
        String canceltext = getResources().getString(R.string.wxpay_tip_cancel);
        String confirmtext = getResources().getString(R.string.wxpay_tip_confirm_download);
        OnClickListener cancelListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDismiss(systemTipDialog);
            }
        };
        systemTipDialog = buildSystemTipDialog(this, content, canceltext, cancelListener, confirmtext, confirmListener);
        systemTipDialog.show();
    }

    private void showVersionNotSupportDialog() {
        String content = getResources().getString(R.string.wxpay_err_version_not_support);
        String canceltext = getResources().getString(R.string.wxpay_tip_cancel);
        String confirmtext = getResources().getString(R.string.wxpay_tip_confirm_updatever);
        OnClickListener cancelListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDismiss(systemTipDialog);
            }
        };
        systemTipDialog = buildSystemTipDialog(this, content, canceltext, cancelListener, confirmtext, confirmListener);
        systemTipDialog.show();
    }

    public Dialog buildSystemTipDialog(Context context, String content, String canceltext, OnClickListener cancelListener, String confirmtext, OnClickListener confirmListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogPayTipView = inflater.inflate(R.layout.dialog_pay_tip, null);
        LinearLayout layout = (LinearLayout) dialogPayTipView.findViewById(R.id.pay_dialog_root);

        TextView errorTipTV = (TextView) dialogPayTipView.findViewById(R.id.show_device_error_tip);
        errorTipTV.setText(content);

        TextView cancelTV = (TextView) dialogPayTipView.findViewById(R.id.wxpay_dialog_cancel);
        cancelTV.setText(canceltext);
        cancelTV.setOnClickListener(cancelListener);

        TextView confirmTV = (TextView) dialogPayTipView.findViewById(R.id.wxpay_dialog_confirm);
        confirmTV.setText(confirmtext);
        confirmTV.setOnClickListener(confirmListener);

        Dialog systemTipDialog = new Dialog(context, R.style.pay_dialog);
        systemTipDialog.setCancelable(false);
        systemTipDialog.setContentView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return systemTipDialog;
    }

    private void dialogDismiss(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void showToast(int resStringId) {
        String content = getResources().getString(resStringId);
        showToast(content);
    }

    public void showToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    public Dialog buildLoadingDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View payDialogView = inflater.inflate(R.layout.dialog_pay_loading, null);
        LinearLayout layout = (LinearLayout) payDialogView.findViewById(R.id.dialog_view);
        ImageView spaceshipImage = (ImageView) payDialogView.findViewById(R.id.img);
        TextView tipTextView = (TextView) payDialogView.findViewById(R.id.tipTextView);
        tipTextView.setText(msg);

        Animation loadingAnim = AnimationUtils.loadAnimation(context, R.anim.pay_loading);
        spaceshipImage.startAnimation(loadingAnim);

        Dialog loadingDialog = new Dialog(context, R.style.pay_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return loadingDialog;
    }

    public void showWaitingDialog() {
        if (waitingDialog == null) {
            String loadingTip = getResources().getString(R.string.wxpay_tip_loading);
            waitingDialog = buildLoadingDialog(this, loadingTip);
        }
        if (!waitingDialog.isShowing()) {
            waitingDialog.show();
        }
    }

    public void hideWaitingDialog() {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
//            long duration = m_StopTimestamp - m_StartTimestamp;
//            Log.d(TAG, "start:" + m_StartTimestamp + "  stop:" + m_StopTimestamp + " duration:" + duration);
//            if (duration >= m_SendRequestTimeOut) {
//                showToast(R.string.wxpay_err_pay_timeout);
//            }
            waitingDialog = null;
        }
    }
}

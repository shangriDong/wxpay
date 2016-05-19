package com.cornapp.coolplay.pay;

public class WXPayEvent {
	public static final int PAY_RESULT_SUCCESS = 0;
	public static final int PAY_RESULT_CANCEL = 1;
	public static final int PAY_RESULT_FAIL = 2;

	public int payResultCode;
	public String payResultMessage;

	@Override
	public String toString() {
		return "WXPayEvent [payResultCode=" + payResultCode + ", payResultMessage=" + payResultMessage + "]";
	}

}

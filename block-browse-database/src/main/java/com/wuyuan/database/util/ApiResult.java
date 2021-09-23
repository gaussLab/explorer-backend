package com.wuyuan.database.util;

/**
 * Description:json
 *
 **/
public class ApiResult<T> {

	private int code;
	private String message;
	private T result;
	public ApiResult() {
		super();
	}
	public ApiResult(T t) {
		super();
		this.message="请求成功";
		this.code=200;
		this.result=t;
	}



	public ApiResult(String message, int statusCode) {
		super();
		this.code = statusCode;
		this.message = message;
	}

	public int getCode() {
		return code;
	}
	public ApiResult(int statusCode, String message, T data) {
		super();
		this.code = statusCode;
		this.message = message;
		this.result = data;
	}

	public ApiResult(int statusCode, String message) {
		super();
		this.code = statusCode;
		this.message = message;
	}
	public void setCode(int statusCode) {
		this.code = statusCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
}

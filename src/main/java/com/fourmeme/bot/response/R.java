package com.fourmeme.bot.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
public class R<T> implements Serializable {

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 0;
    /**
     * 失败状态码
     */
    public static final int FAIL_CODE = 1;
    /**
     * 成功消息
     */
    private static final String SUCCESS_MSG = "success";
    /**
     * 失败消息
     */
    private static final String FAIL_MSG = "failure";

    /**
     * 状态码
     */
    private int code;
    /**
     * 提示信息
     */
    private String msg;
    /**
     * 响应数据
     */
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public R(int code, String msg) {
        this(code, msg, null);
    }

    // ==================== 成功 ====================

    public static <T> R<T> success() {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    public static <T> R<T> success(T data) {
        return new R<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static <T> R<T> success(String msg, T data) {
        return new R<>(SUCCESS_CODE, msg, data);
    }

    // ==================== 失败 ====================

    public static <T> R<T> failure() {
        return new R<>(FAIL_CODE, FAIL_MSG, null);
    }

    public static <T> R<T> failure(String msg) {
        return new R<>(FAIL_CODE, msg, null);
    }

    public static <T> R<T> failure(String msg, T data) {
        return new R<>(FAIL_CODE, msg, data);
    }

    public static <T> R<T> failure(int code, String msg) {
        return new R<>(code, msg, null);
    }

    @Override
    public String toString() {
        return "R{" + "code=" + code + ", msg='" + msg + '\'' + ", data=" + data + '}';
    }
}
package com.ubtrobot.transport.message;

import android.support.annotation.Nullable;

import java.util.Map;

/**
 * 调用应答器
 */
public interface Responder {

    /**
     * 获取调用请求
     *
     * @return 调用请求
     */
    Request getRequest();

    /**
     * 应答（粘滞）调用请求（最终）的成功响应，无参数情况
     */
    void respondSuccess();

    /**
     * 应答（粘滞）调用请求（最终）的成功响应，有参数情况
     *
     * @param param 响应参数，可为空
     */
    void respondSuccess(@Nullable Param param);

    /**
     * 应答（粘滞）调用请求（最终）的失败响应
     *
     * @param code    错误码
     * @param message 错误消息
     */
    void respondFailure(int code, String message);

    /**
     * 应答（粘滞）调用请求（最终）的失败响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param detail  错误详情，可填充键值对
     */
    void respondFailure(int code, String message, @Nullable Map<String, String> detail);

    /**
     * 应答粘滞调用请求的中间状态
     *
     * @param param 中间状态的参数
     */
    void respondStickily(Param param);

    /**
     * 设置调用者取消调用的监听器
     *
     * @param listener 调用取消的监听器
     */
    void setCallCancelListener(CallCancelListener listener);
}
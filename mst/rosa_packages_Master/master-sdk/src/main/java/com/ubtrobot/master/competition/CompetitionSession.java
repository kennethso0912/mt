package com.ubtrobot.master.competition;

import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.master.service.ServiceProxy;

/**
 * 竞争会话
 */
public interface CompetitionSession {

    /**
     * 获取 Master 上下文
     *
     * @return Master 上下文
     */
    MasterContext getContext();

    /**
     * 将竞争者添加到当前会话参与竞争
     *
     * @param competing 竞争者
     * @return 当前会话
     */
    CompetitionSession addCompeting(Competing competing);

    boolean containsCompeting(Competing competing);

    CompetitionSession removeCompeting(Competing competing);

    /**
     * 注册会话打断监听
     *
     * @param listener 监听
     */
    void registerInterruptionListener(InterruptionListener listener);

    /**
     * 注销会话打断监听
     *
     * @param listener 监听
     */
    void unregisterInterruptionListener(InterruptionListener listener);

    /**
     * 设置激活选项
     *
     * @param option 选项
     * @return 当前会话
     */
    CompetitionSession setActivateOption(ActivateOption option);

    /**
     * 激活当前会话
     *
     * @param callback 激活回调
     */
    void activate(ActivateCallback callback);

    /**
     * 是否已激活
     *
     * @return 激活返回 true，否则返回 false
     */
    boolean isActive();

    /**
     * 是否被打断
     *
     * @return 已打断返回 true，否则返回 false
     */
    boolean isInterrupted();

    /**
     * 获取会话 id
     *
     * @return 会话 id
     */
    String getSessionId();

    /**
     * 创建 MasterSystemService (系统服务) 的访问代理，从而访问该系统服务
     *
     * @param serviceName 系统服务名称
     * @return 服务访问代理
     */
    ServiceProxy createSystemServiceProxy(String serviceName);

    /**
     * 创建 MasterService (普通服务) 的访问代理，从而访问该服务
     *
     * @param packageName 服务所在包的包名
     * @param serviceName 服务名称
     * @return 服务访问代理
     */
    ServiceProxy createServiceProxy(String packageName, String serviceName);

    void deactivate();
}
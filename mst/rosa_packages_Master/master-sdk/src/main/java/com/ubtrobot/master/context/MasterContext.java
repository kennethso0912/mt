package com.ubtrobot.master.context;

import com.ubtrobot.master.competition.CompetitionSession;
import com.ubtrobot.master.event.EventReceiver;
import com.ubtrobot.master.service.ServiceProxy;

/**
 * Created by column on 17-12-6.
 */

public interface MasterContext {

    /**
     * 动态订阅 MasterService 发布的事件
     *
     * @param receiver 事件接收者
     * @param action   事件类型
     */
    void subscribe(EventReceiver receiver, String action);

    /**
     * 取消订阅 MasterService 发布的事件
     *
     * @param receiver 事件接收者
     */
    void unsubscribe(EventReceiver receiver);

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

    /**
     * 启动当前 Package 内的某个服务
     *
     * @param service 服务名称
     */
    void startMasterService(String service);

    /**
     * 打开竞争会话
     *
     * @return 竞争会话
     */
    CompetitionSession openCompetitionSession();
}
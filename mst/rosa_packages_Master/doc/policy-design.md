# 策略服务 - 方案设计

#### 背景

* 机器人 Android 系统中，统一采用基于“机器人服务总线”的多进程架构。围绕总线进程，分布着多个服务和应用的进程
* 对于总线进程，所有机器人的 Android 系统安装的是同一个总线 APK 包
* 但对于不同的机器人，应用通过总线调用服务或服务通过总线调用应用时，由于产品差异总线需要执行不同的决策逻辑
* 为保证总线进程的通用性，将上述决策逻辑放置到独立的“策略服务”中，由各个机器人产品自行配置或代码实现

#### 策略服务职责

配合总线进程，完成以下 3 方面的策略管理：

* 状态管理
  * 服务在运行过程中，达到某个条件时将向总线发布某个状态。在这个状态下，某些服务将不可用或某些服务的部分调用将不可用。一般存在多个服务向总线发布状态，达到某个条件时状态也可由服务向总线清除
  * 决定在某些状态下哪些服务是否可用或哪些服务的哪些调用是否可用的过程称为状态管理
* 调用应用仲裁
  * 某些服务可能向连接到总线上的所有应用发起调用，而这个调用可能多个应用都能应答。决定哪个应用来应答由服务发起的某个调用的过程称为调用应用仲裁
* 服务调度
  * 某个机器人 Android 系统上，可能存在同一个服务的多个实现，比如：同时存在集成讯飞语音引擎的语音服务以及集成 Nuance 语音引擎的语音服务。决定哪个采用哪个服务实现的调用的过程称为服务调度

#### 交互过程设计

##### 1. 状态管理

![policy-state-manage](images/policy-state-manage.svg)

##### 2. 调用应用仲裁

![policy-application-arbitration](images/policy-application-arbitrate.svg)

##### 3. 服务调度

![policy-service-schedule](images/policy-service-schedule.svg)

##### 4. 统一说明

* 对于应用调用服务，总线内依次根据状态管理策略和服务调度策略选择性地转发调用到服务
* 对于服务调用应用，总线内只根据调用应用仲裁策略选择性地转发调用到应用
* 策略服务是标准的总线服务，只是由总线直接调用。该调用属于进程间调用，总线应该做好策略的缓存，避免不必要的调用，另外策略服务在策略变更的时候也应该通知总线清除缓存

#### 集成设计

* 在 Android Studio 中创建 Project
  * APK package name 必须为 com.ubtrobot.servicebus.policy
  * APK Application name 由各个产品自行决定，建议是 “${ProductName}Policy”
* 配置策略服务依赖库

```groovy
compile 'com.ubtrobot.servicebus:policy:${version}@aar'
```

* 实现 Android APK 的 Application

```java
// 以 com.ubtrobot.servicebus.policy.BasePolicyApplication 为基类实现 Application
// 在 AndroidManifest.xml 中配置 manifest.application#name，PolicyApplication 名称可自行决定
public class PolicyApplication extends BasePolicyApplication {

    // 如果需要在 Application 创建时执行逻辑，覆盖 onApplicationCreate
    @Override
    protected void onApplicationCreate() {
        // Your logic
    }
}
```

* 默认策略

  * 状态管理

    * APK 中 res/xml/state.xml 不存在时，任何些状态下应用都能调用服务
    * APK 中 res/xml/state.xml 存在时，依照下述配置管理状态

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <state-list>
        <!-- 服务发布的某个状态。name：状态名称，label：状态标签，用于用户交互，如语音提示，description：状态描述，也可用于用户交互-->
        <state name="ubtrobot.state.FOO_BAR" label="..." description="...">
            <!-- 某个状态下，能执行的服务调用很少，则配置白名单 -->
            <whitelist>
                <!--整个服务的调用都能执行-->
                <service name="service-a" />
                <!--服务中的部分调用能执行-->
                <service name="service-b">
                    <call path="/foo" />
                    <call path="/bar" />
                </service>
            </whitelist>
        </state>
        <state name="ubtrobot.state.BAZ_QUX" label="..." description="...">
            <!-- 某个状态下，不能执行的服务调用很少，则配置黑名单 -->
            <blacklist>
                <!--整个服务的调用都不能执行-->
                <service name="service-c" />
                <!--服务中的部分调用不能执行-->
                <service name="service-d">
                    <call path="/baz" />
                    <call path="/qux" />
                </service>
            </blacklist>
        </state>
        <state name="..." label="..." description="...">
            <!--黑白名单互斥，不能同时出现在统一个 state xml 元素下-->
        </state>
    </state-list>
    ```

  * 调用应用仲裁

    * APK 中 res/xml/arbitration.xml 不存在时，不进行仲裁，总线直接给服务返回调用应用冲突
    * APK 中 res/xml/arbitration.xml 存在时，依照下述配置进行仲裁

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <arbitration>
        <global>
        <!-- 全局仲裁策略，预留 -->
        </global>
        <call-list>
            <!-- 针对单个调用的仲裁策略，优先级高于全局仲裁策略 -->
            <call package="..." path="..." />
        </call-list>
    </arbitration>
    ```

  * 服务调度

    * APK 中 res/xml/scheduling.xml 不存在时，不进行调度，总线直接给应用返回调用服务冲突
    * APK 中 res/xml/scheduling.xml 存在时，依照下述配置进行调度

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <!-- 机制预留，具体支持哪些配置，根据产品实际需求来考虑 -->
    <scheduling>
        <global>
        </global>
        <service-list>
            <service name="..." package="..." />
            <service name="...">
                <call path="" package="..." />
            </service>
        </service-list>
    </scheduling>
    ```

* 覆盖默认策略

```java
public class PolicyApplication extends BasePolicyApplication {

    // 需要覆盖默认策略，则覆盖 createPolicy 方法，返回自身需要的策略实现
    @Override
    protected Policy createPolicy() {
        Policy defaultPolicy = super.createPolicy();
        // 可结合默认策略实现自身需要的策略

        return new PolicyAdapter() {
            @Override
            public Arbitration arbitrateApplicationCall(String callPath) {
                // 调用应用仲裁
            }

            @Override
            public StateBlacklist getCallStateBlacklist(String service, String callPath) {
                // 状态管理
            }

            @Override
            public Scheduling scheduleServiceCall(String service, String callPath) {
                // 服务调度
            }

            // 当某个策略发生变更时，可通过以下对应方法通知到总线
            // PolicyAdapter.notifyApplicationCallArbitrationChanged
            // PolicyAdapter.notifyCallStateBlacklistChanged
            // PolicyAdapter.notifyServiceCallSchedulingChanged
        };
    }
}
```
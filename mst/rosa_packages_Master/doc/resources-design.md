# 服务总线资源管理 - 方案设计

#### 背景

存在价值可共享出来供外部访问和使用的组件，可称为资源。机器人 Android 系统上基于服务总线的架构中，“服务”是资源的载体，其通过总线向“应用”或其他“服务”提供了资源操作的调用接口。

依托于服务的资源被多个访问者（应用和其他服务均可能）访问时，访问者之间可能存在竞争或协作的关系，这时必须要对资源的访问进行控制。系统中存在多个服务，而每个服务可能包含一至多个需要控制访问的资源。所有对服务的访问均通过服务总线，因此可以借助服务总线提供统一的管理机制控制资源访问。

#### 资源管理职责

资源管理职责规定如下：

1. 对服务访问者（应用和其他服务均可能）提供查询资源信息、申请资源、访问资源、释放资源的统一机制
2. 提供实现服务时注册资源的统一机制

#### 交互过程设计

![resource-manage](images/resource-manage.svg)

注：

* 仅设计独占型竞争资源的情况
* 服务可访问其他服务，将“ApplicationX”替换成“ServiceX”可相应理解

#### API 设计

###### 注册资源

```java
// 将服务的所有调用接口注册到某个或某些资源
@BusService(name = "service_name")
@Resource(name = "res_name")
// @Resource(name = "another_res_name") 可注册多个资源
public class YourCallable extends AbstractCallable {

    @Call(path = "/foo/bar")
    public void onFooBar(Request request, Responder responder) {
        // Some code
    }

    @Override
    public void onCall(Request request, Responder responder) {
         // Default process code
    }
}
```

```java
// 将服务的部分调用接口注册到某个或某些资源
@BusService(name = "service_name")
public class YourCallable extends AbstractCallable {

    // 在 @Call 注解的服务调用接口上注册资源
    @Call(path = "/foo/bar")
    @Resource(name = "res_name")
    // @Resource(name = "another_res_name") 可注册多个资源
    public void onFooBar(Request request, Responder responder) {
        // Some code
    }

    @Override
    public void onCall(Request request, Responder responder) {
         // Default process code
    }
}
```

###### 查询、申请、访问、释放资源

* 获取资源对象

```java
private void getResourceSampleCode() {
    // 获取服务内资源对象
    BusService service = ServiceBus.get().getBusService(serviceName);
    Resource serviceResource = service.getResource(resourceName);
}
```

* 通过资源对象查询、申请、访问、释放资源

```java
// Resource 接口定义
public interface Resource {

    // 查询资源被哪个 APK 持有
    String getHeldBy();

    // 查询当前 APK 是否持有资源
    boolean isHeldByMe();

    // 查询当前 APK 能否持有资源
    boolean canBeHeldByMe();

    // 查询某个 APK 能否持有资源
    boolean canBeHeldBy(String packageName);

    // 申请持有资源，可能阻塞，不能在主线程调用。timeout 时间内申请成功将返回，否则抛出异常
    // 申请成功后，才能调用资源对应的服务接口，否则调用失败
    void acquire(long timeout) throws AcquireException;
  
    // 异步申请持有资源，timeout 时间内申请成功将回调申请成功，否则回调申请失败
    // 申请成功后，才能调用资源对应的服务接口，否则调用失败
    void acquire(long timeout, AcquireCallback callback);

    // 尝试申请持有资源，不会阻塞。资源可用时，立即返回申请成功，否则立即返回申请失败
    void tryAcquire();

    // 释放资源
    void release();
}
```


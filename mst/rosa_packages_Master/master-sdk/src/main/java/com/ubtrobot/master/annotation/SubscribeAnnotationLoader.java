package com.ubtrobot.master.annotation;

import android.text.TextUtils;

import com.ubtrobot.master.context.MasterContext;
import com.ubtrobot.transport.message.Event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-11-13.
 */

public final class SubscribeAnnotationLoader {


    private static final HashMap<Class, Map<String, Method>> sClazzReceiveMethodsMap = new HashMap<>();

    private SubscribeAnnotationLoader() {
    }

    public static synchronized Map<String, Method> loadReceiveMethods(Class clazz) {
        Map<String, Method> receiveMethods = sClazzReceiveMethodsMap.get(clazz);
        if (receiveMethods != null) {
            return receiveMethods;
        }

        receiveMethods = new HashMap<>();
        sClazzReceiveMethodsMap.put(clazz, receiveMethods);


        StringBuilder receiveMethodsError = new StringBuilder();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) {
                continue;
            }

            if (TextUtils.isEmpty(annotation.action())) {
                receiveMethodsError.append("Method ");
                receiveMethodsError.append(method.getName());
                receiveMethodsError.append(" 's @Subscribe annotation has illegal action. Should NOT be empty.\n");
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[0].equals(MasterContext.class) &&
                    parameterTypes[1].equals(Event.class)) {
                receiveMethods.put(annotation.action(), method);
                continue;
            }

            receiveMethodsError.append("Method ");
            receiveMethodsError.append(method.getName());
            receiveMethodsError.append(" has unexpected parameter types. Should be (MasterContext context, Event event)\n");
        }

        if (receiveMethodsError.length() > 0) {
            throw new IllegalStateException(receiveMethodsError.toString());
        }

        return receiveMethods;
    }
}

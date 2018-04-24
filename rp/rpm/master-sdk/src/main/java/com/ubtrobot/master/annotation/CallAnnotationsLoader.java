package com.ubtrobot.master.annotation;

import android.text.TextUtils;

import com.ubtrobot.transport.message.Request;
import com.ubtrobot.transport.message.Responder;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 17-11-13.
 */

public final class CallAnnotationsLoader {

    private static final HashMap<Class, Map<String, Method>> sClazzCallMethodsMap = new HashMap<>();

    private CallAnnotationsLoader() {
    }

    public static synchronized Map<String, Method> loadCallMethods(Class clazz) {
        Map<String, Method> callMethods = sClazzCallMethodsMap.get(clazz);
        if (callMethods != null) {
            return callMethods;
        }

        callMethods = new HashMap<>();
        sClazzCallMethodsMap.put(clazz, callMethods);

        StringBuilder callMethodsError = new StringBuilder();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            Call annotation = method.getAnnotation(Call.class);
            if (annotation == null) {
                continue;
            }

            if (TextUtils.isEmpty(annotation.path()) || !annotation.path().startsWith("/")) {
                callMethodsError.append("Method ");
                callMethodsError.append(method.getName());
                callMethodsError.append(" 's @Call annotation has illegal path. Should NOT empty and start with \"/\"\n");
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 &&
                    parameterTypes[0].equals(Request.class) &&
                    parameterTypes[1].equals(Responder.class)) {
                callMethods.put(annotation.path(), method);
                continue;
            }

            callMethodsError.append("Method ");
            callMethodsError.append(method.getName());
            callMethodsError.append(" has unexpected parameter types. Should be (Request request, Responder responder)\n");
        }

        if (callMethodsError.length() > 0) {
            throw new IllegalStateException(callMethodsError.toString());
        }

        return callMethods;
    }
}
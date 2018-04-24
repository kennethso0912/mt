package com.ubtrobot.master.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by column on 17-9-9.
 */

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Call {

    String path();
}
package com.ubtrobot.master.sample.mstinteractor;

import android.app.Application;

import com.ubtrobot.master.Master;
import com.ubtrobot.ulog.LoggerFactory;
import com.ubtrobot.ulog.ULog;
import com.ubtrobot.ulog.logger.android.AndroidLoggerFactory;

/**
 * Created by column on 17-12-8.
 */

public class SampleInteractorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Master.initialize(this);

        // 初始化
        Master.initialize(this);

        LoggerFactory loggerFactory = new AndroidLoggerFactory();
        ULog.setup("MstInteractor", loggerFactory);

        // 配置 Master SDK 打印调试日志
        Master.get().setLoggerFactory(loggerFactory);
    }
}
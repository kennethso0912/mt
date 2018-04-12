package com.ubtrobot.master.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.component.CallInfo;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.component.StringResource;
import com.ubtrobot.master.component.validate.ComponentValidator;
import com.ubtrobot.master.component.validate.ValidateException;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.service.ServiceCallInfo;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.skill.SkillCallInfo;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillIntent;
import com.ubtrobot.master.skill.SkillIntentFilter;
import com.ubtrobot.ulog.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by column on 17-11-22.
 */

public class ComponentInfoPool {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("CallPool");

    private static final String EMPTY_PATH = "";

    private final Context mContext;
    private final EventLoop mEventLoop;

    // Key: packageName
    private final HashMap<String, List<? extends ComponentInfo>> mComponentInfos = new HashMap<>();
    // Key: uri (skill:///some/path | service://service_name/some/path)
    private final HashMap<String, List<CallInfo>> mUriCallInfoMap = new HashMap<>();
    private final HashMap<SkillIntent, List<SkillCallInfo>> mIntentCallInfoMap = new HashMap<>();

    private final ReadWriteLock mPoolLock = new ReentrantReadWriteLock();

    private final PackageReceiver mPackageReceiver = new PackageReceiver();

    public ComponentInfoPool(Context context, EventLoop eventLoop) {
        mContext = context;

        mEventLoop = eventLoop;
        mEventLoop.post(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });

        registerPackageReceiver();
    }

    private void registerPackageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mPackageReceiver, filter);
    }

    private void unregisterPackageReceiver() {
        mContext.unregisterReceiver(mPackageReceiver);
    }

    private void load() {
        List<PackageInfo> packageInfos = mContext.getPackageManager().
                getInstalledPackages(packageInfoFlags());

        mPoolLock.writeLock().lock();
        try {
            mUriCallInfoMap.clear();
            mIntentCallInfoMap.clear();

            for (PackageInfo packageInfo : packageInfos) {
                loadPackageCallInfosLocked(packageInfo);
            }
        } finally {
            mPoolLock.writeLock().unlock();
        }
    }

    private int packageInfoFlags() {
        return PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA |
                PackageManager.GET_SERVICES;
    }

    private void loadPackageCallInfosLocked(PackageInfo packageInfo) {
        ComponentValidator validator = new ComponentValidator(mContext, packageInfo);
        try {
            validator.validate();

            if (!validator.getSkillInfoList().isEmpty()) {
                mComponentInfos.put(packageInfo.packageName, validator.getSkillInfoList());
                loadSkillsCallInfosLocked(validator.getSkillInfoList());
            }


            if (!validator.getServiceInfoList().isEmpty()) {
                mComponentInfos.put(packageInfo.packageName, validator.getServiceInfoList());
                loadServicesCallInfosLocked(validator.getServiceInfoList());
            }
        } catch (ValidateException e) {
            if (ValidateException.CODE_NO_PERMISSION == e.getCode()) {
                return;
            }

            LOGGER.e(e, "Detect a skill or service has illegal configurations.");
        }
    }

    private void loadSkillsCallInfosLocked(List<SkillInfo> skillInfos) {
        for (SkillInfo skillInfo : skillInfos) {
            for (SkillCallInfo skillCallInfo : skillInfo.getCallInfoList()) {
                String uri = CallUri.createSkillCallUri(skillCallInfo.getPath());
                List<CallInfo> callInfos = mUriCallInfoMap.get(uri);

                if (callInfos == null) {
                    callInfos = new LinkedList<>();
                    mUriCallInfoMap.put(uri, callInfos);
                }

                callInfos.add(skillCallInfo);

                loadSkillIntentsLocked(skillCallInfo);
            }
        }
    }

    private void loadSkillIntentsLocked(SkillCallInfo skillCallInfo) {
        for (SkillIntentFilter intentFilter : skillCallInfo.getIntentFilterList()) {
            if (SkillIntentFilter.CATEGORY_SPEECH.equals(intentFilter.getCategory())) {
                for (StringResource stringResource : intentFilter.getUtteranceList()) {
                    // 处理多语言
                    SkillIntent skillIntent = new SkillIntent(intentFilter.getCategory()).
                            setSpeechUtterance(stringResource.getContent());
                    List<SkillCallInfo> callInfos = mIntentCallInfoMap.get(skillIntent);

                    if (callInfos == null) {
                        callInfos = new LinkedList<>();
                        mIntentCallInfoMap.put(skillIntent, callInfos);
                    }

                    callInfos.add(skillCallInfo);
                }
            }
        }
    }

    private void loadServicesCallInfosLocked(List<ServiceInfo> serviceInfos) {
        for (ServiceInfo serviceInfo : serviceInfos) {
            for (ServiceCallInfo callInfo : serviceInfo.getCallInfoList()) {
                putCallInfoLocked(callInfo);
            }

            putCallInfoLocked(new ServiceCallInfo(serviceInfo, EMPTY_PATH));
        }
    }

    private void putCallInfoLocked(CallInfo callInfo) {
        String uri = CallUri.createServiceCallUri(
                callInfo.getParentComponent().getName(), callInfo.getPath());
        List<CallInfo> callInfos = mUriCallInfoMap.get(uri);

        if (callInfos == null) {
            callInfos = new LinkedList<>();
            mUriCallInfoMap.put(uri, callInfos);
        }

        callInfos.add(callInfo);
    }

    public List<? extends ComponentInfo> getComponentInfos(String packageName) {
        mPoolLock.readLock().lock();
        try {
            List<? extends ComponentInfo> componentInfos = mComponentInfos.get(packageName);
            if (componentInfos == null) {
                componentInfos = new LinkedList<>();
            }

            return componentInfos;
        } finally {
            mPoolLock.readLock().unlock();
        }
    }

    public List<SkillCallInfo> getSkillCallInfos(String path) {
        mPoolLock.readLock().lock();
        try {
            LinkedList<SkillCallInfo> ret = new LinkedList<>();

            List<CallInfo> callInfos = mUriCallInfoMap.get(CallUri.createSkillCallUri(path));
            if (callInfos == null) {
                return ret;
            }

            for (CallInfo callInfo : callInfos) {
                ret.add((SkillCallInfo) callInfo);
            }

            return ret;
        } finally {
            mPoolLock.readLock().unlock();
        }
    }

    public List<SkillCallInfo> getSkillCallInfos(SkillIntent intent) {
        mPoolLock.readLock().lock();
        try {
            LinkedList<SkillCallInfo> ret = new LinkedList<>();

            List<SkillCallInfo> callInfos = mIntentCallInfoMap.get(intent);
            if (callInfos == null) {
                return ret;
            }

            for (CallInfo callInfo : callInfos) {
                ret.add((SkillCallInfo) callInfo);
            }

            return ret;
        } finally {
            mPoolLock.readLock().unlock();
        }
    }

    public List<ServiceCallInfo> getServiceCallInfos(String service, String path) {
        mPoolLock.readLock().lock();
        try {
            LinkedList<ServiceCallInfo> ret = new LinkedList<>();

            List<CallInfo> callInfos = mUriCallInfoMap.get(CallUri.createServiceCallUri(service, path));
            if (callInfos == null) {
                return ret;
            }

            for (CallInfo callInfo : callInfos) {
                ret.add((ServiceCallInfo) callInfo);
            }

            return ret;
        } finally {
            mPoolLock.readLock().unlock();
        }
    }

    public List<ServiceInfo> getServiceInfos(String service) {
        mPoolLock.readLock().lock();
        try {
            LinkedList<ServiceInfo> serviceInfos = new LinkedList<>();
            List<ServiceCallInfo> serviceCallInfos = getServiceCallInfos(service, EMPTY_PATH);
            for (ServiceCallInfo callInfo : serviceCallInfos) {
                serviceInfos.add((ServiceInfo) callInfo.getParentComponent());
            }

            return serviceInfos;
        } finally {
            mPoolLock.readLock().unlock();
        }
    }

    public void close() {
        unregisterPackageReceiver();
    }

    private void unloadPackageCallInfosLocked(String packageName) {
        mComponentInfos.remove(packageName);

        Iterator<Map.Entry<String, List<CallInfo>>> uriMapIterator = mUriCallInfoMap.entrySet().iterator();
        while (uriMapIterator.hasNext()) {
            Map.Entry<String, List<CallInfo>> entry = uriMapIterator.next();
            Iterator<CallInfo> listIterator = entry.getValue().iterator();

            while (listIterator.hasNext()) {
                CallInfo callInfo = listIterator.next();
                if (packageName.equals(callInfo.getParentComponent().getPackageName())) {
                    listIterator.remove();
                }
            }

            if (entry.getValue().isEmpty()) {
                uriMapIterator.remove();
            }
        }

        Iterator<Map.Entry<SkillIntent, List<SkillCallInfo>>> intentMapIterator =
                mIntentCallInfoMap.entrySet().iterator();
        while (intentMapIterator.hasNext()) {
            Map.Entry<SkillIntent, List<SkillCallInfo>> entry = intentMapIterator.next();
            Iterator<SkillCallInfo> listIterator = entry.getValue().iterator();

            while (listIterator.hasNext()) {
                SkillCallInfo callInfo = listIterator.next();
                if (packageName.equals(callInfo.getParentComponent().getPackageName())) {
                    listIterator.remove();
                }
            }

            if (entry.getValue().isEmpty()) {
                intentMapIterator.remove();
            }
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String packageName = intent.getDataString() == null ?
                    null : intent.getDataString().replace("package:", "");
            if (TextUtils.isEmpty(packageName)) {
                LOGGER.e("Package installed, uninstalled, or updated, but packageName is empty.");
                return;
            }

            mEventLoop.post(new Runnable() {
                @Override
                public void run() {
                    mPoolLock.writeLock().lock();
                    try {
                        unloadPackageCallInfosLocked(packageName);
                        loadPackageCallInfosLocked(mContext.getPackageManager().getPackageInfo(
                                packageName, packageInfoFlags()));
                    } catch (PackageManager.NameNotFoundException e) {
                        // Ignore. Uninstalled.
                    } finally {
                        mPoolLock.writeLock().unlock();
                    }
                }
            });
        }
    }
}
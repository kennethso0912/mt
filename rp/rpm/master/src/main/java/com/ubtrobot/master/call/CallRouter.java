package com.ubtrobot.master.call;

import android.support.annotation.Nullable;

import com.ubtrobot.concurrent.EventLoop;
import com.ubtrobot.master.component.ComponentInfo;
import com.ubtrobot.master.service.ServiceCallInfo;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.skill.SkillCallInfo;
import com.ubtrobot.master.skill.SkillIntent;
import com.ubtrobot.master.transport.message.parcel.AbstractParcelRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelImplicitRequest;
import com.ubtrobot.master.transport.message.parcel.ParcelRequest;
import com.ubtrobot.transport.connection.Connection;
import com.ubtrobot.transport.message.Request;

import java.util.List;
import java.util.Map;

/**
 * Created by column on 17-11-24.
 */

public class CallRouter {

    private final ComponentInfoPool mComponentInfoPool;
    private final CallDestinations mCallDestinations;

    public CallRouter(ComponentInfoPool componentInfoPool, CallDestinations callDestinations) {
        mComponentInfoPool = componentInfoPool;
        mCallDestinations = callDestinations;
    }

    public void routeSkill(
            EventLoop requestEventLoop,
            final ParcelRequest request,
            final RouteCallback<Request> callback) {
        requestEventLoop.post(new Runnable() {
            @Override
            public void run() {
                routeSkillInLoop(request, mComponentInfoPool.getSkillCallInfos(request.getPath()), callback);
            }
        });
    }

    private void routeSkillInLoop(
            AbstractParcelRequest request,
            List<SkillCallInfo> skillCallInfos,
            RouteCallback<Request> callback) {
        if (skillCallInfos.isEmpty()) {
            callback.onNotFound(request);
            return;
        }

        StringBuilder arbitrateMessage = new StringBuilder();
        SkillCallInfo skillCallInfo = arbitrateSkillCall(skillCallInfos, arbitrateMessage);

        if (skillCallInfo == null) {
            callback.onConflict(request);
            return;
        }

        Connection destinationConnection = mCallDestinations.getSkillCallDestination(
                skillCallInfo.getParentComponent().getPackageName(), skillCallInfo.getPath());
        if (request instanceof ParcelImplicitRequest) {
            ((ParcelImplicitRequest) request).setPath(skillCallInfo.getPath());
        }

        request.getContext().changeResponder(skillCallInfo.getParentComponent().getName());
        callback.onRoute(request, skillCallInfo.getParentComponent(), destinationConnection);
    }

    private SkillCallInfo arbitrateSkillCall(
            List<SkillCallInfo> skillCallInfos,
            StringBuilder outArbitrateMessage) {
        if (skillCallInfos.size() == 1) {
            return skillCallInfos.get(0);
        }

        if (skillCallInfos.isEmpty()) {
            throw new AssertionError("skillCallInfos is empty. Should NOT be here.");
        }

        SkillCallInfo systemSkill = null;
        for (SkillCallInfo skillCallInfo : skillCallInfos) {
            if (!skillCallInfo.getParentComponent().isSystemPackage()) {
                continue;
            }

            if (systemSkill == null) {
                systemSkill = skillCallInfo;
                continue;
            }

            // TODO 冲突，多个 System Skill 冲突
            return null;
        }

        if (systemSkill != null) {
            return systemSkill;
        }

        return null; // TODO 冲突，多个非 System Skill 冲突
    }

    public void routeSkill(
            EventLoop requestEventLoop,
            final ParcelImplicitRequest request,
            final RouteCallback<Request> callback) {
        Map<String, String> matchingRules = request.getMatchingRules();
        final SkillIntent intent = new SkillIntent(matchingRules.get("category"));
        intent.setSpeechUtterance(matchingRules.get("utterance"));

        requestEventLoop.post(new Runnable() {
            @Override
            public void run() {
                routeSkillInLoop(request, mComponentInfoPool.getSkillCallInfos(intent), callback);
            }
        });
    }

    public void routeService(
            EventLoop requestEventLoop,
            final ParcelRequest request,
            final RouteCallback<Request> callback) {
        requestEventLoop.post(new Runnable() {
            @Override
            public void run() {
                routeServiceInLoop(request, callback);
            }
        });
    }

    private void routeServiceInLoop(ParcelRequest request, RouteCallback<Request> callback) {
        String service = request.getContext().getResponder();

        List<ServiceCallInfo> serviceCallInfos = mComponentInfoPool.getServiceCallInfos(
                service, request.getPath());
        if (serviceCallInfos.isEmpty()) {
            callback.onNotFound(request);
            return;
        }

        if (serviceCallInfos.size() > 1) {
            // TODO 做服务实现选择
            callback.onConflict(request);
            return;
        }

        ServiceCallInfo serviceCallInfo = serviceCallInfos.get(0);
        Connection destinationConnection = mCallDestinations.getServiceCallDestination(
                serviceCallInfo.getParentComponent().getPackageName(), service);
        callback.onRoute(request, serviceCallInfo.getParentComponent(), destinationConnection);
    }

    public void routeService(
            EventLoop requestEventLoop,
            final String service,
            final RouteCallback<String> callback) {
        requestEventLoop.post(new Runnable() {
            @Override
            public void run() {
                routeServiceInLoop(service, callback);
            }
        });
    }

    private void routeServiceInLoop(String service, RouteCallback<String> callback) {
        List<ServiceInfo> serviceInfos = mComponentInfoPool.getServiceInfos(service);
        if (serviceInfos.isEmpty()) {
            callback.onNotFound(service);
            return;
        }

        if (serviceInfos.size() > 1) {
            // TODO 做服务实现选择
            callback.onConflict(service);
            return;
        }

        ServiceInfo serviceInfo = serviceInfos.get(0);
        Connection destinationConnection = mCallDestinations.getServiceCallDestination(
                serviceInfo.getPackageName(), service);
        callback.onRoute(service, serviceInfo, destinationConnection);
    }

    public interface RouteCallback<W> {

        void onRoute(
                W what,
                ComponentInfo destinationComponentInfo,
                @Nullable Connection destinationConnection
        );

        void onNotFound(W what);

        void onConflict(W what);
    }
}

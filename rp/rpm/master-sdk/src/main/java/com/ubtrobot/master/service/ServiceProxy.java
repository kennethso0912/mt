package com.ubtrobot.master.service;

import com.ubtrobot.master.call.ConvenientStickyCallable;
import com.ubtrobot.master.competition.CompetingItem;

import java.util.List;

/**
 * Created by column on 17-8-29.
 */

/**
 * 接入到 Master 的服务。注意区分 Android Service，此处“Service”是对通过 Master 对外提供能力调用、资源操作的程序的统称
 */
public interface ServiceProxy extends ConvenientStickyCallable {

    List<CompetingItem> getCompetingItems();

    boolean didAddState(String state);
}
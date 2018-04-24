package com.ubtrobot.master.policy;

import android.os.Parcel;
import android.os.Parcelable;

import com.ubtrobot.master.component.ComponentBaseInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by column on 26/11/2017.
 */

/**
 * Skill 政策
 */
public class SkillPolicy implements Parcelable {

    private ComponentBaseInfo skill;

    private BlackWhiteList<ComponentBaseInfo> didStart;
    private BlackWhiteList<ComponentBaseInfo> willStart;

    private Map<String, BlackWhiteList<ComponentBaseInfo>> didSetState;
    private Map<String, BlackWhiteList<ComponentBaseInfo>> willSetState;

    public static final Creator<SkillPolicy> CREATOR = new Creator<SkillPolicy>() {
        @Override
        public SkillPolicy createFromParcel(Parcel in) {
            return new SkillPolicy(in);
        }

        @Override
        public SkillPolicy[] newArray(int size) {
            return new SkillPolicy[size];
        }
    };

    public SkillPolicy(ComponentBaseInfo skill) {
        this.skill = skill;

        didStart = new BlackWhiteList<>();
        willStart = new BlackWhiteList<>();

        didSetState = new HashMap<>();
        willSetState = new HashMap<>();
    }

    private SkillPolicy(Parcel in) {
        skill = in.readParcelable(ComponentBaseInfo.class.getClassLoader());

        didStart = in.readParcelable(BlackWhiteList.class.getClassLoader());
        willStart = in.readParcelable(BlackWhiteList.class.getClassLoader());

        didSetState = new HashMap<>();
        in.readMap(didSetState, BlackWhiteList.class.getClassLoader());
        willSetState = new HashMap<>();
        in.readMap(willSetState, BlackWhiteList.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(skill, flags);

        dest.writeParcelable(didStart, flags);
        dest.writeParcelable(willStart, flags);

        dest.writeMap(didSetState);
        dest.writeMap(willSetState);
    }

    /**
     * 获取针对哪个 Skill 配置生命周期控制策略
     *
     * @return
     */
    public ComponentBaseInfo getSkill() {
        return skill;
    }

    /**
     * 获取当前 Skill start 后后续能够 start 的 Skill 黑白名单
     *
     * @return 黑白名单
     */
    public BlackWhiteList<ComponentBaseInfo> getDidStart() {
        return didStart;
    }

    /**
     * 获取 start 当前 Skill 前需要关闭的的 Skill 黑白名单
     *
     * @return 黑白名单
     */
    public BlackWhiteList<ComponentBaseInfo> getWillStart() {
        return willStart;
    }

    /**
     * 获取当前 Skill 已经设置某个状态后后续能够 start 的 Skill 黑白名单
     *
     * @param state Skill 内部状态
     * @return 黑白名单
     */
    public BlackWhiteList<ComponentBaseInfo> getDidSetState(String state) {
        BlackWhiteList<ComponentBaseInfo> blackWhiteList = didSetState.get(state);
        if (blackWhiteList == null) {
            blackWhiteList = new BlackWhiteList<>();
            didSetState.put(state, blackWhiteList);
        }

        return blackWhiteList;
    }

    /**
     * 获取当前 Skill 将设置某个状态前需要关闭的的 Skill 黑白名单
     *
     * @param state Skill 内部状态
     * @return 黑白名单
     */
    public BlackWhiteList<ComponentBaseInfo> getWillSetState(String state) {
        BlackWhiteList<ComponentBaseInfo> blackWhiteList = willSetState.get(state);
        if (blackWhiteList == null) {
            blackWhiteList = new BlackWhiteList<>();
            willSetState.put(state, blackWhiteList);
        }

        return blackWhiteList;
    }

    @Override
    public String toString() {
        return "SkillPolicy{" +
                "skill=" + skill +
                ", didStart=" + didStart +
                ", willStart=" + willStart +
                ", didSetState=" + didSetState +
                ", willSetState=" + willSetState +
                '}';
    }
}
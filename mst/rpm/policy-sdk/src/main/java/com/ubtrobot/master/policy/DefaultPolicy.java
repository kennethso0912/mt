package com.ubtrobot.master.policy;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;

import com.ubtrobot.master.Unsafe;
import com.ubtrobot.master.component.ComponentBaseInfo;
import com.ubtrobot.master.log.MasterLoggerFactory;
import com.ubtrobot.master.transport.message.ParamBundleConstants;
import com.ubtrobot.ulog.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by column on 26/11/2017.
 */

public class DefaultPolicy extends AbstractPolicy {

    private static final Logger LOGGER = MasterLoggerFactory.getLogger("DefaultPolicy");

    private static final String SKILL_STATE_DEFAULT = ParamBundleConstants.VAL_SKILL_STATE_DEFAULT;

    private final Context mContext;
    private final HashMap<ComponentBaseInfo, SkillPolicy> mSkillPolicies = new HashMap<>();
    private final HashMap<ComponentBaseInfo, ServicePolicy> mServicePolicies = new HashMap<>();

    public DefaultPolicy(Context context, Unsafe unsafe) {
        super(context, unsafe);
        this.mContext = context;

        load();
    }

    public void load() {
        Resources resources = mContext.getResources();
        int identifier = resources.getIdentifier(
                "component_policy", "xml", mContext.getPackageName());

        try {
            XmlResourceParser parser = resources.getXml(identifier);
            parseXml(parser);
        } catch (Resources.NotFoundException e) {
            LOGGER.i("xml/component_policy.xml NOT found. No component policies.");
        }
    }

    private void parseXml(XmlResourceParser parser) {
        int eventType;

        try {
            while (XmlPullParser.END_DOCUMENT != (eventType = parser.next())) {
                if (XmlPullParser.START_TAG != eventType) {
                    continue;
                }

                if ("component-policy".equals(parser.getName())) {
                    parseComponentPolicy(parser, "component-policy");
                    continue;
                }

                throw new ParsePolicyException("Illegal root tag <" + parser.getName() +
                        "> in the component policy xml.");
            }

        } catch (XmlPullParserException e) {
            throw new ParsePolicyException("Parse component policy xml failed.", e);
        } catch (IOException e) {
            throw new ParsePolicyException("Read component policy file failed.", e);
        }
    }

    private void parseComponentPolicy(
            XmlResourceParser parser, String endTag)
            throws IOException, XmlPullParserException {
        HashSet<String> appeared = new HashSet<>();
        int eventType;

        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("package".equals(parser.getName())) {
                String packageName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(packageName)) {
                    throw new ParsePolicyException("Illegal name attribute value " +
                            "in the <component-policy>.<package> tag.");
                }

                checkAndSetAppearedTag(appeared, "package", "name", packageName);
                parsePackageComponentPolicy(parser, "package", packageName);
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the <component-policy> tag.");
        }
    }

    private void checkAndSetAppearedTag(
            Set<String> appeared, String tagName,
            String attributeName, String attributeValue) {
        String key = tagName + attributeName + attributeValue;
        if (!appeared.add(key)) {
            throw new ParsePolicyException("Repeated <" + tagName + " " + attributeName +
                    "=\"" + attributeValue + "\".");
        }
    }

    private void parsePackageComponentPolicy(
            XmlResourceParser parser,
            String endTag,
            String packageName)
            throws IOException, XmlPullParserException {
        HashSet<String> appeared = new HashSet<>();
        int eventType;

        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("skill".equals(parser.getName())) {
                SkillPolicy skillPolicy = new SkillPolicy(new ComponentBaseInfo(
                        parseComponentName(parser, "<component-policy>.<package>.<skill>"),
                        packageName)
                );
                checkAndSetAppearedTag(appeared, "skill", "name", skillPolicy.getSkill().getName());

                mSkillPolicies.put(skillPolicy.getSkill(), skillPolicy);
                parseSkillPolicy(parser, "skill", skillPolicy);
                continue;
            }

            if ("service".equals(parser.getName())) {
                ServicePolicy servicePolicy = new ServicePolicy(new ComponentBaseInfo(
                        parseComponentName(parser, "<component-policy>.<package>.<service>"),
                        packageName)
                );
                checkAndSetAppearedTag(appeared, "service", "name", servicePolicy.getService().getName());

                mServicePolicies.put(servicePolicy.getService(), servicePolicy);
                parseServicePolicy(parser, "service", servicePolicy);
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the <component-policy>.<package> tag.");
        }
    }

    private String parseComponentName(XmlResourceParser parser, String tag) {
        String componentName = parser.getAttributeValue(null, "name");
        if (TextUtils.isEmpty(componentName)) {
            throw new ParsePolicyException("Illegal name or package attribute value in the " +
                    tag + " tag.");
        }

        return componentName;
    }

    private void parseSkillPolicy(
            XmlResourceParser parser, String endTag, SkillPolicy policy)
            throws IOException, XmlPullParserException {
        HashSet<String> appeared = new HashSet<>();

        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("did-start".equals(parser.getName())) {
                checkAndSetAppearedTag(appeared, "did-start", endTag);
                parseBlackWhiteList(parser, "did-start", policy.getDidStart());
                continue;
            }

            if ("did-set-state".equals(parser.getName())) {
                String state = parseState(parser, "did-set-state", true);
                checkAndSetAppearedTag(appeared, "did-set-state", "state_name", state);
                parseBlackWhiteList(parser, "did-set-state", policy.getDidSetState(state));
                continue;
            }

            if ("will-start".equals(parser.getName())) {
                checkAndSetAppearedTag(appeared, "will-start", endTag);
                parseBlackWhiteList(parser, "will-start", policy.getWillStart());
                continue;
            }

            if ("will-set-state".equals(parser.getName())) {
                String state = parseState(parser, "will-set-state", true);
                checkAndSetAppearedTag(appeared, "will-set-state", "state_name", state);
                parseBlackWhiteList(parser, "will-set-state", policy.getWillSetState(state));
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the <component-policy>.<package>.<skill> tag.");
        }
    }

    private void checkAndSetAppearedTag(
            Set<String> appeared, String tagName, String parentTag) {
        if (!appeared.add(tagName)) {
            throw new ParsePolicyException(
                    "Repeated <" + tagName + "> in the <" + parentTag + "> tag.");
        }
    }

    private void parseBlackWhiteList(
            XmlResourceParser parser, String endTag,
            BlackWhiteList<ComponentBaseInfo> blackWhiteList)
            throws IOException, XmlPullParserException {
        boolean blacklistAppeared = false;
        boolean whitelistAppeared = false;

        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("blacklist".equals(parser.getName())) {
                if (whitelistAppeared) {
                    throw new ParsePolicyException("blacklist and whitelist are exclusive " +
                            "in the <" + endTag + "> tag");
                }

                blacklistAppeared = true;
                blackWhiteList.setBlacklist(true);
                parseBlackWhiteListSkills(parser, "blacklist", blackWhiteList);
                continue;
            }

            if ("whitelist".equals(parser.getName())) {
                if (blacklistAppeared) {
                    throw new ParsePolicyException("blacklist and whitelist are exclusive " +
                            "in the <" + endTag + "> tag");
                }

                whitelistAppeared = true;
                blackWhiteList.setWhitelist(true);
                parseBlackWhiteListSkills(parser, "whitelist", blackWhiteList);
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the <skill-list> tag.");
        }
    }

    private void parseBlackWhiteListSkills(
            XmlResourceParser parser, String endTag,
            BlackWhiteList<ComponentBaseInfo> blackWhiteList)
            throws IOException, XmlPullParserException {
        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("skill".equals(parser.getName())) {
                blackWhiteList.add(parseComponentBaseInfo(parser, "skill"));
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the " + endTag + " tag.");
        }
    }

    private ComponentBaseInfo parseComponentBaseInfo(XmlResourceParser parser, String tag) {
        String skillName = parser.getAttributeValue(null, "name");
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(skillName) || TextUtils.isEmpty(packageName)) {
            throw new ParsePolicyException("Illegal name or package attribute value in the " +
                    tag + " tag.");
        }

        return new ComponentBaseInfo(skillName, packageName);
    }

    private String parseState(XmlResourceParser parser, String tagName, boolean isSkill) {
        String state = parser.getAttributeValue(null, "state_name");
        if (TextUtils.isEmpty(state)) {
            throw new ParsePolicyException(
                    "Illegal state_name attribute value in the <" + tagName + "> tag.");
        }

        if (isSkill) {
            if (SKILL_STATE_DEFAULT.equals(state)) {
                throw new ParsePolicyException(
                        "Illegal state_name attribute value in the <" + tagName + "> tag. " +
                                SKILL_STATE_DEFAULT + " is used by the sdk.");
            }
        }

        return state;
    }

    private void parseServicePolicy(
            XmlResourceParser parser, String endTag, ServicePolicy policy)
            throws IOException, XmlPullParserException {
        HashSet<String> appeared = new HashSet<>();

        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("did-add-state".equals(parser.getName())) {
                String state = parseState(parser, "did-add-state", false);
                checkAndSetAppearedTag(appeared, "did-add-state", "state_name", state);
                parseBlackWhiteList(parser, "did-add-state", policy.getDidAddState(state));
                continue;
            }

            if ("will-add-state".equals(parser.getName())) {
                String state = parseState(parser, "will-add-state", false);
                checkAndSetAppearedTag(appeared, "will-add-state", "state_name", state);
                parseBlackWhiteList(parser, "will-add-state", policy.getWillAddState(state));
                continue;
            }

            throw new ParsePolicyException("Illegal tag <" + parser.getName() +
                    "> in the <component-policy>.<package>.<service> tag.");
        }
    }

    @Override
    public void getSkillPolicies(
            List<ComponentBaseInfo> skillBaseInfos, List<SkillPolicy> outPolicies) {
        for (ComponentBaseInfo skillBaseInfo : skillBaseInfos) {
            SkillPolicy skillPolicy = mSkillPolicies.get(skillBaseInfo);
            if (skillPolicy != null) {
                outPolicies.add(skillPolicy);
            }
        }
    }

    @Override
    public void getServicePolicies(
            List<ComponentBaseInfo> serviceBaseInfos, List<ServicePolicy> outPolicies) {
        for (ComponentBaseInfo serviceBaseInfo : serviceBaseInfos) {
            ServicePolicy servicePolicy = mServicePolicies.get(serviceBaseInfo);
            if (servicePolicy != null) {
                outPolicies.add(servicePolicy);
            }
        }
    }

    private static class ParsePolicyException extends RuntimeException {

        ParsePolicyException(String message) {
            super(message);
        }

        ParsePolicyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
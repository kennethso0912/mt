package com.ubtrobot.master.component.validate;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;

import com.ubtrobot.master.component.StringResource;
import com.ubtrobot.master.service.ServiceCallInfo;
import com.ubtrobot.master.service.ServiceInfo;
import com.ubtrobot.master.skill.SkillCallInfo;
import com.ubtrobot.master.skill.SkillInfo;
import com.ubtrobot.master.skill.SkillIntentFilter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by column on 17-11-20.
 */

public class ComponentValidator {

    private final Context mContext;
    private final PackageInfo mPackageInfo;

    private volatile boolean mPermissionValidated;

    private boolean mMasterPermissionRequested;
    private boolean mMasterSystemServicePermissionRequested;

    private final HashSet<String> mDuplicateDetection = new HashSet<>();

    private final LinkedList<SkillInfo> mSkillInfoList = new LinkedList<>();
    private final LinkedList<ServiceInfo> mServiceInfoList = new LinkedList<>();

    public ComponentValidator(Context context) {
        mContext = context;
        try {
            mPackageInfo = context.getPackageManager().getPackageInfo(
                    mContext.getPackageName(),
                    PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA |
                            PackageManager.GET_SERVICES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 构造
     *
     * @param context     上下文
     * @param packageInfo 需包含 PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA |
     *                    PackageManager.GET_SERVICES
     */
    public ComponentValidator(Context context, PackageInfo packageInfo) {
        mContext = context;
        mPackageInfo = packageInfo;
    }

    public void validate() throws ValidateException {
        PackageManager packageManager = mContext.getPackageManager();
        if (!mPermissionValidated) {
            validatePermissionOnly();
        }

        if (!mMasterPermissionRequested) {
            throw new ValidateException(ValidateException.CODE_NO_PERMISSION,
                    " Miss the master permission. Use permission: " +
                            ComponentConstants.PERMISSION_MASTER);
        }

        if (mPackageInfo.services == null || mPackageInfo.services.length == 0) {
            return;
        }

        try {
            validateComponents(packageManager, mPackageInfo);
        } finally {
            mDuplicateDetection.clear();
        }
    }

    public void validatePermissionOnly() throws ValidateException {
        if (mPackageInfo.requestedPermissions != null) {
            for (String permission : mPackageInfo.requestedPermissions) {
                if (ComponentConstants.PERMISSION_MASTER.equals(permission)) {
                    mMasterPermissionRequested = true;
                    continue;
                }

                if (ComponentConstants.PERMISSION_SYSTEM_SERVICE.equals(permission)) {
                    mMasterSystemServicePermissionRequested = true;
                }
            }
        }

        mPermissionValidated = true;

        if (!mMasterPermissionRequested) {
            throw new ValidateException(ValidateException.CODE_NO_PERMISSION,
                    " Miss the master permission. Use permission: " +
                            ComponentConstants.PERMISSION_MASTER);
        }
    }

    private void validateComponents(PackageManager packageManager, PackageInfo packageInfo)
            throws ValidateException {
        for (android.content.pm.ServiceInfo serviceInfo : packageInfo.services) {
            if (serviceInfo.metaData == null) {
                continue;
            }

            boolean isSkill = serviceInfo.metaData.containsKey(ComponentConstants.META_DATA_KEY_SKILL);
            boolean isService = serviceInfo.metaData.containsKey(ComponentConstants.META_DATA_KEY_SERVICE);

            if (!isSkill && !isService) {
                continue;
            }

            if (isSkill && isService) {
                throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                        "Illegal meta data in " + serviceInfo.name + " 's declaration. " +
                                ComponentConstants.META_DATA_KEY_SKILL + " and " +
                                ComponentConstants.META_DATA_KEY_SKILL + " are conflict."
                );
            }

            if (isSkill) {
                XmlResourceParser parser = serviceInfo.loadXmlMetaData(packageManager,
                        ComponentConstants.META_DATA_KEY_SKILL);
                if (parser == null) {
                    throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                            "Illegal meta-data<" + ComponentConstants.META_DATA_KEY_SKILL +
                                    "> content. Should be <meta-data android:name=\"" +
                                    ComponentConstants.META_DATA_KEY_SKILL +
                                    "\" android:resource=\"@xml/a_xml_resource\" />");
                }

                parseSkillList(parser, isSystemPackage(packageInfo), serviceInfo);
            } else {
                XmlResourceParser parser = serviceInfo.loadXmlMetaData(packageManager,
                        ComponentConstants.META_DATA_KEY_SERVICE);
                if (parser == null) {
                    throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                            "Illegal meta-data<" + ComponentConstants.META_DATA_KEY_SERVICE +
                                    "> content. Should be <meta-data android:name=\"" +
                                    ComponentConstants.META_DATA_KEY_SERVICE +
                                    "\" android:resource=\"@xml/a_xml_resource\" />");
                }

                parseServiceList(parser, isSystemPackage(packageInfo), serviceInfo);
            }
        }
    }

    private boolean isSystemPackage(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
    }

    private void parseSkillList(
            XmlResourceParser parser,
            boolean isSystemPackage,
            android.content.pm.ServiceInfo serviceInfo) throws ValidateException {
        try {
            int eventType;
            while (XmlPullParser.END_DOCUMENT != (eventType = parser.next())) {
                if (XmlPullParser.START_TAG != eventType) {
                    continue;
                }

                if ("skill".equals(parser.getName())) {
                    String name = parseName(parser, "name");
                    if (!mDuplicateDetection.add("skill://" + name)) {
                        throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                                "Skill " + name + " has already been declared.");
                    }

                    SkillInfo skillInfo = new SkillInfo.Builder(name,
                            serviceInfo.packageName, serviceInfo.name).
                            setSystemPackage(isSystemPackage).
                            setLabel(parseNullableStrRes(parser, "label")).
                            setDescription(parseNullableStrRes(parser, "description")).
                            setIconRes(parseDrawableRes(parser, "icon")).
                            setCallInfoList(new ArrayList<SkillCallInfo>()).
                            build();

                    parseSkillCallInfo(parser, "skill", skillInfo, skillInfo.getCallInfoList());

                    skillInfo.makeImmutable();
                    mSkillInfoList.add(skillInfo);

                    continue;
                }

                throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                        "Unexpected root tag<" + parser.getName() + ">.");
            }
        } catch (XmlPullParserException e) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Parse xml failed.", e);
        } catch (IOException e) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Parse xml failed.", e);
        }
    }

    private String parseName(XmlResourceParser parser, String attributeName)
            throws ValidateException {
        String name = parser.getAttributeValue(null, attributeName);
        if (name == null) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Illegal " + attributeName + " attribute value."); // TODO name 合法性
        }

        return name;
    }

    private StringResource parseNullableStrRes(XmlResourceParser parser,
                                               String attributeName) throws ValidateException {
        int resId = parser.getAttributeResourceValue(null, attributeName, 0);
        if (resId != 0) {
            // TODO 判断是不是 string
            return new StringResource(resId);
        }

        return new StringResource(parser.getAttributeValue(null, attributeName));
    }

    private int parseDrawableRes(XmlResourceParser parser,
                                 String attributeName) throws ValidateException {
        return parser.getAttributeResourceValue(null, attributeName, 0);
        // TODO，检查是不是 Drawable
    }

    private void parseSkillCallInfo(
            XmlResourceParser parser, String endTag,
            SkillInfo skillInfo, List<SkillCallInfo> skillCallInfos)
            throws IOException, XmlPullParserException, ValidateException {
        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("call".equals(parser.getName())) {
                String path = parsePath(parser, "path");
                if (!mDuplicateDetection.add("skill://" + path)) {
                    throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                            "Skill call path " + path + " is conflict in the same skill or other skills");
                }

                skillCallInfos.add(new SkillCallInfo(
                        skillInfo,
                        path,
                        parseNullableStrRes(parser, "description"),
                        parseSkillIntentFilter(parser, "call"))
                );

                continue;
            }

            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Unexpected tag<" + parser.getName() + "> in the " + endTag + " tag.");
        }
    }

    private String parsePath(XmlResourceParser parser, String attributeName)
            throws ValidateException {
        String path = parser.getAttributeValue(null, attributeName);
        if (path == null || !path.startsWith("/")) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Illegal " + attributeName + " attribute value."); // TODO path 合法性
        }

        return path;
    }

    private List<SkillIntentFilter> parseSkillIntentFilter(XmlResourceParser parser, String endTag)
            throws IOException, XmlPullParserException, ValidateException {
        ArrayList<SkillIntentFilter> intentFilters = new ArrayList<>();

        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("intent-filter".equals(parser.getName())) {
                String category = parseIntentFilterCategory(parser, "category");
                if (SkillIntentFilter.CATEGORY_SPEECH.equals(category)) {
                    intentFilters.add(
                            new SkillIntentFilter(
                                    category,
                                    parseSpeechUtterance(parser, "intent-filter")
                            )
                    );
                    continue;
                }

                throw new AssertionError("Should NOT be here. Should process all category.");
            }

            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Unexpected tag<" + parser.getName() + "> in the " + endTag + " tag.");
        }

        return intentFilters;
    }

    private String parseIntentFilterCategory(XmlResourceParser parser, String attributeName)
            throws ValidateException {
        String category = parser.getAttributeValue(null, attributeName);
        for (String aCategory : SkillIntentFilter.CATEGORIES) {
            if (aCategory.equals(category)) {
                return category;
            }
        }

        throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                "Unsupported " + attributeName + " attribute value. Only support " +
                        SkillIntentFilter.CATEGORIES);
    }

    private List<StringResource> parseSpeechUtterance(XmlResourceParser parser, String endTag)
            throws IOException, XmlPullParserException, ValidateException {
        ArrayList<StringResource> utteranceList = new ArrayList<>();

        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("utterance".equals(parser.getName())) {
                utteranceList.add(parseNonEmptyStrRes(parser, "sentence"));
                continue;
            }

            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Unexpected tag<" + parser.getName() + "> in the " + endTag + " tag.");
        }

        return utteranceList;
    }

    private StringResource parseNonEmptyStrRes(XmlResourceParser parser,
                                               String attributeName) throws ValidateException {
        int resId = parser.getAttributeResourceValue(null, attributeName, 0);
        if (resId != 0) {
            // TODO 判断是不是 string
            return new StringResource(resId);
        }

        String value = parser.getAttributeValue(null, attributeName);
        if (TextUtils.isEmpty(value)) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "The value of " + attributeName + " attribute is empty.");
        }

        return new StringResource(value);
    }

    private void parseServiceList(
            XmlResourceParser parser,
            boolean isSystemPackage,
            android.content.pm.ServiceInfo androidServiceInfo) throws ValidateException {
        try {
            int eventType;
            while (XmlPullParser.END_DOCUMENT != (eventType = parser.next())) {
                if (XmlPullParser.START_TAG != eventType) {
                    continue;
                }

                if ("service".equals(parser.getName())) {
                    String name = parseName(parser, "name");
                    if (!mDuplicateDetection.add("service://" + name)) {
                        throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                                "Service " + name + " has already been declared.");
                    }

                    ServiceInfo serviceInfo = new ServiceInfo.Builder(name,
                            androidServiceInfo.packageName, androidServiceInfo.name).
                            setSystemPackage(isSystemPackage).
                            setLabel(parseNullableStrRes(parser, "label")).
                            setDescription(parseNullableStrRes(parser, "description")).
                            setIconRes(parseDrawableRes(parser, "icon")).
                            setCallInfoList(new ArrayList<ServiceCallInfo>()).
                            build();
                    parseServiceCallInfo(parser, "service", serviceInfo,
                            serviceInfo.getCallInfoList());

                    serviceInfo.makeImmutable();
                    mServiceInfoList.add(serviceInfo);
                    continue;
                }

                throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                        "Unexpected root tag<" + parser.getName() + ">.");
            }
        } catch (XmlPullParserException e) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Parse xml failed.", e);
        } catch (IOException e) {
            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Parse xml failed.", e);
        }
    }

    private void parseServiceCallInfo(
            XmlResourceParser parser, String endTag,
            ServiceInfo serviceInfo,
            List<ServiceCallInfo> callInfoList)
            throws IOException, XmlPullParserException, ValidateException {
        int eventType;
        while (XmlPullParser.END_TAG != (eventType = parser.next()) ||
                !endTag.equals(parser.getName())) {
            if (XmlPullParser.START_TAG != eventType) {
                continue;
            }

            if ("call".equals(parser.getName())) {
                String path = parsePath(parser, "path");
                if (!mDuplicateDetection.add("service://" + serviceInfo.getName() + path)) {
                    throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                            "Service call path " + path + " is conflict in the " +
                                    serviceInfo.getName() + " service.");
                }

                callInfoList.add(new ServiceCallInfo(
                        serviceInfo,
                        path,
                        parseNullableStrRes(parser, "description")
                ));

                continue;
            }

            throw new ValidateException(ValidateException.CODE_ILLEGAL_CONFIGURATION,
                    "Unexpected tag<" + parser.getName() + "> in the " + endTag + " tag.");
        }
    }


    public List<SkillInfo> getSkillInfoList() {
        return mSkillInfoList;
    }

    public List<ServiceInfo> getServiceInfoList() {
        return mServiceInfoList;
    }
}
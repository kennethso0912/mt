package com.ubtrobot.master.component;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by column on 17-11-23.
 */

public class ComponentInfo implements Parcelable {

    private String name;
    private String packageName;
    private String className;

    private boolean isSystemPackage;
    private StringResource label;
    private StringResource description;
    private int iconResource;

    public static final Creator<ComponentInfo> CREATOR = new Creator<ComponentInfo>() {
        @Override
        public ComponentInfo createFromParcel(Parcel in) {
            return new ComponentInfo(in);
        }

        @Override
        public ComponentInfo[] newArray(int size) {
            return new ComponentInfo[size];
        }
    };

    protected ComponentInfo(Parcel in) {
        name = in.readString();
        packageName = in.readString();
        className = in.readString();
        isSystemPackage = in.readByte() != 0;
        label = in.readParcelable(StringResource.class.getClassLoader());
        description = in.readParcelable(StringResource.class.getClassLoader());
        iconResource = in.readInt();
    }

    protected ComponentInfo(String name, String packageName, String className) {
        this.name = name;
        this.packageName = packageName;
        this.className = className;
    }

    protected ComponentInfo(Builder<?> builder) {
        name = builder.name;
        packageName = builder.packageName;
        className = builder.className;
        isSystemPackage = builder.isSystemPackage;
        label = builder.label;
        description = builder.description;
        iconResource = builder.iconRes;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public boolean isSystemPackage() {
        return isSystemPackage;
    }

    public StringResource getLabel() {
        return label;
    }

    public StringResource getDescription() {
        return description;
    }

    public int getIconResource() {
        return iconResource;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(packageName);
        dest.writeString(className);
        dest.writeByte((byte) (isSystemPackage ? 1 : 0));
        dest.writeParcelable(label, flags);
        dest.writeParcelable(description, flags);
        dest.writeInt(iconResource);
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                ", isSystemPackage=" + isSystemPackage +
                ", label=" + label +
                ", description=" + description +
                ", iconResource=" + iconResource +
                '}';
    }

    public static class Builder<T extends Builder<T>> {

        private String name;
        private String packageName;
        private String className;

        private boolean isSystemPackage;
        private StringResource label;
        private StringResource description;
        private int iconRes;

        public Builder(String name, String packageName, String className) {
            this.name = name;
            this.packageName = packageName;
            this.className = className;
        }

        public T setSystemPackage(boolean systemPackage) {
            isSystemPackage = systemPackage;
            //noinspection unchecked
            return (T) this;
        }

        public T setLabel(StringResource label) {
            this.label = label;
            //noinspection unchecked
            return (T) this;
        }

        public T setDescription(StringResource description) {
            this.description = description;
            //noinspection unchecked
            return (T) this;
        }

        public T setIconRes(int iconRes) {
            this.iconRes = iconRes;
            //noinspection unchecked
            return (T) this;
        }

        public ComponentInfo build() {
            ComponentInfo componentInfo = new ComponentInfo(name, packageName, className);
            componentInfo.isSystemPackage = isSystemPackage;
            componentInfo.label = label;
            componentInfo.description = description;
            componentInfo.iconResource = iconRes;
            return componentInfo;
        }
    }
}
package com.ubtrobot.master.call;

/**
 * Created by column on 17-9-23.
 */

public class CallConfiguration {

    private static final int DEFAULT_TIMEOUT = 15000;

    private int timeout;
    private boolean suppressSyncCallOnMainThreadWarning;

    private CallConfiguration() {
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isSyncCallOnMainThreadWarningSuppressed() {
        return suppressSyncCallOnMainThreadWarning;
    }

    @Override
    public String toString() {
        return "CallConfiguration{" +
                "timeout=" + timeout +
                '}';
    }

    public static class Builder {

        private int timeout;
        private boolean suppressSyncCallOnMainThreadWarning;

        public Builder() {
            timeout = DEFAULT_TIMEOUT;
            suppressSyncCallOnMainThreadWarning = false;
        }

        public Builder(CallConfiguration configuration) {
            timeout = configuration.getTimeout();
            suppressSyncCallOnMainThreadWarning = configuration.isSyncCallOnMainThreadWarningSuppressed();
        }

        public Builder setTimeout(int timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Argument timeout <= 0.");
            }

            this.timeout = timeout;
            return this;
        }

        public Builder suppressSyncCallOnMainThreadWarning(boolean suppress) {
            suppressSyncCallOnMainThreadWarning = suppress;
            return this;
        }

        public CallConfiguration build() {
            CallConfiguration configuration =  new CallConfiguration();
            configuration.timeout = timeout;
            configuration.suppressSyncCallOnMainThreadWarning = suppressSyncCallOnMainThreadWarning;
            return configuration;
        }
    }
}

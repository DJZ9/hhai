package tech.aiflowy.ai.service.userProfileService;

public class DataScopeManager {
    private static final ThreadLocal<Boolean> ignoreFlags = new ThreadLocal<>();

    public static void ignoreDataScopeCondition() {
        ignoreFlags.set(Boolean.TRUE);
    }

    public static void restoreDataScopeCondition() {
        ignoreFlags.remove();
    }
}

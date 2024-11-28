package io.ten1010.aipub.projectcontroller.core;

public final class IndexNames {

    public static final String BY_NAMESPACE_NAME_TO_PROJECT_OBJECT = "BY_NAMESPACE_NAME_TO_PROJECT_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_GROUP_OBJECT = "BY_NAMESPACE_NAME_TO_GROUP_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_CRON_JOB_OBJECT = "BY_NAMESPACE_NAME_TO_CRON_JOB_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT = "BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_DEPLOYMENT_OBJECT = "BY_NAMESPACE_NAME_TO_DEPLOYMENT_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_JOB_OBJECT = "BY_NAMESPACE_NAME_TO_JOB_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_POD_OBJECT = "BY_NAMESPACE_NAME_TO_POD_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_REPLICA_SET_OBJECT = "BY_NAMESPACE_NAME_TO_REPLICA_SET_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_REPLICATION_CONTROLLER_OBJECT = "BY_NAMESPACE_NAME_TO_REPLICATION_CONTROLLER_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_STATEFUL_SET_OBJECT = "BY_NAMESPACE_NAME_TO_STATEFUL_SET_OBJECT";
    public static final String BY_NAMESPACE_NAME_TO_SECRET_OBJECT = "BY_NAMESPACE_NAME_TO_SECRET_OBJECT";
    public static final String BY_IMAGE_NAMESPACE_GROUP_NAME_TO_IMAGE_NAMESPACE_GROUP_OBJECT = "BY_IMAGE_NAMESPACE_GROUP_NAME_TO_IMAGE_NAMESPACE_GROUP_OBJECT";
    public static final String BY_IMAGE_NAMESPACE_GROUP_BINDING_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT = "BY_IMAGE_NAMESPACE_GROUP_BINDING_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT";
    public static final String BY_NODE_NAME_TO_GROUP_OBJECT = "BY_NODE_NAME_TO_GROUP_OBJECT";
    public static final String BY_NODE_GROUP_NAME_TO_NODE_GROUP_OBJECT = "BY_NODE_GROUP_NAME_TO_NODE_GROUP_OBJECT";
    public static final String BY_NODE_GROUP_BINDING_NAME_TO_NODE_GROUP_BINDING_OBJECT = "BY_NODE_GROUP_BINDING_NAME_TO_NODE_GROUP_BINDING_OBJECT";
    public static final String BY_PROJECT_NAME_TO_PROJECT_OBJECT = "BY_PROJECT_NAME_TO_PROJECT_OBJECT";
    public static final String BY_PROJECT_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT = "BY_PROJECT_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT";
    public static final String BY_PROJECT_NAME_TO_NODE_GROUP_BINDING_OBJECT = "BY_PROJECT_NAME_TO_NODE_GROUP_BINDING_OBJECT";
    public static final String BY_DAEMON_SET_KEY_TO_GROUP_OBJECT = "BY_DAEMON_SET_KEY_TO_GROUP_OBJECT";

    private IndexNames() {
        throw new UnsupportedOperationException();
    }

}

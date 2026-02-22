package com.beingadish.AroundU.common.constants;

public class URIConstants {

    // API versioning
    private static final String API = "/api";
    private static final String V1 = "/v1";
    private static final String BASE = API + V1;

    // Base paths
    public static final String AUTH_BASE = BASE + "/auth";
    public static final String CLIENT_BASE = BASE + "/client";
    public static final String WORKER_BASE = BASE + "/worker";
    public static final String JOB_BASE = BASE + "/jobs";
    public static final String BID_BASE = BASE + "/bid";
    public static final String REVIEW_BASE = BASE + "/reviews";
    public static final String CHAT_BASE = BASE + "/chat";
    public static final String PUBLIC_BASE = BASE + "/public";
    public static final String SKILL_BASE = BASE + "/skills";
    public static final String ADMIN_BASE = BASE + "/admin";
    public static final String USER_BASE = BASE + "/users";

    // Auth endpoints
    public static final String REGISTER = "/register";
    public static final String LOGIN = "/login";

    // Job endpoints
    public static final String JOB_CREATE = JOB_BASE + "/create";
    public static final String JOB_BY_ID = JOB_BASE + "/{id}";
}

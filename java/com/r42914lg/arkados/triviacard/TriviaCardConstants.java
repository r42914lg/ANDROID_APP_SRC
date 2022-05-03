package com.r42914lg.arkados.triviacard;

public class TriviaCardConstants {
    public static final boolean LOG = BuildConfig.DEBUG;

    public static final boolean ADS_ENABLED = true;
    public static final boolean STRICT_MODE = false;

    public static final float SHORT_DISPLAY_RATION = 1.9f;
    public static final float SHORT_DISPLAY_SIZE_PIXELS = 1000;
    public static final int DISPLAY_NORMAL = 1;
    public static final int DISPLAY_SHORT = 2;
    public static final int DISPLAY_LOW_RES = 3;

    public static final int NUM_OF_QUESTIONS_IN_QUIZ = 10;
    public static final int NUM_OF_OPTIONS = 4;
    public static final int MARGIN_PD = 2;
    public static final int NUM_OF_OPTIONS_JSON = 6;
    public static final int CELLS_COUNT = 9;
    public static final int POINTS_PER_QUESTION = 300;
    public static final int POINTS_PER_FAILURE = 50;

    public static final int Q_NOT_PLAYED = 0;
    public static final int Q_WON = 1;
    public static final int Q_FAILURE_1 = 2;
    public static final int Q_FAILURE_2 = 3;

    public static final int ANIMATE_OPTION_WIN = 0;
    public static final int ANIMATE_OPTION_FAIL_1 = 1;
    public static final int ANIMATE_OPTION_FAIL_2 = 2;

    public static final int FAB_ACTION_REFRESH = 0;
    public static final int FAB_ACTION_NEXT = 1;
    public static final int FAB_ACTION_SKIP = 2;
    public static final int FAB_ACTION_FINISH = 3;

    public static final long BUFFER_SIZE_LIMIT = 1024 * 500;

    public static final int SUBSCRIPTION_STATE_ACTIVE = 0;
    public static final int SUBSCRIPTION_STATE_CANCELLED = 1;
    public static final int SUBSCRIPTION_STATE_GRACE = 2;
    public static final int SUBSCRIPTION_STATE_ON_HOLD = 3;
    public static final int SUBSCRIPTION_STATE_PAUSED = 4;
    public static final int SUBSCRIPTION_STATE_EXPIRED = 5;

    public static final String SUBSCRIPTION_SKU = "photoriddle.noads1";
    public static final String APP_PACKAGE_NAME = "com.r42914lg.arkados.triviacard";
    public static final String FUNCTION_NAME = "verifySubscription";

    public static final String JSON_QUIZZES = "quizzes";
    public static final String JSON_QUESTIONS = "questions";
    public static final String JSON_IMAGES = "images";
    public static final String JSON_QUESTION_USAGE = "question_usage";
    public static final String JSON_LOCAL_FILE_NAME = "local.json";
}

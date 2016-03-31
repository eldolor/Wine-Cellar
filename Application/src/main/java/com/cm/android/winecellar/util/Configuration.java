package com.cm.android.winecellar.util;

/**
 * Created by anshugaind on 3/28/16.
 */
public class Configuration {

    /******* PRODUCTION ******/
    public static final String BASE_URL = "https://skok-prod.appspot.com";
    public static final String CONTENTS_URL = BASE_URL
            + "/winecellar/wine";
    public static final String DROPBOX_URL = BASE_URL + "/dropbox/url";

    public static final int HTTP_SOCKET_TIMEOUT = 45000;
    public static final int HTTP_CONNECTION_TIMEOUT = 60000;
    public static final int HTTP_MAX_ATTEMPTS = 5;
    public static final int HTTP_BACKOFF_MS = 2000;

}

package com.cm.android.winecellar.provider;

import android.content.Context;

import com.cm.android.winecellar.util.Utils;

import java.util.List;

/**
 * Created by anshugaind on 3/13/16.
 */
public class Images {


    public static List<String> getImageUrls(Context context){

        return Utils.listFileAbsolutePaths(Utils.getExternalImageStorageDir(context).getAbsolutePath());

    }

    public static List<String> getThumbnailUrls(Context context){

        return Utils.listFileAbsolutePaths(Utils.getExternalThumbnailStorageDir(context).getAbsolutePath());

    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cm.android.winecellar.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StrictMode;

import com.cm.android.common.logger.Log;
import com.cm.android.winecellar.AnalyticsTrackers;
import com.cm.android.winecellar.ui.ImageDetailActivity;
import com.vvw.activity.lite.ImageGridActivity;
import com.google.android.gms.analytics.Tracker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Class containing some static utility methods.
 */
public class Utils {

    private static String sTag = Utils.class.getName();


    private Utils() {
    }

    ;
    private static final String TAG = Utils.class.getName();
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    public static final String SHARED_PREF_NAME = "com.vvw.activity";
    public static final int THUMBNAIL_SIZE = 320;
    public static final String PICTURES_EXTENSION = ".jpg";


    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static void enableStrictMode() {
        if (Utils.hasGingerbread()) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            if (Utils.hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen();
                vmPolicyBuilder
                        .setClassInstanceLimit(ImageGridActivity.class, 1)
                        .setClassInstanceLimit(ImageDetailActivity.class, 1);
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    /**
     * Recursively extracts all file names from a given path
     *
     * @param path
     * @return list of file names
     * @author anshu
     */
    public static List<String> listFiles(String path) {

        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();

        if (list == null)
            return null;

        for (File f : list) {
            if (f.isDirectory()) {
                Utils.listFiles(f.getAbsolutePath());
            } else {
                fileNames.add(f.getName());
            }
        }
        return fileNames;
    }

    /**
     * Recursively extracts all file names with their absolute path from a given path
     *
     * @param path
     * @return list of file names with absolute path
     * @author anshu
     */
    public static List<String> listFileAbsolutePaths(String path) {

        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();

        if (list == null)
            return null;

        for (File f : list) {
            if (f.isDirectory()) {
                Utils.listFiles(f.getAbsolutePath());
            } else {
                fileNames.add(f.getAbsolutePath());
            }
        }
        return fileNames;
    }

    /**
     * Recursively deletes all files
     *
     * @param f
     * @throws java.io.IOException
     */
    public static void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            Log.d(TAG, "Failed to delete file: " + f);
    }


    /**
     * Get the external storage directory.
     *
     * @param context The context to use
     * @return The external storage dir
     */
    @TargetApi(VERSION_CODES.FROYO)
    public static File getExternalImageStorageDir(Context context) {
        File storageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/images");
        if (!storageDir.exists())
            storageDir.mkdirs();
        return storageDir;
    }

    /**
     * Get the external storage directory.
     *
     * @param context The context to use
     * @return The external storage dir
     */
    @TargetApi(VERSION_CODES.FROYO)
    public static File getExternalThumbnailStorageDir(Context context) {
        File storageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/thumbs");
        if (!storageDir.exists())
            storageDir.mkdirs();
        return storageDir;
    }

    /**
     * @return
     * @throws IOException
     * @author anshu
     */
    public static File createImageFile(Context context) throws IOException {
        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";

//        File storageDir = Utils.getExternalImageStorageDir(context);

//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );


        String pathname = Utils.getExternalImageStorageDir(context).getAbsolutePath() + File.separator + (Math.abs(new Random(System
                .currentTimeMillis()).nextLong())) + PICTURES_EXTENSION;
        File image = new File(pathname);

        return image;
    }

    /**
     *
     * @param urlString
     * @param outputStream
     * @return
     */
    public static boolean write(String urlString, OutputStream outputStream) {

        File image = new File(urlString);
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            //final URL url = new URL(urlString);
            in = new BufferedInputStream(new FileInputStream(image), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
            }
        }
        return false;
    }


    /**
     * Create a thumnail image, and store it
     *
     * @param context
     * @param imageAbsolutePath
     * @param thumbnailAbsolutePath
     * @param thumbnailWidth
     * @param thumbnailHeight
     * @return the bitmap
     */
    public static Bitmap createThumbnail(Context context, String imageAbsolutePath, String thumbnailAbsolutePath, int thumbnailWidth, int thumbnailHeight) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {

            //bis = new BufferedInputStream(new FileInputStream(imageAbsolutePath), Utils.IO_BUFFER_SIZE);
            bos = new BufferedOutputStream(new FileOutputStream(thumbnailAbsolutePath), Utils.IO_BUFFER_SIZE);


//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//
//            options.inSampleSize = ImageResizer.calculateInSampleSize(options, thumbnailWidth, thumbnailHeight);
//
//            Bitmap decodedBitmap = BitmapFactory.decodeStream(bis, null, options);
//            if(decodedBitmap == null ){
//                Log.e("createThumbnail", "image data could not be decoded.");
//            }
//
//            Bitmap scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, thumbnailWidth, thumbnailHeight, false);
//            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//            Bitmap bMap = ThumbnailUtils.extractThumbnail(imageBitmap, thumbnailWidth, thumbnailHeight);
//            bMap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imageAbsolutePath), thumbnailWidth, thumbnailHeight);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            return thumbnail;
        } catch (Exception e) {
            Log.e("createThumbnail", e.toString());
            return null;
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                    //imageBitmap.recycle();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (final Exception e) {
            }
        }


    }

    /**
     * Return GA Tracker
     *
     * @param context
     * @param target
     * @return
     */
    public static Tracker getAnalyticsTracker(Context context, AnalyticsTrackers.Target target){
        try{
            AnalyticsTrackers.initialize(context);
        }catch (Exception e) {}

        return AnalyticsTrackers.getInstance().get(target);

    }

    /**
     * Returns null for any exception thrown
     *
     * @param filename
     * @return
     */
    public static Long extractRowIdFromFileName(String filename) {
        try {
            String[] split = filename.split(".jpg");
            int lastIndexOf = split[0].lastIndexOf(File.separator);
            return Long.valueOf(split[0].substring(lastIndexOf + 1));
        } catch (Throwable e) {
            android.util.Log.e(Utils.sTag, "error: "
                    + ((e.getMessage() != null) ? e.getMessage().replace(" ",
                    "_") : ""), e);
        }
        return null;

    }

    public static int retryIfNetworkDisconnected(ConnectivityManager connectivityManager) {
        long NETWORK_DISCONNECTED_BACKOFF = Configuration.HTTP_BACKOFF_MS
                + new Random().nextInt(Configuration.HTTP_BACKOFF_MS);
        int MAX_ATTEMPTS = Configuration.HTTP_MAX_ATTEMPTS;
        try {
            android.util.Log.d(Utils.sTag,"Entering");
            for (int j = 1; j <= MAX_ATTEMPTS; j++) {

                if (isNetworkConnected(connectivityManager)) {
                    // success
                    j = MAX_ATTEMPTS;
                    return StatusCode.SUCCESS;
                } else {
                    // Sleep till the network state is connected, and then
                    // restart
                    // the download process
                    try {
                        android.util.Log.d(Utils.sTag,"Network disconnected. Sleeping for "
                                    + NETWORK_DISCONNECTED_BACKOFF
                                    + " ms before retry");
                        Thread.sleep(NETWORK_DISCONNECTED_BACKOFF);
                    } catch (InterruptedException e1) {
                        // Activity
                        // finished
                        // before we
                        // complete -
                        // exit.
                        android.util.Log.d(Utils.sTag,"Thread interrupted: abort remaining retries!");
                        Thread.currentThread().interrupt();
                    }
                    // increase backoff
                    // exponentially
                    NETWORK_DISCONNECTED_BACKOFF *= 2;

                }
            }
            return StatusCode.NETWORK_DISCONNECTED;
        } finally {
            android.util.Log.d(Utils.sTag,"Exiting");

        }

    }

    private static boolean isNetworkConnected(ConnectivityManager connectivityManager) {
        return (connectivityManager.getActiveNetworkInfo() != null && connectivityManager
                .getActiveNetworkInfo().isConnected());
    }

    private static boolean isConnectedToWifi(ConnectivityManager connectivityManager) {
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnected())
            return true;
        return false;
    }

    public static final String[] WINES = new String[] { "…chezeaux",
            "Sauterns", "Blanc de Noirs", "Roussette de Savoie",
            "Savennieres-Coulee-de-Serrant", "Caino Blanco", "Saint-Pourcain",
            "Alsace Grand Cru", "CÙtes de Blaye", "Torontel",
            "Quarts de Chaume", "Mourvedre", "Barbera d'Asti", "Pedro Ximenez",
            "Buzet", "Chenin Blanc", "Tinta Cao", "Anjou Villages",
            "CÙtes du Marmandais", "Gew¸rztraminer", "Bergerac",
            "CÙtes du Ventoux", "Beaujolais-Villages", "Bordeaux Haut-Benauge",
            "Chablis Premier Cru", "Dominio de Valdepusa",
            "Coteaux de Pierrevert", "Saint-Aubin", "Cotes de Provence",
            "Banylus", "Listrac-Medoc", "Merlot", "ChÈnas",
            "Chablis Grand Cru", "CrÈmant d'Alsace", "Rosette", "Champagne",
            "Cotes de Blaye", "Semillon", "Chaume", "Cotes de la Malepere",
            "Arneis", "Mazis-Chambertin", "Volnay", "Malbec", "Cerons",
            "CrÈmant de Bordeaux", "RÌas Baixas", "Corse†or†Vin de Corse",
            "Lussac-Saint-Emilion", "Bearn", "CÙtes de Montravel",
            "Treixadura", "Bourgogne mousseux", "Cremant de Limoux",
            "Lalande-de-Pomerol", "Cote-Rotie", "OrlÈans", "Cheverny",
            "Echezeaux", "Haut-MÈdoc", "Montrachet", "Finca Elez",
            "Cremant de Die", "CrÈmant de Bourgogne", "Bonnezeaux",
            "Coteaux du Loir", "Bordeaux rose", "Clos de Vougeot", "Neac",
            "Cortese", "AligotÈ", "Garnacha", "Lussac-Saint-emilion",
            "Anjou Villages Brissac", "Campo de la Guardia", "Santenay",
            "Orleans-Clery", "Cotes du Rhone Villages", "Hermitage",
            "Pauillac", "Gavi", "Super Tuscans", "Chablis",
            "Coteaux du Languedoc", "Cremant de Bourgogne",
            "Dehesa del Carrizal", "Moulis†or Moulis-en-MÈdoc", "Vougeot",
            "Bourgogne le Chapitre", "Griotte-Chambertin",
            "PenedËs (Barcelona)", "Montepulciano", "Ch‚tillon-en-Diois",
            "Muscadet-Cotes de Grandlieu", "Clos des Lambrays", "Irouleguy",
            "Eiswein", "Puisseguin Saint-emilion", "Petit Chablis", "Fitou",
            "CÙtes de Bourg", "CrÈmant de Limoux", "RosÈ de Loire",
            "CÙte de Nuits-villages", "Saint-Emilion", "Vin de Savoie",
            "Grand Roussillon", "Mead", "Cote de Beaune", "Tempranillo",
            "CÙte de Beaune", "Saint-…milion Grand Cru", "CarmÈnËre",
            "SÈmillon", "Touraine-Amboise", "Pouilly-LochÈ",
            "Gaillac Premieres Cotess", "Muscat de Rivesaltes", "Grenache",
            "Bourgogne aligote", "Carignane", "Touraine Noble Joue",
            "Bordeaux rosÈ", "Cotes de Toul", "Crepy",
            "Pacherenc du Vic-Bilh Sec", "Costieres de Nimes", "Moscato",
            "Brouilly", "Saint-Emilion Grand Cru", "Pacherenc du Vic-Bilh",
            "Cotes du Ventoux", "Pessac-LÈognan", "Bardolino", "Vinsobres",
            "Saumur", "Saint-Bris", "Barolo", "Coteaux du Lyonnais", "Cahors",
            "CrÈmant du Jura", "Morey-Saint-Denis",
            "CÙtes de Bordeaux Saint-Macaire", "RosÈ", "Scheurebe",
            "Coteaux du Vendomois", "Blanquette de Limoux", "Frontignan",
            "Touraine", "Coteaux de Die", "Ovada", "Saint-PourÁain", "Chenas",
            "Bourgogne clairet", "Givry", "Guijoso", "Barbaresco",
            "CÙtes de Duras", "L'etoile", "Bordeaux Cotes de Francs",
            "Volnay Santenots", "Bouzeron", "Clos de Tart", "Blush",
            "Gigondas", "Cote Roannaise", "Barbera del Monferrato Superiore",
            "Boal", "CÙtes de la Malepere", "Beaujolais", "Bergerac rose",
            "Grands echezeaux", "Souson", "Banyuls", "Charlemagne",
            "Romanee-Saint-Vivant", "Bourgogne aligotÈ", "Corbieres",
            "CÙtes du Vivarais", "Cremant de Loire", "Canon Fronsac",
            "Coteaux Varois", "Chapelle-Chambertin", "Touraine Noble JouÈ",
            "Asti Spumante", "Priorat", "Marsala", "Auxey-Duresses",
            "Pouilly-Fume", "Pinot Grigio", "Lambrusco", "Vouvray",
            "Touriga Francesa", "Grands …chezeaux", "Graves Superieures",
            "M‚con-villages", "Pouilly-Loche", "Margaux", "Albarino",
            "Banyuls Grand Cru", "Graves SupÈrieures", "Clos Saint-Denis",
            "Pessac-Leognan", "Moulin a vent", "Coteaux d'Aix-en-Provence",
            "Bugey", "Tinta Roriz", "Bourgogne Montrecul", "Soave",
            "CÙtes de Toul", "Lussac-Saint-…milion", "Bordeaux",
            "Bienvenues-B‚tard-Montrachet", "Bordeaux moelleux", "Madiran",
            "Petite Sirah", "Claret", "Montlouis", "Premieres CÙtes de Blaye",
            "Muscadet", "Cabernet Sauvignon", "Sainte-Foy-Bordeaux",
            "Saint-Estephe", "Montagny", "Saint-emilion", "Cour-Cheverny",
            "Chambertin", "CÙte-RÙtie", "Chinon", "Bual", "Richebourg",
            "Bourgogne clairet CÙte chalonnaise", "Grands Echezeaux",
            "Chardonnay", "Grignolino", "Malvasia", "Cotes de Montravel",
            "Coteaux de l'Aubance", "Mazoyeres-Chambertin", "Rivesaltes",
            "La T‚che", "Pinot Gris", "Savennieres-CoulÈe-de-Serrant", "Rose",
            "Premieres CÙtes de Bordeaux", "Touraine-Mesland", "Charbono",
            "Asti", "Bourgogne Cote Saint-Jacques", "Macon-villages", "BÈarn",
            "Roero", "CÙtes du Forez", "Crozes-Hermitage", "Irancy",
            "Chambertin-Clos-de-Beze", "Nuits-Saint-Georges", "Finca elez",
            "Loupiac", "Bergerac rosÈ", "Bourgogne Hautes-cÙtes de Nuits",
            "Maranges", "Cotes de Bourg", "Cotes du Roussillon Villages",
            "Coteaux du Giennois", "Beaumes de Venise", "AlbariÒo", "Reuilly",
            "Cabernet d'Anjou", "Montagne Saint-emilion", "Cotes du Forez",
            "Patrimonio", "Bourgogne clairet Cote chalonnaise", "Sangiovese",
            "Campo de Borja", "Saint-Chinian", "Haut-Montravel",
            "Pouilly-Vinzelles", "Pouilly-FumÈ", "CÙte Roannaise", "Retsina",
            "Savennieres-Roche-aux-Moines", "Regnie",
            "Bourgogne Passe-tout-grains", "Cotes du Roussillon",
            "Chianti Classico", "Cote de Brouilly", "Saint-…milion",
            "Vin Santo", "Quincy", "Bourgueil", "Meritage", "CrÈpy", "Rioja",
            "Lirac", "VirÈ-ClessÈ", "Saint-Amour", "Anjou", "Pomerol",
            "Faugeres", "Trebbiano", "Bergerac sec", "Bordeaux clairet",
            "La Grande Rue", "M‚con supÈrieur", "Cotes de Castillon", "Rueda",
            "Coteaux Champenois", "Aloxe-Corton",
            "Bourgogne CÙtes du Couchois", "La Romanee", "Gamay Beaujolais",
            "Otazu", "Bourgogne VÈzelay", "Coteaux du Tricastin",
            "CÙtes de Castillon", "Pernand-Vergelesses",
            "Muscadet-Sevre et Maine", "Sainte-Croix-du-Mont", "Toro",
            "Muller-Thurgau", "Bordeaux superieur", "Muscat", "Haut-Medoc",
            "Saumur-Champigny", "Barbera", "CrÈmant de Loire",
            "Bourgogne Cotes du Couchois", "Gaillac",
            "Bourgogne La Chapelle Notre-Dame", "Saint-Romain", "Port",
            "Moulis†or Moulis-en-Medoc", "Cotes du Rhone",
            "Corton-Charlemagne", "Saint-Georges Saint-…milion", "Ajaccio",
            "Grappa", "Ugni Blanc", "Clairette de Bellegarde",
            "Penedes (Barcelona)", "Carignan", "Batard-Montrachet", "Shiraz",
            "Bourgogne Coulanges-la-Vineuse", "Coteaux du Layon",
            "CÙtes du RhÙne Villages", "Rias Baixas",
            "Blanquette methode ancestrale", "OrlÈans-ClÈry",
            "CÙtes du Roussillon Villages", "Gattinara",
            "Touraine-Azay-le-Rideau", "Meursault", "Sauvignon vert",
            "Romanee-Conti", "Ruchottes-Chambertin", "Pouilly-sur-Loire",
            "Barsac", "Saint-Joseph", "Monthelie",
            "Bourgogne Hautes-cÙtes de Beaune", "Clairette de Die",
            "Zinfandel", "Vino Nobile di Montepulciano", "Savigny-les-Beaune",
            "CÈrons", "CÙtes de Provence", "Jerez-Xeres-Sherry", "Cinsault",
            "Saumur mousseux", "Maury", "Pommard", "Vosne-Romanee",
            "Ch‚teau-Grillet", "Gewurztraminer", "Pago Florentino",
            "Ribera del Duero", "Fronton", "Saint-Georges Saint-Emilion",
            "Vins Fins de la Cote de Nuits", "Pinot Meunier", "Dolcetto",
            "Loureira", "Chianti", "Fleurie", "CÙtes de Bergerac Blanc",
            "Durif", "Seyssel", "Vins Fins de la CÙte de Nuits", "Kir",
            "Jumilla", "La Tache", "Musigny", "Marsannay", "echezeaux",
            "Coteaux du VendÙmois", "CrÈmant de Die", "Blanc de Blancs",
            "Muscadet-CÙtes de Grandlieu", "Chorey-les-Beaune", "MÈdoc",
            "Chassagne-Montrachet", "Chatillon-en-Diois", "Bourgogne Epineuil",
            "Caino Tinto", "Vernaccia di San Gimignano",
            "Montagne Saint-…milion", "Bordeaux sec", "Pinot Blanc", "NÈac",
            "Bellet", "Macon", "Bourgogne", "Vacqueyras", "Valpolicella",
            "CÙtes du Roussillon", "Menetou-Salon", "Rose de Loire", "Morgon",
            "Monastrell", "Cornas", "Ch‚teauneuf-du-Pape",
            "Latricieres-Chambertin", "Verdicchio", "Roussane",
            "Cotes du Vivarais", "Syrah", "Sauternes", "M¸ller-Thurgau",
            "Muscat de Frontignan", "FumÈ Blanc", "Cotes du Marmandais",
            "Clairette du Languedoc", "Muscat de Lunel", "CÙtes de Bergerac",
            "Premieres Cotes de Bordeaux", "B‚tard-Montrachet", "Anjou-Gamay",
            "Cote de Nuits-villages", "Cabardes", "Montagne Saint-Emilion",
            "Rose des Riceys", "Bourgogne Hautes-cotes de Beaune",
            "Clos de la Roche", "RomanÈe-Conti", "Cabernet de Saumur",
            "Saint-Julien", "French Colombard", "Macon superieur",
            "Liebfraumilch", "CÙtes du RhÙne", "Sancerre", "Pinot Noir",
            "Charmes-Chambertin", "L'…toile", "Bourgogne CÙtes d'Auxerre",
            "Marcillac", "Pouilly-Fuisse", "Saussignac", "Madeira", "Viognier",
            "Cotes du Luberon", "ValenÁay", "Cotes de Bordeaux Saint-Macaire",
            "Folle Blanche", "Bienvenues-Batard-Montrachet",
            "Bourgogne Hautes-cotes de Nuits", "Pinotage", "Touriga Nacional",
            "Dogliani", "Puisseguin Saint-Emilion", "Monbazillac",
            "Saint-Georges Saint-emilion", "Finca …lez", "SeÒorÌo de ArÌnzano",
            "Gaillac Premieres CÙtess", "Pecharmant", "Brunello di Montalcino",
            "Chateau-Grillet", "RÈgniÈ", "CÙte de Brouilly", "Sherry",
            "Cotes du Jura", "Blaye", "Puligny-Montrachet",
            "Cotes de Bergerac Blanc", "JuliÈnas", "Jerez-XÈrËs-Sherry",
            "La RomanÈe", "Gamay", "IroulÈguy", "Fixin", "Rousanne", "Blagny",
            "Costieres de NÓmes", "Tavel", "Bandol", "L'Etoile", "Acqui",
            "Blanquette mÈthode ancestrale", "Saint-VÈran", "Ladoix",
            "Premieres Cotes de Blaye", "Bonnes-Mares", "Tokay", "Limoux",
            "Saint-Nicolas-de-Bourgueil", "JuranÁon", "Bordeaux supÈrieur",
            "Rully", "Entre-Deux-Mers", "Cremant d'Alsace", "Muscadelle",
            "Cremant du Jura", "Pouilly-FuissÈ", "Carmenere", "Jasnieres",
            "PÈcharmant", "Orleans", "Cote de Beaune-Villages",
            "Bourgogne ordinaire", "Muscat de Mireval", "Graves", "M‚con",
            "Arbois", "Cremant de Bordeaux", "Cava", "Cotes de Bergerac",
            "Prado de Irache", "Brunello", "Vosne-RomanÈe",
            "Bourgogne CÙte Saint-Jacques", "Muscadet-Coteaux de la Loire",
            "Frascati", "Amarone", "Bourgogne rosÈ",
            "Puisseguin Saint-…milion", "Mercurey", "Minervois-La Liviniere",
            "Cadillac", "Marsanne", "Alsace", "Vire-Clesse", "Aligote",
            "Corton", "Gevrey-Chambertin", "Johannisberg Riesling",
            "Sauvignon Blanc", "RomanÈe-Saint-Vivant", "Torrontes",
            "Muscat de Beaumes-de-Venise", "CÙte de Beaune-Villages",
            "Ch‚teau-Chalon", "Carinena", "Fronsac", "Palette",
            "Pineau des Charentes", "Senorio de Arinzano", "RosÈ des Riceys",
            "CÙtes du Jura", "Bourgogne Cotes d'Auxerre", "Montravel",
            "Cabernet Franc", "Coteaux de Saumur", "Chambolle-Musigny",
            "Criots-B‚tard-Montrachet", "Fume Blanc", "Melon de Bourgogne",
            "Listrac-MÈdoc", "Beaune", "Muscat de Saint-Jean de Minervois",
            "Chevalier-Montrachet", "Petit Verdot",
            "Entre-Deux-Mers-Haut-Benauge", "Bordeaux CÙtes de Francs",
            "Ghemme", "Julienas", "Valencay", "Pedro XimÈnez",
            "Chateauneuf-du-Pape", "Nebbiolo", "Bourgogne grand ordinaire",
            "Les Baux-de-Provence", "Tinta Barroca", "Marc", "Constantia",
            "Cassis", "Palomino", "Chateau-Chalon", "Medoc", "Colombard",
            "Graves de Vayres", "CÙtes du Luberon", "Saint-Peray", "Traminer",
            "Collioure", "Chiroubles", "Criots-Batard-Montrachet", "Condrieu",
            "Auslese", "Bourgogne Vezelay", "Saint-Veran", "Saint-PÈray",
            "Roussette du Bugey", "Jurancon", "Bourgogne rose", "Savennieres",
            "Anjou-Coteaux de la Loire", "Saint-emilion Grand Cru",
            "Cotes de Duras", "Minervois" };
}

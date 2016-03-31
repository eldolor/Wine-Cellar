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

package com.cm.android.winecellar.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import android.util.Log;
import com.cm.android.winecellar.AnalyticsTrackers;
import com.cm.android.winecellar.db.Note;
import com.cm.android.winecellar.db.NotesDbAdapter;
import com.cm.android.winecellar.provider.AuthProvider;
import com.cm.android.winecellar.provider.Images;
import com.cm.android.winecellar.util.Configuration;
import com.cm.android.winecellar.util.ImageCache;
import com.cm.android.winecellar.util.ImageFetcher;
import com.cm.android.winecellar.util.ImageWorker;
import com.cm.android.winecellar.util.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Key;
import com.google.api.client.util.Maps;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vvw.activity.lite.BuildConfig;
import com.vvw.activity.lite.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import me.philio.pinentry.PinEntryView;

/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
public class ImageGridFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "ImageGridFragment";
    private static final String IMAGE_CACHE_DIR = "thumbs";
    private static final int REQUEST_TAKE_PHOTO = 1;

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ImageAdapter mAdapter;
    private ImageFetcher mImageFetcher;
    //private Uri mImageUri;
    private PinEntryView mPinEntryView;

    private Tracker mTracker;
    private AdView mAdView;

    private static String CLOUD_VISION_API_KEY;
    private static String APPLICATION_NAME;
    private Vibrator mVibrator;
    private GridView mGridView;
    private Bundle mExtras;

    private ConnectivityManager mConnectivityManager;


    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageGridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new ImageAdapter(getActivity());

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

        mTracker = Utils.getAnalyticsTracker(getActivity(), AnalyticsTrackers.Target.APP);
        mVibrator = (Vibrator) getActivity().getSystemService(getActivity().VIBRATOR_SERVICE);

        mExtras = getActivity().getIntent().getExtras();

        mConnectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        insertSeedData();
    }

    private void insertSeedData() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    String seedDataSet = getActivity().getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).getString("SEED_DATA_SET", "N");
                    if (seedDataSet.equals("N")) {
                        FileOutputStream out1 = null;
                        FileOutputStream out2 = null;
                        Bitmap bm1 = null;
                        Bitmap bm2 = null;
                        NotesDbAdapter dbHelper = null;
                        long rowId = Math.abs(new Random(System
                                .currentTimeMillis()).nextLong());

                        try {
                            String imageAbsolutePath = Utils.getExternalImageStorageDir(getActivity()).getAbsolutePath();
                            String thumbnailAbsolutePath = Utils.getExternalThumbnailStorageDir(getActivity()).getAbsolutePath();

                            bm1 = BitmapFactory.decodeResource(getResources(), R.drawable.wine_label_icon);
                            File imageFile = new File(imageAbsolutePath, rowId + Utils.PICTURES_EXTENSION);
                            out1 = new FileOutputStream(imageFile);
                            bm1.compress(Bitmap.CompressFormat.PNG, 100, out1);

                            bm2 = BitmapFactory.decodeResource(getResources(), R.drawable.wine_label_icon);
                            File thumbnailFile = new File(thumbnailAbsolutePath, rowId + Utils.PICTURES_EXTENSION);
                            out2 = new FileOutputStream(thumbnailFile);
                            bm2.compress(Bitmap.CompressFormat.PNG, 100, out2);

                            Note note = new Note();
                            note.id = rowId;
                            note.wine = "Cabernet Sauvignon";
                            note.rating = "4.0";
                            note.textExtract = "Kenwood Vineyards 2004 Sonoma County";
                            note.notes = "Raspberry and candied strawberry with moderately high tannin on the finish.";
                            //default to Y
                            note.share = "Y";
                            note.picture = rowId + Utils.PICTURES_EXTENSION;

                            dbHelper = new NotesDbAdapter(getActivity());
                            dbHelper.open();
                            dbHelper.createNote(note);

                            //finally
                            getActivity().getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().putString("SEED_DATA_SET", "Y").commit();
                        } catch (Exception e) {
                            //do nothing
                        } finally {
                            try {
                                out1.flush();
                                out1.close();
                                bm1.recycle();
                                out2.flush();
                                out2.close();
                                bm2.recycle();
                                if (dbHelper != null)
                                    dbHelper.close();
                            } catch (Exception e) {//do nothing}
                            }
                        }
                    }

                }
            });
        } catch (Throwable t) {
            //do nothing
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    if (!Utils.hasHoneycomb()) {
                        mImageFetcher.setPauseWork(true);
                    }
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @TargetApi(VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                                    mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                                }
                                if (Utils.hasJellyBean()) {
                                    mGridView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    mGridView.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    }
                });

        //@author anshu
        final ImageView mAddImage = (ImageView) v.findViewById(R.id.add_image);
        mAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mVibrator.vibrate(250);
                    takePhoto();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e("takePhoto", ex.toString());
                }
            }
        });

        //Admob
        mAdView = (AdView) v.findViewById(R.id.adView);

        //constants
        CLOUD_VISION_API_KEY = getActivity().getResources().getString(R.string.cloud_vision_api_key);
        APPLICATION_NAME = getActivity().getResources().getString(R.string.app_name);


        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                //GA
                mTracker.setScreenName("ImageGrid");
                //use count of images
                mTracker.send(new HitBuilders.ScreenViewBuilder().setCustomMetric(1, Images.getImageUrls(getActivity()).size()).build());
                //AdMob
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        });

    }


    @Override
    public void onStop() {
        super.onStop();
        AuthProvider.getAuthProvider().logout();
    }


    /**
     * @author anshu
     */
    public void takePhoto() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = Utils.createImageFile(getActivity());
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("takePhoto", ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                getActivity().getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().putString("IMAGE_NAME", photoFile.getName()).commit();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    String imageFileName = getActivity().getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).getString("IMAGE_NAME", null);
                    String imageAbsolutePath = Utils.getExternalImageStorageDir(getActivity()).getAbsolutePath() + File.separator + imageFileName;
                    String thumbnailAbsolutePath = Utils.getExternalThumbnailStorageDir(getActivity()).getAbsolutePath() + File.separator + imageFileName;
                    Bitmap thumbnailBitmap = Utils.createThumbnail(getActivity(), imageAbsolutePath, thumbnailAbsolutePath, Utils.THUMBNAIL_SIZE, Utils.THUMBNAIL_SIZE);
                    mAdapter.notifyDataSetChanged();

                    //Call Google Vision API
                    uploadImage(thumbnailAbsolutePath, thumbnailBitmap);
                }
        }
    }

    public void uploadImage(String imageAbsolutePath, Bitmap bitmap) {
        try {

            callCloudVision(imageAbsolutePath, bitmap);


        } catch (Throwable e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
            Toast.makeText(getActivity(), "Image picking failed because " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final String imageAbsolutePath, final Bitmap bitmap) throws IOException {
        // Switch text to loading
        // mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = new NetHttpTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(CLOUD_VISION_API_KEY));
                    Vision vision = builder.build();
                    builder.setApplicationName(APPLICATION_NAME);

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature detection = new Feature();
                            detection.setType("TEXT_DETECTION");
                            detection.setMaxResults(1);
                            add(detection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (Throwable e) {
                    Log.d(TAG, "failed to make API request because of " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            /**
             * @param response
             * @return
             */
            private String convertResponseToString(BatchAnnotateImagesResponse response) {
                String message = "";

                List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
                if (labels != null) {
                    for (EntityAnnotation label : labels) {
                        message += String.format("%s", label.getDescription());
                    }
                }

                return message;
            }

            protected void onPostExecute(String result) {
                //mImageDetails.setText(result);
                Log.i(TAG, "RESULT " +
                        result);
                NotesDbAdapter dbHelper = null;
                try {
                    dbHelper = new NotesDbAdapter(getActivity());
                    dbHelper.open();
                    Note newNote = new Note();
                    newNote.id = Utils.extractRowIdFromFileName(imageAbsolutePath);
                    newNote.textExtract = result;
                    dbHelper.createNote(newNote);

                    //TODO: upload to cloud

                    try{
                        HttpTransport httpTransport = new NetHttpTransport();
                        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(new MyInitializer());
                        GenericUrl genericUrl = new GenericUrl(Configuration.DROPBOX_URL);

                        //Get the upload URL from server
                        HttpRequest httpGetRequest = requestFactory.buildGetRequest(genericUrl);
                        HttpResponse httpGetResponse = httpGetRequest.execute();
                        Log.i(TAG, "HTTP STATUS:: " + httpGetResponse.getStatusCode());

                        if (httpGetResponse.getStatusCode() == 200) {
                            //MyUrl myUrl = null;
                            JSONObject getJson = null;

                            try {
                                // process the HTTP response object
                                String response = httpGetResponse.parseAsString();
                                Log.i(TAG, "URL:: " + response);
                                getJson = new JSONObject(response);
                            } finally {
                                httpGetResponse.disconnect();
                            }

                            //upload the image
                            {
                                Map<String, String> parameters = Maps.newHashMap();
                                parameters.put("rowId", String.valueOf(newNote.id));

                                // Add parameters
                                MultipartContent content = new MultipartContent().setMediaType(
                                        new HttpMediaType("multipart/form-data")
                                                .setParameter("boundary", "__END_OF_PART__"));

                                for (String name : parameters.keySet()) {
                                    MultipartContent.Part part = new MultipartContent.Part(
                                            new ByteArrayContent(null, parameters.get(name).getBytes()));
                                    part.setHeaders(new HttpHeaders().set(
                                            "Content-Disposition", String.format("form-data; name=\"%s\"", name)));
                                    content.addPart(part);
                                }

                                // Add file
                                FileContent fileContent = new FileContent(
                                        "image/jpeg", new File(imageAbsolutePath));
                                MultipartContent.Part part = new MultipartContent.Part(fileContent);
                                part.setHeaders(new HttpHeaders().set(
                                        "Content-Disposition",
                                        String.format("form-data; name=\"file\"; filename=\"%s\"", imageAbsolutePath)));
                                content.addPart(part);

                                HttpResponse httpPostResponse = null;

                                try {
                                    httpPostResponse = requestFactory.buildPostRequest(
                                            new GenericUrl(getJson.getString("url")), content).execute();

                                    Log.i(TAG, "HTTP STATUS:: " + httpPostResponse.getStatusCode());
                                    // process the HTTP response object
                                    String response = httpPostResponse.parseAsString();
                                    Log.i(TAG, "URI:: " + response);
                                    JSONObject postJson = new JSONObject(response);

                                    //update the uri in the database
                                    Note updatedNote = new Note();
                                    updatedNote.id = newNote.id;
                                    updatedNote.uri = postJson.getString("uri");
                                    dbHelper.updateNote(updatedNote);


                                } finally {
                                    httpPostResponse.disconnect();
                                }
                            }
                            //upload the Note Json to the server. It now has the URI to the image,and textextract
                            {
                                JSONObject jsonObject = new JSONObject();

                                Note note = dbHelper.fetchNote(newNote.id);
                                jsonObject.put("rowId", note.id);
                                jsonObject.put("wine", note.wine);
                                jsonObject.put("rating", note.rating);
                                jsonObject.put("textExtract", note.textExtract);
                                jsonObject.put("notes", note.notes);
                                jsonObject.put("uri", note.uri);
                                jsonObject.put("timeCreatedMs", note.created);
                                jsonObject.put("timeCreatedTimeZoneOffsetMs", TimeZone.getDefault()
                                        .getRawOffset());
                                jsonObject.put("timeUpdatedMs", note.updated);
                                jsonObject.put("timeUpdatedTimeZoneOffsetMs", TimeZone.getDefault()
                                        .getRawOffset());


                                HttpResponse httpPostResponse = null;

                                try {
                                    httpPostResponse = requestFactory.buildPostRequest(
                                            new GenericUrl(Configuration.CONTENTS_URL),
                                                new ByteArrayContent("application/json", jsonObject.toString().getBytes())).execute();

                                    Log.i(TAG, "HTTP STATUS:: " + httpPostResponse.getStatusCode());


                                } finally {
                                    httpPostResponse.disconnect();
                                }
                            }

                        }


                    } catch (Throwable e) {
                        Log.e(TAG, e.getMessage(), e);
                    }



//                    long BACKOFF = Configuration.HTTP_BACKOFF_MS
//                            + new Random().nextInt(Configuration.HTTP_BACKOFF_MS);
//                    int MAX_ATTEMPTS = Configuration.HTTP_MAX_ATTEMPTS;
//                    int CONNECTION_TIMEOUT = Configuration.HTTP_CONNECTION_TIMEOUT;
//                    int SOCKET_TIMEOUT = Configuration.HTTP_SOCKET_TIMEOUT;
//                    int[] validResponseCodes = {200};
//
//                    for (int j = 1; j <= MAX_ATTEMPTS; j++) {
//                        Log.d(TAG, "Attempt #" + j + " to upload content");
//                        try {
//                            Log.d(TAG, "Entering");
//                            // wait for network connectivity; will download the content list
//                            // over cellular network
//                            int lStatusCode = Utils.retryIfNetworkDisconnected(mConnectivityManager);
//                            if (lStatusCode == StatusCode.NETWORK_DISCONNECTED)
//                                break;
//
//                            HashMap<String, String> params = new HashMap<String, String>();
//                            params.put("id", String.valueOf(note.id));
//
//                            String responseJson = HttpUtils.doHttpGet(Configuration.DROPBOX_URL, CONNECTION_TIMEOUT, SOCKET_TIMEOUT, validResponseCodes);
//
//                            JSONObject jsonObject = new JSONObject(responseJson);
//                            String url = jsonObject.getString("url");
//                            Log.d(TAG, "URL: " + url);
//                            String uri = HttpUtils.doMultiPartHttpPost(url,
//                                    imageAbsolutePath,
//                                    CONNECTION_TIMEOUT,
//                                    SOCKET_TIMEOUT, validResponseCodes);
//                            Log.d(TAG, "URI: " + uri);
//                            JSONObject jsonObject1 = new JSONObject();
//                            jsonObject1.put("id", note.id);
//                            jsonObject1.put("uri", uri);

//                            HttpUtils.doHttpPost(Configuration.CONTENTS_URL, jsonObject1, "application/json", CONNECTION_TIMEOUT, SOCKET_TIMEOUT, validResponseCodes);

                            // success
//                            j = MAX_ATTEMPTS;
//                        } catch (ClientProtocolException e) {
//                            Log.e(TAG, e.getMessage(), e);
//                        } catch (IOException e) {
//                            Log.e(TAG, "IOException on attempt " + j, e);
//                            if (j == MAX_ATTEMPTS) {
//                                break;
//                            }
//                            try {
//                                Log.d(TAG, "Sleeping for " + BACKOFF
//                                        + " ms before retry");
//                                Thread.sleep(BACKOFF);
//                            } catch (InterruptedException e1) {
//                                // Activity finished
//                                // before we
//                                // complete - exit.
//                                Log.d(TAG, "Thread interrupted: abort remaining retries!");
//                                Thread.currentThread().interrupt();
//                            }
//                            // increase backoff
//                            // exponentially
//                            BACKOFF *= 2;
//
//                        } catch (StatusCodeException e) {
//                            Log.e(TAG, e.getMessage(), e);
//                            if (e.getStatusCode() == 503) {
//                                Log.e(TAG, "Failed to upload on attempt " + j, e);
//                                if (j == MAX_ATTEMPTS) {
//                                    break;
//                                }
//                                try {
//                                    Log.d(TAG, "Sleeping for " + BACKOFF
//                                            + " ms before retry");
//                                    Thread.sleep(BACKOFF);
//                                } catch (InterruptedException e1) {
//                                    // Activity finished before we complete - exit.
//                                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
//                                    Thread.currentThread().interrupt();
//                                }
//                                // increase backoff exponentially
//                                BACKOFF *= 2;
//                            } else {
//                                break;
//                            }
//                        } finally {
//                            Log.d(TAG, "Exiting");
//
//                        }
//                    }


                } catch (Throwable e) {
                    android.util.Log.e(TAG, "error: "
                            + ((e.getMessage() != null) ? e.getMessage().replace(" ",
                            "_") : ""), e);
                } finally {
                    if (dbHelper != null)
                        dbHelper.close();
                }


            }


        }.execute();
    }

    private static class MyInitializer implements HttpRequestInitializer, HttpUnsuccessfulResponseHandler {

        @Override
        public boolean handleResponse(
                HttpRequest request, HttpResponse response, boolean retrySupported) throws IOException {
            Log.d(TAG, response.getStatusCode() + " " + response.getStatusMessage());
            if(response.getStatusCode() == 503){
                //retry
                return true;
            }
            return false;
        }

        @Override
        public void initialize(HttpRequest request) throws IOException {
            ExponentialBackOff backoff = new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(1000)
                    .setMaxElapsedTimeMillis(900000)
                    .setMaxIntervalMillis(10000)
                    .setMultiplier(1.5)
                    .setRandomizationFactor(0.5)
                    .build();
            request.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(backoff));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.EXTRA_IMAGE, (int) id);
        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            ActivityOptions options =
                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            getActivity().startActivity(i, options.toBundle());
        } else {
            startActivity(i);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_menu:
                return true;
            case R.id.logout_menu:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageGridFragment.this.getActivity());

        builder
                .setMessage("Logout?")
                .setPositiveButton("Logout now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        AuthProvider.getAuthProvider().logout();
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }


    /**
     * The main adapter that backs the GridView. This is fairly standard except the number of
     * columns in the GridView is used to create a fake top row of empty views as we use a
     * transparent ActionBar and don't want the real top row of images to start off covered by it.
     */
    private class ImageAdapter extends BaseAdapter {

        private final Context mContext;
        private int mItemHeight = 0;
        private int mNumColumns = 0;
        private int mActionBarHeight = 0;
        private GridView.LayoutParams mImageViewLayoutParams;

        public ImageAdapter(Context context) {
            super();
            mContext = context;
            mImageViewLayoutParams = new GridView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            // Calculate ActionBar height
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, tv, true)) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
        }

        @Override
        public int getCount() {
            // If columns have yet to be determined, return no items
            if (getNumColumns() == 0) {
                return 0;
            }

            // Size + number of columns for top empty row
            //return Images.imageThumbUrls.length + mNumColumns;
            //return Images.getThumbnailUrls(getActivity()).size() + mNumColumns;
            return Images.getThumbnailUrls(getActivity()).size() + mNumColumns;
        }

        @Override
        public Object getItem(int position) {
//            return position < mNumColumns ?
//                    null : Images.imageThumbUrls[position - mNumColumns];
//            return position < mNumColumns ?
//                    null : Images.getThumbnailUrls(getActivity()).get(position - mNumColumns);
            return position < mNumColumns ?
                    null : Images.getThumbnailUrls(getActivity()).get(position - mNumColumns);
        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            // Two types of views, the normal ImageView and the top row of empty views
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            //BEGIN_INCLUDE(load_gridview_item)
            // First check if this is the top row
            if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(mContext);
                }
                // Set empty view with height of ActionBar
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        LayoutParams.MATCH_PARENT, mActionBarHeight));
                return convertView;
            }

            // Now handle the main ImageView thumbnails
            View view;
            ImageView imageView;
            final RatingBar ratingBar;
            if (convertView == null) { // if it's not recycled, instantiate and initialize
                //imageView = new RecyclingImageView(mContext);
                //imageView = (RecyclingImageView)container.findViewById(R.id.grid_image);
                view = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.grid_item, container, false);
                imageView = (RecyclingImageView) view.findViewById(R.id.grid_image);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(mImageViewLayoutParams);
                ratingBar = (RatingBar) view.findViewById(R.id.grid_image_rating);

            } else { // Otherwise re-use the converted view
                //imageView = (ImageView) convertView;
                view = convertView;
                imageView = (ImageView) convertView.findViewById(R.id.grid_image);
                ratingBar = (RatingBar) convertView.findViewById(R.id.grid_image_rating);
            }

            // Check the height matches our calculated column width
//            if (imageView.getLayoutParams().height != mItemHeight) {
//                imageView.setLayoutParams(mImageViewLayoutParams);
//            }
            if (view.getLayoutParams().height != mItemHeight) {
                view.setLayoutParams(mImageViewLayoutParams);
            }

            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
            //mImageFetcher.loadImage(Images.imageThumbUrls[position - mNumColumns], imageView);
            mImageFetcher.loadImage(Images.getThumbnailUrls(getActivity()).get(position - mNumColumns), imageView, new ImageWorker.OnImageLoadedListener() {
                @Override
                public void onImageLoaded(boolean success) {
                    if (ratingBar != null)
                        ratingBar.setNumStars(3);
                }
            });
            //mImageFetcher.loadImage(Images.getThumbnailUrls(getActivity()).get(position - mNumColumns), imageView);

 /*           new AsyncTask<Object, Void, Void>() {
                @Override
                protected Void doInBackground(Object... params) {
                    try {

                    } catch (Throwable e) {
                        Log.d(TAG, "failed " +
                                e.getMessage());
                    }
                    return null;
                }

                protected void onPostExecute() {
                    try {


                    } catch (Throwable e) {
                        android.util.Log.e(TAG, "error: "
                                + ((e.getMessage() != null) ? e.getMessage().replace(" ",
                                "_") : ""), e);
                    } finally {
                    }


                }


            }.execute();
 */
            return imageView;
            //END_INCLUDE(load_gridview_item)


        }

        /**
         * Sets the item height. Useful for when we know the column width so the height can be set
         * to match.
         *
         * @param height
         */
        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }
            mItemHeight = height;
            mImageViewLayoutParams =
                    new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
            mImageFetcher.setImageSize(height);
            notifyDataSetChanged();
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }

//    new ImageWorker.OnImageLoadedListener() {
//
//        @Override
//        public void onImageLoaded(boolean success) {
//            //
//        }
//    }

}

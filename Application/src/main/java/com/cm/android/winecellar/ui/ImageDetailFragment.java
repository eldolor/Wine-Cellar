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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cm.android.winecellar.AnalyticsTrackers;
//import com.cm.android.displayingbitmaps.R;
import com.cm.android.winecellar.db.Note;
import com.cm.android.winecellar.db.NotesDbAdapter;
import com.cm.android.winecellar.util.AsyncTask;
import com.cm.android.winecellar.util.ImageFetcher;
import com.cm.android.winecellar.util.ImageWorker;
import com.cm.android.winecellar.util.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.vvw.activity.lite.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This fragment will populate the children of the ViewPager from {@link ImageDetailActivity}.
 */
public class ImageDetailFragment extends Fragment implements ImageWorker.OnImageLoadedListener, ImageWorker.OnImageDeletedListener {
    private static final String ROW_ID = "row_id";
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private static final String IMAGE_THUMBNAIL_DATA_EXTRA = "extra_thumbnail_image_data";
    private String mImageUrl;
    private String mThumbnailUrl;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private ImageFetcher mImageFetcher;
    private RatingBar mRatingBar;
    private EditText mTextExtract;
    private EditText mNotes;
    private ImageView mSave;

    private String sTag = ImageDetailFragment.class.getName();

    //@author anshu
    private Tracker mTracker;
    private AdView mAdView;
    SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM d, yyyy");
    // Stateful Field
    private Long mRowId;
    private boolean mIsNew;

    private NotesDbAdapter mDbHelper;
    private Vibrator mVibrator;
    private TextView mDateCreated;
    private TextView mDateUpdated;
    private AutoCompleteTextView mWine;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl, String imageThumbnailUrl) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        args.putString(IMAGE_THUMBNAIL_DATA_EXTRA, imageThumbnailUrl);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {
    }

    /**
     * Populate image using a url from extras, use the convenience factory method
     * to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;
        mThumbnailUrl = getArguments() != null ? getArguments().getString(IMAGE_THUMBNAIL_DATA_EXTRA) : null;
        mRowId = Utils.extractRowIdFromFileName(mImageUrl);

        mTracker = Utils.getAnalyticsTracker(getActivity(), AnalyticsTrackers.Target.APP);
        //required to set the options menu
        setHasOptionsMenu(true);

        mVibrator = (Vibrator) getActivity().getSystemService(getActivity().VIBRATOR_SERVICE);


//        if (mRowId == 0L) {
//            Log.w(sTag, "onCreate::ID is ZERO!!!");
//            getActivity().finish();
//        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
        //@author anshu:
        //Admob
        mAdView = (AdView) v.findViewById(R.id.adView);

        mRatingBar = (RatingBar) v.findViewById(R.id.rating);
        mTextExtract = (EditText) v.findViewById(R.id.text_extract);
        mNotes = (EditText) v.findViewById(R.id.notes);
        mSave = (ImageView) v.findViewById(R.id.save);
        mDateCreated = (TextView) v.findViewById(R.id.date_created_label);
        mDateUpdated = (TextView) v.findViewById(R.id.date_updated_label);
        mWine = (AutoCompleteTextView)v.findViewById(R.id.wine);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                //GA
                mTracker.setScreenName("ImageDetail");
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                //AdMob
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
                //wine adapter
                mWine.setAdapter(new ArrayAdapter<String>(getActivity(),
                        R.layout.list_item, Utils.WINES));
            }
        });
        //display the other contents
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

            }
        });

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrator.vibrate(250);
                save();
                getActivity().finish();
            }
        });
        load();


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageDetailActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((ImageDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(mImageUrl, mImageView, this);

            //database opened and managed by over arching activity
            mDbHelper = ((ImageDetailActivity) getActivity()).getDbHelper();
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.image_detail_menu, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            case R.id.delete_menu:
                deleteImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageDetailFragment.this.getActivity());

        builder
                .setMessage("Delete?")
                .setPositiveButton("Delete now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Yes-code
                        mProgressBar.setVisibility(View.VISIBLE);
                        //String[] params = {mImageUrl, mThumbnailUrl};
                        //new DeleteImageAsyncTask().execute(params);

                        try {
                            //delete files under Pictures
                            File imageFile = new File(mImageUrl);
                            File thumbnailFile = new File(mThumbnailUrl);
                            //delete thumbnail first for better ux
                            if (thumbnailFile.delete() && imageFile.delete()) {
                                List<Object> data = new ArrayList<Object>();
                                //delete thumbnail first
                                data.add(mThumbnailUrl);
                                data.add(mImageUrl);
                                //delete files in the cache
                                mImageFetcher.deleteImages(data, ImageDetailFragment.this);
                                //database
                                mDbHelper.deleteNote(mRowId);

                            } else {
                                Log.e(ImageDetailFragment.class.getName(), "Unable to delete files");
                                Toast.makeText(getActivity(), "Unable to delete",
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            Log.e(ImageDetailFragment.class.getName(), e.getMessage());
                        }

                        //getActivity().finish();
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

    @Override
    public void onImageLoaded(boolean success) {
        // Set loading spinner to gone once image has loaded. Cloud also show
        // an error view here if needed.
        mProgressBar.setVisibility(View.GONE);


    }

    //TODO: Load called multiple times, once for each grid item
    private void load() {
        Log.i(sTag, "load");
        try {
            if (mRowId != null) {
                Cursor cursor = mDbHelper.fetchNote(mRowId);
                getActivity().startManagingCursor(cursor);
                String ratingStr = cursor.getString(cursor
                        .getColumnIndexOrThrow(NotesDbAdapter.KEY_RATING));
                mRatingBar.setRating(Float.valueOf(ratingStr));
                mWine.setText(cursor.getString(cursor
                        .getColumnIndexOrThrow(NotesDbAdapter.KEY_WINE)));
                mTextExtract.setText(cursor.getString(cursor
                        .getColumnIndexOrThrow(NotesDbAdapter.KEY_TEXT_EXTRACT)));
                mNotes.setText(cursor.getString(cursor
                        .getColumnIndexOrThrow(NotesDbAdapter.KEY_NOTES)));
                String dateAdded = "Added on "
                        + mDateFormat.format(cursor.getLong(cursor
                        .getColumnIndex(NotesDbAdapter.KEY_CREATED)));
                mDateCreated.setText(dateAdded);
                String dataUpdated = "Last updated on "
                        + mDateFormat.format(cursor.getLong(cursor
                        .getColumnIndex(NotesDbAdapter.KEY_UPDATED)));
                mDateUpdated.setText(dataUpdated);
            }
        } catch (Throwable e) {
            Log.e(sTag, "error: "
                    + ((e.getMessage() != null) ? e.getMessage().replace(" ",
                    "_") : ""), e);
        }
    }

    private void save() {
        try {
            Note note = new Note();
            note.id = mRowId;
            note.wine = mWine.getText().toString();
            note.rating = String.valueOf(mRatingBar.getRating());
            note.textExtract = mTextExtract.getText().toString();
            note.notes = mNotes.getText().toString();
            //default to Y
            note.share = "Y";
            note.picture = mRowId + Utils.PICTURES_EXTENSION;
            //note.share = mShare.isChecked() ? "Y" : "N";
            if (mIsNew) {
                mDbHelper.createNote(note);
                mIsNew = false;
            } else {
                mDbHelper.updateNote(note);
            }

            // Do the real work in an async task, because we need to use the network anyway
//            new android.os.AsyncTask<Object, Void, String>() {
//                @Override
//                protected String doInBackground(Object... params) {
//                    try {
//                        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
//                        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
//                        NotesDbAdapter dbHelper = null;
//                        Note note = (Note)params[0];
//
//
//                        return null;
//
//                    } catch (Throwable e) {
//                        com.cm.android.common.logger.Log.d("ASYNCTASK", "failed to make API request because of " +
//                                e.getMessage());
//                    }
//                    return "Cloud Vision API request failed. Check logs for details.";
//                }
//
//                protected void onPostExecute(String result) {
//                    //mImageDetails.setText(result);
//                    com.cm.android.common.logger.Log.d("ASYNCTASK", "RESULT " +
//                            result);
//                }
//
//
//            }.execute(note);

        } catch (Throwable e) {
            Log.e(sTag, "error: "
                    + ((e.getMessage() != null) ? e.getMessage().replace(" ",
                    "_") : ""), e);
        }

    }

    @Override
    public void onImageDeleted(boolean success) {
        //getActivity().getSupportFragmentManager().popBackStack();
        mProgressBar.setVisibility(View.GONE);
        getActivity().finish();
    }


    /**
     * @author anshu
     */
    private class DeleteImageAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String mImageUrl = strings[0];
            String mImageThumbnailUrl = strings[1];

            try {
                //delete files under Pictures
                File imageFile = new File(mImageUrl);
                File thumbnailFile = new File(mImageUrl);
                //delete thumbnail first for better ux
                if (thumbnailFile.delete() && imageFile.delete()) {
                    List<Object> data = new ArrayList<Object>();
                    //delete thumbnail first
                    data.add(mImageThumbnailUrl);
                    data.add(mImageUrl);
                    //delete files in the cache
                    mImageFetcher.deleteImages(data, ImageDetailFragment.this);
                } else {
                    Log.e(ImageDetailFragment.class.getName(), "Unable to delete files");
                    Toast.makeText(getActivity(), "Unable to delete",
                            Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Log.e(ImageDetailFragment.class.getName(), e.getMessage());
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }


}

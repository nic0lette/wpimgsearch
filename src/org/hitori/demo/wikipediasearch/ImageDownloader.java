
package org.hitori.demo.wikipediasearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageDownloader extends
        AsyncTask<ImageDownloader.ViewDownloadRequest, ImageDownloader.ProgressData, Void> {
    private static final String LOG_TAG = ImageDownloader.class.getSimpleName();

    private final Context mContext;

    public ImageDownloader(final Context context) {
        // Save the context for stuff later
        mContext = context;
    }

    @Override
    protected Void doInBackground(ViewDownloadRequest... requests) {
        for (final ViewDownloadRequest request : requests) {
            // Hash the name of the file (that we'll use caching)
            final String filename = createCacheFilename(request.url);
            final File file = (mContext.getExternalCacheDir() == null) ? null : new File(mContext.getExternalCacheDir(), filename);

            // Image data
            final Bitmap image;

            // Do we have it already?
            if (file != null && file.exists() && file.canRead() && file.isFile()) {
                // Cached - just use this and go
                image = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                // Download the image from the server
                image = downloadImage(request.url);

                // Write the data out to the file (based on:
                // http://stackoverflow.com/questions/649154/save-bitmap-to-location)
                if (file != null) {
                    // Only try if there actually is a cache dir
                    try {
                        final FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
                        image.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                    } catch (final Exception e) {
                        Log.w(LOG_TAG, String.format("Could not cache %s", request.url), e);
                    }
                }
            }

            // Report progress
            if (image != null) {
                publishProgress(new ProgressData(request, image));
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(final ProgressData... progress) {
        // I can't imagine why it wouldn't just publish one at a time but
        // *shrug*
        for (final ProgressData pd : progress) {
            // This code is highly coupled and it would be better to do it,
            // really, through a custom view
            // or at least a callback or interface, but it's taking so much time
            // I want to at least get it
            // semi-working/demo-ish
            final TextView textView = (TextView) pd.request.view.findViewById(R.id.page_tv);
            final ImageView imgView = (ImageView) pd.request.view.findViewById(R.id.page_iv);

            // First, set the image, make it visible, then remove the text
            // placeholder
            imgView.setImageBitmap(pd.image);
            imgView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);

            // Update the content description on the image while we're at it
            final WikiPage page = (WikiPage) pd.request.view.getTag();
            imgView.setContentDescription(mContext.getResources().getString(R.string.page_image,
                    page.title));
        }
    }

    /**
     * Interface class for passing in download requests to the async tasks's
     * background thread
     * 
     * @author niya
     */
    public static class ViewDownloadRequest {
        public final String url;
        public final View view;

        public ViewDownloadRequest(final String url, final View view) {
            this.url = url;
            this.view = view;
        }
    }

    /**
     * Helper class to pass back data from the background thread to the UI
     * thread
     * 
     * @author niya
     */
    protected static class ProgressData {
        public final ViewDownloadRequest request;
        public final Bitmap image;

        public ProgressData(final ViewDownloadRequest request, final Bitmap image) {
            this.request = request;
            this.image = image;
        }
    }

    private Bitmap downloadImage(final String url) {
        // Get the default client
        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpGet httpget = new HttpGet(url);

        // Execute the request
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);

            // Get hold of the response entity
            final HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {

                // Get an input stream for the content
                final InputStream instream = entity.getContent();
                final Bitmap image = BitmapFactory.decodeStream(instream);

                // Either way the stream is done
                instream.close();

                // Send back the image
                return image;
            }
        } catch (final Exception e) {
            Log.e(LOG_TAG, "API call failed", e);
        }

        // If we get here, something broke, so there's no image
        return null;
    }

    /*
     * Helper method, based on
     * http://stackoverflow.com/questions/4846484/md5-or-
     * other-hashing-in-android
     */
    public static final String createCacheFilename(final String url) {
        try {
            // Create SHA hash
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(url.getBytes());
            final byte messageDigest[] = digest.digest();

            // Create Hex String
            final StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }

            // Whatever the format on the server, we'll cache them as JPEGs
            return String.format("%s.jpg", hexString.toString());

        } catch (final NoSuchAlgorithmException e) {
            Log.w(LOG_TAG, "Couldn't find message digest algorithm", e);
        }
        return null;
    }
}

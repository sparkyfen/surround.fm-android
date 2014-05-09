package com.surroundfm.surroundfm.app;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created on 5/7/14.
 */
public class CustomList extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> web;
    private final ArrayList<String> imageId;
    private final ImageLoader mImageLoader;

    public CustomList(Activity context, ArrayList<String> web, ArrayList<String> imageId, ImageLoader mImageLoader) {
        super(context, R.layout.list_single, web);
        this.context = context;
        this.web = web;
        this.imageId = imageId;
        this.mImageLoader = mImageLoader;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // Logging
        final String LOGTAG = context.getResources().getString(R.string.log_tag_custom_list);

        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_single, null, true);
        // Get username view
        TextView text = (TextView) rowView.findViewById(R.id.username_item);

        // Set text to incoming username.
        text.setText(web.get(position));

        // Set image to incoming url.
        String tempImage = imageId.get(position);
        try {
            URI tempImageURI = new URI(tempImage);
            // Get network  image view
            NetworkImageView imageView = (NetworkImageView) rowView.findViewById(R.id.avatar);
            if(tempImageURI.isAbsolute()) {
                Log.d(LOGTAG, "Image was absolute: " + tempImage);
                imageView.setImageUrl(tempImage, mImageLoader);
            } else {
                Log.d(LOGTAG, "Image was relative: " + tempImage);
                imageView.setImageResource(R.drawable.default_avatar);
            }
        } catch (URISyntaxException e) {
            Log.e(LOGTAG, "URL is invalid for image.");
        }
        return rowView;
    }


}

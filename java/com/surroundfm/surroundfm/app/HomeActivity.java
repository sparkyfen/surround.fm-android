package com.surroundfm.surroundfm.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class HomeActivity extends ListActivity {
    private ListView mUserListView;
    private View mProgressView;

    private ObjectMapper mapper;
    private User user;
    RequestQueue queue;

    private LocationManager locationManager;
    private LocationListener locationListener;


    // 5 Miles.
    private static final float MIN_DISTANCE = 8046.72f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUserListView = (ListView) findViewById(android.R.id.list);
        mProgressView = findViewById(R.id.geo_progress);

        // Setup Google Volley HTTP queue.
        queue = Volley.newRequestQueue(getApplicationContext());
        // Show progress while we load the list of user's around them.
        showProgress(true);

        // Create location listener for GPS.
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
            @Override
            public void onProviderEnabled(String provider) {

            }
            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        // Get location manager to work with GPS.
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Request location changes based on whatever the static float is.
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, MIN_DISTANCE, locationListener);
        // Get user string from login activity.
        String userSerialized = getIntent().getStringExtra("user");
        mapper  = new ObjectMapper();
        try {
            user = mapper.readValue(userSerialized, User.class);
            getCloseUsers();
        } catch (JsonMappingException e) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_deserial), Toast.LENGTH_SHORT).show();
            finish();
        } catch (JsonParseException e) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_deserial), Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_deserial), Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Kill the HTTP request queue.
        queue.cancelAll("update-location");
        queue.cancelAll("close-users");

        // Remove the GPS listener we previously added.
        locationManager.removeUpdates(locationListener);
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mUserListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mUserListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUserListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mUserListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Send request to API backend to update the user's location.
     *
     * @param newLocation
     *   The location of the user.
     */
    private void updateLocation(Location newLocation) {

        final double latitude = newLocation.getLatitude();
        final double longitude = newLocation.getLongitude();
        // URL of the request.
        final String API_GEO = getResources().getString(R.string.api_updateGeo);

        StringRequest rq = new StringRequest(Request.Method.POST, API_GEO, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // We don't need the response because it is supposed to run in the background.
                Log.d("GEO Response: ", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                try {
                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                    JSONObject errorResp = new JSONObject(responseBody);
                    if(volleyError.networkResponse.statusCode == 401) {
                        // Unauthorized request, login again.
                        Toast.makeText(getApplicationContext(), errorResp.getString("message"), Toast.LENGTH_SHORT).show();
                        Intent loginIntent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(loginIntent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), errorResp.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not update GEO location.", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not update GEO location.", Toast.LENGTH_SHORT).show();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("lat", String.valueOf(latitude));
                params.put("lng", String.valueOf(longitude));
                return params;
            };

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = super.getHeaders();
                if(params == null || params.equals(Collections.emptyMap())) {
                    params = new HashMap<String, String>();
                }
                params.put("Cookie", "connect.sid=" + user.getCookie() + ";");
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        rq.setTag("update-location");
        queue.add(rq);
    }

    private void getCloseUsers() {
        final String API_CLOSEUSERS = getResources().getString(R.string.api_getCloseUsers);
        final String LOGTAG = getResources().getString(R.string.log_tag_home);
        StringRequest rq = new StringRequest(Request.Method.GET, API_CLOSEUSERS, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    Log.d(LOGTAG, s);
                    JSONObject userObj = new JSONObject(s);
                    JSONArray userList = userObj.getJSONArray("users");

                    final ArrayList<String> usernames = new ArrayList<String>();
                    final ArrayList<String> avatars = new ArrayList<String>();
                    for(int i = 0; i < userList.length(); i++) {
                        JSONObject tempUser = userList.getJSONObject(i);
                        usernames.add(tempUser.get("_id").toString());
                        avatars.add(tempUser.get("avatar").toString());
                    }
                    // Create Google Volley Image loader.
                    final ImageLoader mImageLoader = createImageLoader();
                    CustomList adapter = new CustomList(HomeActivity.this, usernames, avatars, mImageLoader);
                    mUserListView.setAdapter(adapter);
                    mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // TODO: Deal with when we click the item.
                            Toast.makeText(getApplicationContext(), "You clicked " + usernames.get(position), Toast.LENGTH_SHORT).show();
                        }
                    });
                    showProgress(false);
                    // TODO: If we need to add more elements, use adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Could not get user list.", Toast.LENGTH_SHORT).show();
                    Log.e(LOGTAG, e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                try {
                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                    JSONObject errorResp = new JSONObject(responseBody);
                    if (volleyError.networkResponse.statusCode == 401) {
                        // Removing user from local storage.
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.remove("user");
                        editor.commit();

                        // Unauthorized request, login again.
                        Toast.makeText(getApplicationContext(), errorResp.getString("message"), Toast.LENGTH_SHORT).show();

                        Intent loginIntent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(loginIntent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), errorResp.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(getApplicationContext(), "Could not get user list.", Toast.LENGTH_SHORT).show();
                    Log.e(LOGTAG, e.getMessage());
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Could not get user list.", Toast.LENGTH_SHORT).show();
                    Log.e(LOGTAG, e.getMessage());
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = super.getHeaders();
                if(params == null || params.equals(Collections.emptyMap())) {
                    params = new HashMap<String, String>();
                }
                params.put("Cookie", "connect.sid=" + user.getCookie() + ";");
                return params;
            }
        };
        rq.setTag("close-users");
        queue.add(rq);
    }

    private ImageLoader createImageLoader() {
        // Logging
        final String LOGTAG = getResources().getString(R.string.log_tag_home);
        // API Base URL
        final String API_BASE_URL = getResources().getString(R.string.api_base_url);
        ImageLoader mImageLoader = new ImageLoader(queue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);
            @Override
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                try {
                    final URI u = new URI(url);
                    if(u.isAbsolute()) {
                        mCache.put(url, bitmap);
                    }
                    // Don't deal with non-absolute urls, Android should just use the default.
                } catch (URISyntaxException e) {
                    Log.e(LOGTAG, "URL is invalid for image.");
                }
            }
        });
        return mImageLoader;
    }

    private void logout() {
        final String API_LOGOUT = getResources().getString(R.string.api_logout);
        StringRequest rq = new StringRequest(Request.Method.GET, API_LOGOUT, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                String message;
                try {
                    JSONObject logout_resp = new JSONObject(s);
                    message = logout_resp.getString("message");
                } catch (JSONException e) {
                    message = "Failed to logout, redirecting to login.";
                }
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                // Removing user from local storage.
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("user");
                editor.commit();
                Intent loginIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(loginIntent);
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getApplicationContext(), "Failed to logout, redirecting to login.", Toast.LENGTH_SHORT).show();
                // Removing user from local storage.
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("user");
                editor.commit();
                Intent loginIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(loginIntent);
                finish();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = super.getHeaders();
                if(params == null || params.equals(Collections.emptyMap())) {
                    params = new HashMap<String, String>();
                }
                params.put("Cookie", "connect.sid=" + user.getCookie() + ";");
                return params;
            }
        };
        queue.add(rq);
    }

}

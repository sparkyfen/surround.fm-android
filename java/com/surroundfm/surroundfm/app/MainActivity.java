package com.surroundfm.surroundfm.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A login screen that offers login via user/password.

 */
public class MainActivity extends Activity {

    // UI references.
    private EditText mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mMainView;


    // User Object.
    private User user;
    private String cookie;

    // Data Storage.
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up storage.
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = preferences.edit();

        // Setup user
        user = new User();

        // Bypass login if user established.
        String userObj = preferences.getString("user", null);
        if(userObj != null) {
            Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
            homeIntent.putExtra("user", userObj);
            startActivity(homeIntent);
            finish();
        }
        // Set up the login form.
        mUserView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.username_sign_in_button || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mUserSignInButton = (Button) findViewById(R.id.username_sign_in_button);
        mUserSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.registerBtn);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(registerIntent);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mMainView = findViewById(R.id.main_layout);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        } else if (!isUserValid(username)) {
            mUserView.setError(getString(R.string.error_invalid_username));
            focusView = mUserView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            login(username, password);
        }
    }
    private boolean isUserValid(String username) {
        return true;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
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

            mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    private void login(final String username, final String password) {
        final String LOGTAG = getResources().getString(R.string.log_tag_main);
        final String API_LOGIN = getResources().getString(R.string.api_login);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest rq = new StringRequest(Request.Method.POST, API_LOGIN, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                showProgress(false);
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JSONObject respObj = new JSONObject(response);
                    JSONObject userObj = respObj.getJSONObject("user");
                    user = mapper.readValue(userObj.toString(), User.class);
                    user.setCookie(cookie);
                    String userStr = mapper.writeValueAsString(user);
                    // Store user in sharedPreferences.
                    editor.putString("user", userStr);
                    editor.commit();
                    // Parse message
                    JSONObject messageObj = new JSONObject(response);
                    // Show message from log in.
                    Toast.makeText(getApplicationContext(), messageObj.getString("message"), Toast.LENGTH_SHORT).show();
                    // Start intent for home activity.
                    Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                    homeIntent.putExtra("user", userStr);
                    startActivity(homeIntent);
                    finish();
                } catch (IOException e) {
                    Log.e(LOGTAG, String.valueOf(e.getMessage()));
                    mUserView.setError(getString(R.string.error_login));
                    mUserView.requestFocus();
                } catch (JSONException e) {
                    Log.e(LOGTAG, String.valueOf(e.getMessage()));
                    mUserView.setError(getString(R.string.error_login));
                    mUserView.requestFocus();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showProgress(false);
                try {
                    // TODO If the error is for the username, we have to focus on that.
                    String responseBody = new String(error.networkResponse.data, "utf-8" );
                    JSONObject errorObject = new JSONObject(responseBody);
                    mPasswordView.setError(errorObject.getString("message"));
                } catch (UnsupportedEncodingException e) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                } catch (JSONException e) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                }
                mPasswordView.requestFocus();
            }
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("password", password);
                params.put("rememberMe", "true");
                return  params;
            };

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = super.getHeaders();
                if(params == null || params.equals(Collections.emptyMap())) {
                    params = new HashMap<String, String>();
                }
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse resp) {
                Map<String, String> headers = resp.headers;
                if(headers.containsKey("set-cookie")) {
                    String localCookie = headers.get("set-cookie");
                    String[] splitCookie = localCookie.split(";");
                    boolean found = false;
                    for(int i = 0; i < splitCookie.length; i++) {
                        String[] splitSession = splitCookie[i].split("=");
                        // NodeJS (ExpressJS) Cookie
                        if (splitSession[0].equals("connect.sid")) {
                            found = true;
                            localCookie = splitSession[1];
                        }
                    }
                    if(!found) {
                        Log.e(LOGTAG, "Missing cookie from login request.");
                    } else {
                        cookie = localCookie;
                    }
                }
                return super.parseNetworkResponse(resp);
            }
        };
        rq.setShouldCache(true);
        queue.add(rq);
    }
}



package com.surroundfm.surroundfm.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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
 * Created on 5/9/14.
 */
public class LinkAccountPreference extends DialogPreference {

    // View elements.
    private EditText mAccountName;
    private Button mOkButton;
    private Button mCancelButton;

    // Google Volley HTTP queue.
    private RequestQueue queue;

    // User object.
    private User user;

    // Logging.
    private final String LOGTAG = getContext().getResources().getString(R.string.log_tag_password);

    // Data Storage.
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public LinkAccountPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPersistent(false);
        setDialogLayoutResource(R.layout.link_accounts);
    }

    public LinkAccountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.link_accounts);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setTitle(R.string.pref_title_link_accounts);
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);

        // Setup Google Volley HTTP queue.
        queue = Volley.newRequestQueue(getContext().getApplicationContext());

        // Setup user object
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        editor = preferences.edit();

        String userSerialized = preferences.getString("user", null);
        // Can't find user in preferences, head back to login screen.
        if(userSerialized == null) {
            errorDialog("Could not find user in database.");
        } else {
            // Setup user
            ObjectMapper mapper = new ObjectMapper();
            try {
                user = mapper.readValue(userSerialized, User.class);
            } catch (IOException e) {
                Log.e(LOGTAG, e.getMessage());
                errorDialog("Could not find user in database.");
            }
        }
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onBindDialogView(View view) {
        mAccountName = (EditText) view.findViewById(R.id.link_accounts_text);
        mOkButton = (Button) view.findViewById(R.id.link_accounts_ok_button);
        mCancelButton = (Button) view.findViewById(R.id.link_accounts_cancel_button);

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lastfmName = mAccountName.getText().toString();
                linkAccounts(lastfmName);
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        // Cancel HTTP request.
        queue.cancelAll("link-account");

        persistBoolean(positiveResult);
    }

    private void linkAccounts(final String lastfmName) {
        final String API_LINK_ACCOUNTS = getContext().getResources().getString(R.string.api_linkAccounts);
        StringRequest rq = new StringRequest(Request.Method.POST, API_LINK_ACCOUNTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject response = new JSONObject(s);
                    String message = response.getString("message");
                    Toast.makeText(getContext().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                } catch (JSONException e) {
                    mAccountName.setError("Accounts could not be linked, try again.");
                    mAccountName.requestFocus();
                    Log.e(LOGTAG, e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(volleyError.networkResponse.data == null) {
                    mAccountName.setError("Accounts could not be linked, try again.");
                    mAccountName.requestFocus();
                    return;
                }
                try {
                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                    JSONObject errorObj = new JSONObject(responseBody);
                    String message = errorObj.getString("message");
                    // Unauthorized request, login again.
                    if(volleyError.networkResponse.statusCode == 401) {
                        // Remove user from preferences.
                        editor.remove("user");
                        editor.commit();
                        // Head to login activity.
                        errorDialog(message);
                    } else {
                        mAccountName.setError(message);
                    }
                } catch (UnsupportedEncodingException e) {
                    mAccountName.setError("Accounts could not be linked, try again.");
                    Log.e(LOGTAG, e.getMessage());
                } catch (JSONException e) {
                    mAccountName.setError("Accounts could not be linked, try again.");
                    Log.e(LOGTAG, e.getMessage());
                }
                mAccountName.requestFocus();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("lastFmUser", lastfmName);
                return params;
            };

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
        rq.setTag("link-account");
        queue.add(rq);
    }

    /**
     * Displays an error about not parsing/finding the user in the database and then heads back to login activity.
     *
     * @param message
     *     The toast message.
     */
    private void errorDialog(String message) {
        Toast.makeText(getContext().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Intent mainIntent = new Intent(getContext().getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getContext().startActivity(mainIntent);
    }

}

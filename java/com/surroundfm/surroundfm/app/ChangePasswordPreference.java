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
 * Created on 5/8/14.
 */
public class ChangePasswordPreference extends DialogPreference {

    // View elements.
    private EditText mCurrentPassword;
    private EditText mNewPassword;
    private EditText mNewConfirmPassword;
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

    public ChangePasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.change_password);
    }

    public ChangePasswordPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPersistent(false);
        setDialogLayoutResource(R.layout.change_password);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setTitle(R.string.pref_title_change_password);
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);

        // Setup Google Volley HTTP queue.
        queue = Volley.newRequestQueue(getContext().getApplicationContext());

        // Setup user object
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        editor = preferences.edit();

        String userSerialized = preferences.getString("user", null);
        Log.d(LOGTAG, userSerialized);
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

        mCurrentPassword = (EditText) view.findViewById(R.id.change_password_current_password);
        mNewPassword = (EditText) view.findViewById(R.id.change_password_new_password);
        mNewConfirmPassword = (EditText) view.findViewById(R.id.change_password_new_confirm_password);

        mOkButton = (Button) view.findViewById(R.id.change_password_ok_button);
        mCancelButton = (Button) view.findViewById(R.id.change_password_cancel_button);

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPass = mCurrentPassword.getText().toString();
                String newPass = mNewPassword.getText().toString();
                String confirmPass = mNewConfirmPassword.getText().toString();
                changePassword(currentPass, newPass, confirmPass);
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        super.onBindDialogView(view);
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        // Cancel HTTP request.
        queue.cancelAll("change-password");

        persistBoolean(positiveResult);
    }

    /**
     * Makes HTTP POST request to change the user's password.
     *
     * @param currentPassword
     *     The current user's password.
     * @param newPassword
     *     The new user's password.
     * @param confirmNewPassword
     *     The confirmation of the new user's password.
     */
    private void changePassword(final String currentPassword, final String newPassword, final String confirmNewPassword) {
        // API call to change password.
        final String API_CHANGE_PASSWORD = getContext().getResources().getString(R.string.api_changePassword);
        StringRequest rq = new StringRequest(Request.Method.POST, API_CHANGE_PASSWORD, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject response = new JSONObject(s);
                    String message = response.getString("message");
                    Toast.makeText(getContext().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                    Intent homeIntent = new Intent(getContext().getApplicationContext(), MainActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getContext().startActivity(homeIntent);
                } catch (JSONException e) {
                    Toast.makeText(getContext().getApplicationContext(), "Error parsing response.", Toast.LENGTH_SHORT).show();
                    Log.e(LOGTAG, e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // Parse response from server and display error to first input value.
                try {
                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                    JSONObject errorObject = new JSONObject(responseBody);
                    String message = errorObject.getString("message");
                    // Unauthorized request, login again.
                    if(volleyError.networkResponse.statusCode == 401) {
                        // Remove user from preferences.
                        editor.remove("user");
                        editor.commit();
                        // Head to login activity.
                        errorDialog(message);
                    } else {
                        mCurrentPassword.setError(message);
                    }
                } catch (UnsupportedEncodingException e) {
                    mCurrentPassword.setError(getContext().getString(R.string.error_incorrect_password));
                    Log.e(LOGTAG, e.getMessage());
                } catch (JSONException e) {
                    mCurrentPassword.setError(getContext().getString(R.string.error_incorrect_password));
                    Log.e(LOGTAG, e.getMessage());
                }
                mCurrentPassword.requestFocus();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("oldPassword", currentPassword);
                params.put("newPassword", newPassword);
                params.put("newConfirmPassword", confirmNewPassword);
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
        rq.setTag("change-password");
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

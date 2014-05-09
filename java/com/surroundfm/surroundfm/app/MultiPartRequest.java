package com.surroundfm.surroundfm.app;

import android.webkit.MimeTypeMap;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 5/8/14.
 * References:
 * https://gist.github.com/mbarcia/8660924#file-photomultipartrequest-java
 * https://github.com/smanikandan14/Volley-demo
 *
 */
public class MultiPartRequest extends StringRequest {

    private Map<String, File> fileUploads = new HashMap<String, File>();
    private Map<String, String> stringUploads = new HashMap<String, String>();
    private Map<String, String> headers = new HashMap<String, String>();

    private MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();

    public MultiPartRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(url, listener, errorListener);

        buildMultipartEntity();
    }

    public MultiPartRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);

        buildMultipartEntity();
    }

    public void addFileUpload(String param, File file) {
        fileUploads.put(param, file);
    }

    public void addStringUpload(String param, String content) {
        stringUploads.put(param, content);
    }

    public Map<String, File> getFileUploads() {
        return fileUploads;
    }

    public Map<String, String> getStringUploads() {
        return stringUploads;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }

    public void setHeader(String title, String content) {
        headers.put(title, content);
    }

    @Override
    public String getBodyContentType() {
        String contentTypeHeader = mBuilder.build().getContentType().getValue();
        return contentTypeHeader;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            mBuilder.build().writeTo(bos);
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream bos, building the multi-part request.");
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return  Response.success(jsonString, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }

    /**
     * Return the mime-type of a file. If not mime-type can be found, it returns null.
     * http://stackoverflow.com/a/8591230
     *
     * @param file
     *   The file to check for it's mime-type.
     * @return
     *   The mime-type as a string.
     */
    private static String getMimeType(File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if(extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }


    // TODO Need to fix this request so that the backend can recognize it.
    private void buildMultipartEntity() {
        // Iterate over each file and add it to the multi-part entity request.
        for(Map.Entry entry : fileUploads.entrySet()) {
            String tempFileParam = entry.getKey().toString();
            File tempFile = (File) entry.getValue();
            String mimeType = getMimeType(tempFile);

            if(mimeType != null) {
                mBuilder.addBinaryBody(tempFileParam, tempFile, ContentType.create(mimeType), tempFile.getName());
            }
        }
        // Iterate over each string param and add it to the request.
        for(Map.Entry entry : stringUploads.entrySet()) {
            String tempParam = entry.getKey().toString();
            String tempValue = entry.getValue().toString();
            mBuilder.addTextBody(tempParam, tempValue);
        }
        mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        mBuilder.setLaxMode().setBoundary("xx").setCharset(Charset.forName("UTF-8"));
    }

}

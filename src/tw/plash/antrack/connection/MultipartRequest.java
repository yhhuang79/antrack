package tw.plash.antrack.connection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import tw.plash.antrack.Utility;
import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

public class MultipartRequest extends Request<JSONObject> {
	
	private MultipartEntity entity;
	
	private final Response.Listener<JSONObject> mListener;
	private final File filepart;
	
	public MultipartRequest(String url, List<NameValuePair> params, File file, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener
			) throws IOException {
		super(Method.POST, url, errorListener);
		
		mListener = listener;
		entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		filepart = file;
		for(NameValuePair nvp : params){
			entity.addPart(nvp.getName(), new StringBody(nvp.getValue()));
		}
		buildMultipartEntity();
	}
	
	private void buildMultipartEntity() throws IOException {
		String path = filepart.getAbsolutePath();
		Bitmap bitmap = Utility.getThumbnail(path, 1024);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] bytes = baos.toByteArray();
		ByteArrayBody bab = new ByteArrayBody(bytes, path.substring(path.lastIndexOf("/") + 1));
		entity.addPart("picture", bab);
	}
	
	@Override
	public String getBodyContentType() {
		return entity.getContentType().getValue();
	}
	
	@Override
	public byte[] getBody() throws AuthFailureError {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			entity.writeTo(bos);
		} catch (IOException e) {
			VolleyLog.e("IOException writing to ByteArrayOutputStream");
		}
		return bos.toByteArray();
	}
	
	@Override
	protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		} catch (JSONException je) {
			return Response.error(new ParseError(je));
		}
	}
	
	@Override
	protected void deliverResponse(JSONObject arg0) {
		mListener.onResponse(arg0);
	}
}
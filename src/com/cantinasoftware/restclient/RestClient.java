package com.cantinasoftware.restclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.PersistentCookieStore;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
public class RestClient {
	private static final String TAG = "RestClient";
	
	private static RestClient sSharedClient;

	public static RestClient getSharedClient(Context context) {
		if (null == sSharedClient)
			sSharedClient = new RestClient(null, context, null);
		return sSharedClient;
	}

	public static RestClient clientWithBaseURL(URL url, Context context, Option[] options) {
		return new RestClient(url, context, options);
	}

	public static RestClient clientWithBaseURL(String url, Context context, Option[] options) throws MalformedURLException {
		return clientWithBaseURL(new URL(url), context, options);
	}

	public static RestClient clientWithBaseURL(URL url, Context context) {
		return new RestClient(url, context, null);
	}

	public static RestClient clientWithBaseURL(String url, Context context) throws MalformedURLException {
		return clientWithBaseURL(new URL(url), context, null);
	}


	public static void setSharedClient(RestClient client) {
		sSharedClient = client;
	}
	
	private Context mContext;
	private URL mBaseURL;
	private DefaultHttpClient mHttpClient;
	private PersistentCookieStore mCookieStore;
	private int mConnectionTimeout = 1000;
	private int mSocketTimeout = 1000;
	private Request mDefaultRequest;
	
	private RestClient(URL baseURL, Context context, Option[] options) {
		mContext  = context.getApplicationContext();
		mBaseURL = baseURL;
		
		mHttpClient = buildHttpClient(options);
		mCookieStore = new PersistentCookieStore(mContext);
		mHttpClient.setCookieStore(mCookieStore);
		setHttpClientTimeoutParameters();
	}

	private URL makeURL(String resource) throws MalformedURLException {
		return new URL(String.format("%s%s", mBaseURL.toString(), resource));
	}

	public URL getBaseURL() {
		return mBaseURL;
	}

	public RestClient setBaseURL(URL u) {
		mBaseURL = u;
		return this;
	}

	public HttpClient getHttpClient() {
		return mHttpClient;
	}
	
	public RestClient setBaseURL(String url) throws MalformedURLException {
		return setBaseURL(new URL(url));
	}

	public int getConnectionTimeout() {
		return mConnectionTimeout;
	}

	public RestClient setConnectionTimeout(int mConnectionTimeout) {
		this.mConnectionTimeout = mConnectionTimeout;
		setHttpClientTimeoutParameters();
		return this;
	}

	public int getSocketTimeout() {
		return mSocketTimeout;
	}

	public RestClient setSocketTimeout(int mSocketTimeout) {
		this.mSocketTimeout = mSocketTimeout;
		setHttpClientTimeoutParameters();
		return this;
	}

	private void setHttpClientTimeoutParameters() {
		HttpConnectionParams.setConnectionTimeout(this.mHttpClient.getParams(), this.getConnectionTimeout());
		HttpConnectionParams.setSoTimeout(this.mHttpClient.getParams(), this.getConnectionTimeout());
	}
	
	public Request getDefaultRequest() {
		return this.mDefaultRequest;
	}

	public void setDefaultRequest(Request defaultRequest) {
		// The default request can be set to null to clear the constant headers
		this.mDefaultRequest = defaultRequest;
	}
	
	protected void send(Request.Method method, String resource, Params params,
			Request.Listener listener) throws MalformedURLException {
		Request request = prepareRequest(method, resource);
		request.setParams(params);
		request.setListener(listener);
		new DownloadTask(this, request).execute();
	}

	protected void send(Request.Method method, String resource, Block block)
			throws MalformedURLException {
		Request request = prepareRequest(method, resource);
		block.execute(request);
		new DownloadTask(this, request).execute();
	}
	
	public Response sendSynchroneous(Request.Method method, String resource, Block block) 
		throws Exception {
		Request request = prepareRequest(method, resource);
		block.execute(request);
		DownloadTask task = new DownloadTask(this, request);
		task.doInBackground();
		task.execute();
		DownloadTaskResult result = task.mResult;
		if (null != result.mException)
			throw result.mException;
		return result.mResponse;
	}

	private Request prepareRequest(Request.Method method, String resource)
			throws MalformedURLException {
		Request request = new Request(makeURL(resource));
		request.setMethod(method);
		if(this.mDefaultRequest != null) {
			// Add constant headers from the default request
			for (NameValuePair header : this.mDefaultRequest.getHeaders())
				request.addHeader(header.getName(), header.getValue());
		}
		
		return request;
	}
	
	public void get(String resource, Params params, Request.Listener listener)
			throws MalformedURLException {
		send(Request.Method.GET, resource, params, listener);
	}

	public void get(String resource, Block block) throws MalformedURLException {
		send(Request.Method.GET, resource, block);
	}

	public void put(String resource, Params params, Request.Listener listener)
			throws MalformedURLException {
		send(Request.Method.PUT, resource, params, listener);
	}

	public void put(String resource, Block block) throws MalformedURLException {
		send(Request.Method.PUT, resource, block);
	}

	public void post(String resource, Params params, Request.Listener listener)
			throws MalformedURLException {
		send(Request.Method.POST, resource, params, listener);
	}

	public void post(String resource, Block block) throws MalformedURLException {
		send(Request.Method.POST, resource, block);

	}

	public void delete(String resource, Params params, Request.Listener listener)
			throws MalformedURLException {
		send(Request.Method.DELETE, resource, params, listener);
	}

	public void delete(String resource, Block block)
			throws MalformedURLException {
		send(Request.Method.DELETE, resource, block);

	}

	public List<Cookie> getCookies() {
		return this.mCookieStore.getCookies();
	}
	
	public List<Cookie> getCookies(String domain) {
		List<Cookie> cookieList = new ArrayList<Cookie>();
		for(Cookie c : this.mHttpClient.getCookieStore().getCookies()) {
			if(c.getDomain().compareTo(domain) == 0) {
				cookieList.add(c);
			}
		}
		
		return cookieList;
	}
	
	public void deleteCookies() {
		this.mCookieStore.clear();
	}

	public void deleteCookies(String domain) {
		List<Cookie> cookieList = new ArrayList<Cookie>();
		for(Cookie c : this.mCookieStore.getCookies()) {
			if(c.getDomain().compareTo(domain) != 0) {
				cookieList.add(c);
			}
		}
		
		// Remove all cookies
		this.mCookieStore.clear();
		
		// Add only those that do not match the specified domain
		for(Cookie c : cookieList) {
			this.mCookieStore.addCookie(c);
		}
	}
	
	public void deleteCookie(Cookie cookie) {
		List<Cookie> cookieList = new ArrayList<Cookie>();
		for(Cookie c : this.mCookieStore.getCookies()) {
			if(c != cookie) {
				cookieList.add(c);
			}
		}
		
		// Remove all cookies
		this.mCookieStore.clear();
		
		// Add all except the specified one
		for(Cookie c : cookieList) {
			this.mCookieStore.addCookie(c);
		}
	}
	
	public enum Option {
		DISABLE_CERTIFICATE_VALIDATION
	}
	
	private static DefaultHttpClient buildHttpClient(Option[] options) {
		boolean disableCertificateValidation = false;
		for (int i = 0; null != options && i < options.length; i++) {
			if (Option.DISABLE_CERTIFICATE_VALIDATION == options[i])
				disableCertificateValidation = true;
		}
		
		if (disableCertificateValidation) {
			HttpParams params = new BasicHttpParams();
			SchemeRegistry registry = new SchemeRegistry();
		    registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		    registry.register(new Scheme("https", new SelfSignedCertificatesHelpers.FakeSocketFactory(), 443));
		    return new DefaultHttpClient(new ThreadSafeClientConnManager(params, registry), params);
			
		} else {
			return new DefaultHttpClient();
		}
			
	}

	public static interface Block {
		public void execute(Request request);
	}

	public static class Response {
		private Request mRequest;
		private HttpResponse mHttpResponse;
		private ByteArrayOutputStream mResponseContent;
		protected Response(Request request, HttpResponse mResponse) throws IOException {
			mRequest = request;
			mHttpResponse = mResponse;
			mResponseContent = new ByteArrayOutputStream();
			mResponse.getEntity().writeTo(mResponseContent);
			mResponse.getEntity().consumeContent();
		}

		public void setRequest(Request r) {
			mRequest = r;
		}

		public Request getRequest() {
			return mRequest;
		}

		public String getBodyAsString() {
			return mResponseContent.toString();
		}

		public JSONObject getBodyAsJSONObject() throws IllegalStateException,
				JSONException, IOException {
			return new JSONObject(getBodyAsString());
		}

		public JSONArray getBodyAsJSONArray() throws IllegalStateException,
				JSONException, IOException {
			return new JSONArray(getBodyAsString());
		}

		public List<NameValuePair> getHeaders() {
			List<NameValuePair> result = new ArrayList<NameValuePair>();
			for (Header header : mHttpResponse.getAllHeaders())
				result.add(new BasicNameValuePair(header.getName(), header
						.getValue()));
			return null;
		}

		public int getStatus() {
			return mHttpResponse.getStatusLine().getStatusCode();
		}

	}

	public static class Request {
		public enum Method {
			GET, POST, PUT, DELETE
		}

		protected Request(URL url) {
			mURL = url;
		}

		public static interface Listener {
			public void requestDidLoad(Request request, Response response);

			public void requestDidFail(Request request, Exception error);
		}

		private Object mUserData;
		private Method mMethod;
		private URL mURL;
		private InputStream mBody;
		private long mBodyLength;
		private Params mParams;
		private File mFile;
		private String mFileMime;
		private Listener mListener;
		private List<NameValuePair> mHeaders = new ArrayList<NameValuePair>();

		public void addHeader(String name, String value) {
			mHeaders.add(new BasicNameValuePair(name, value));
		}

		public List<NameValuePair> getHeaders() {
			return mHeaders;
		}

		public void setParams(Params params) {
			mParams = params;
		}

		public Params getParams() {
			return mParams;
		}

		public URL getURL() {
			return mURL;
		}

		public void setURL(URL url) {
			mURL = url;
		}

		public void setBody(InputStream body, long length) {
			mBody = body;
			mBodyLength = length;
		}

		public void setBody(File file, String mimeType) {
			mFile = file;
			mFileMime = mimeType;
		}

		public Listener getListener() {
			return mListener;
		}

		public void setListener(Listener l) {
			mListener = l;
		}

		public void setMethod(Method method) {
			mMethod = method;
		}

		public Method getMethod() {
			return mMethod;
		}

		public HttpEntity getEntity() {
			if (null != mParams) {
				return mParams.toMultipartEntity();
			}
			if (null != mBody) {
				return new InputStreamEntity(mBody, mBodyLength);
			}
			if (null != mFile) {
				return new FileEntity(mFile, mFileMime);
			}
			return null;
		}

		public Object getUserData() {
			return mUserData;
		}

		public void setUserData(Object userData) {
			mUserData = userData;
		}
	}

	public static class Params {
		Map<String, List<ContentBody>> mContent = new HashMap<String, List<ContentBody>>();

		private List<ContentBody> getList(String name) {
			if (!mContent.containsKey(name))
				mContent.put(name, new ArrayList<ContentBody>());
			return mContent.get(name);
		}

		public boolean add(String name, String value) {
			return add(name, value, "text/plain", Charset.defaultCharset());
		}
		
		public boolean add(String name, String value, String mimeType, Charset charset) {
			try {
				getList(name).add(new StringBody(value, mimeType, charset));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, String.format("Unsupported encoding for param:%s",
						name), e);
				return false;
			}
			return true;
		}

		public boolean add(String name, File value) {
			getList(name).add(new FileBody(value));
			return true;
		}

		public boolean add(String name, File value, String mimeType) {
			getList(name).add(new FileBody(value, mimeType));
			return true;
		}

		public MultipartEntity toMultipartEntity() {
			MultipartEntity entity = new MultipartEntity();
			for (String name : mContent.keySet())
				for (ContentBody body : mContent.get(name))
					entity.addPart(name, body);
			return entity;
		}

		public UrlEncodedFormEntity toUrlEncodedFormEntity() throws IOException {
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			for (String name : mContent.keySet())
				for (ContentBody body : mContent.get(name))
					if (body instanceof StringBody)
						parameters.add(new BasicNameValuePair(name,
								contentBodyToString(body)));
			return new UrlEncodedFormEntity(parameters);
		}

		public HttpParams toHttpParams() throws IOException {
			BasicHttpParams params = new BasicHttpParams();
			for (String name : mContent.keySet()) {
				for (ContentBody body : mContent.get(name)) {
					if (body instanceof StringBody) {
						params.setParameter(name, contentBodyToString(body));
					}
				}
			}
			return params;
		}

		public int size() {
			int size = 0;
			for (String key : mContent.keySet())
				size += mContent.get(key).size();
			return size;
		}
	}

	private static class DownloadTaskResult {
		public Exception mException = null;
		public Response mResponse = null;
		public Request mRequest = null;

		public DownloadTaskResult(Request request, Response response,
				Exception exception) {
			mRequest = request;
			mResponse = response;
			mException = exception;
		}
	}

	private static class DownloadTask extends AsyncTask<Void, Void, Void> {

		WeakReference<RestClient> mClient;
		Request mRequest;
		DownloadTaskResult mResult;

		public DownloadTask(RestClient client, Request request) {
			mClient = new WeakReference<RestClient>(client);
			mRequest = request;
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (null == mRequest)
				return null;

			Request request = mRequest;
			try {

				HttpUriRequest uriRequest;
				switch (request.getMethod()) {
				case GET:
					HttpGet getRequest = new HttpGet(URIWithQueryString(
							request.getURL(), request.getParams()));
					uriRequest = getRequest;
					break;
				case POST:
					HttpPost postRequest = new HttpPost(request.getURL()
							.toURI());
					postRequest.setEntity(request.getEntity());
					uriRequest = postRequest;
					break;
				case PUT:
					HttpPut putRequest = new HttpPut(request.getURL().toURI());
					putRequest.setEntity(request.getEntity());
					uriRequest = putRequest;
					break;
				case DELETE:
					HttpDelete deleteRequest = new HttpDelete(request.getURL()
							.toURI());
					if(request.getParams() != null) {
						deleteRequest.setParams(request.getParams().toHttpParams());
					}
					uriRequest = deleteRequest;
					break;
				default:
					mResult = new DownloadTaskResult(request, null,
							new Exception("Unsupported method"));
					return null;
				}

				// Add headers
				for (NameValuePair header : request.getHeaders()) {
					uriRequest.addHeader(header.getName(), header.getValue());
				}
				if(!uriRequest.containsHeader("Accept-Language")) {
					uriRequest.addHeader("Accept-Language", 
							String.format("%s-%s", Locale.getDefault().getLanguage(), Locale.getDefault().getCountry().toLowerCase()));
				}
				
				Log.d(TAG, String.format("Sending request %s", uriRequest.getURI().toString()));
				mResult = new DownloadTaskResult(request, new Response(request,	
						this.mClient.get().getHttpClient().execute(uriRequest)), null);
				
				return null;

			} catch (Exception e) {
				this.mResult = new DownloadTaskResult(request, null, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Void r) {
			super.onPostExecute(r);

			DownloadTaskResult result = mResult;

			if (null == result) {
				return;
			}

			if (null != result.mException && null != result.mRequest) {
				Log.e(TAG, String.format("An error occurred: %s",
						result.mException.getMessage()), result.mException);
				if (null != result.mRequest.getListener())
					result.mRequest.getListener().requestDidFail(
							result.mRequest, result.mException);
				result.mRequest.setListener(null);
				result.mRequest = null;
				if (null != result.mResponse)
					result.mResponse.mRequest = null;
				result.mResponse = null;
				mResult = null;
				return;
			}

			if (null != result.mResponse && null != result.mRequest) {
				if (null != result.mRequest.getListener())
					result.mRequest.getListener().requestDidLoad(
							result.mRequest, result.mResponse);
				result.mRequest.setListener(null);
				result.mResponse.mRequest = null;
				result.mRequest = null;
				result.mResponse = null;
				mResult = null;
				return;
			}
			mResult = null;
		}

	}

	private static String contentBodyToString(ContentBody body)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		body.writeTo(out);
		return new String(out.toByteArray());
	}

	private static URI URIWithQueryString(URL url, Params params)
			throws ParseException, IOException, URISyntaxException {
		if (null == params || 0 == params.size())
			return new URI(url.toString());

		String queryString = EntityUtils.toString(params
				.toUrlEncodedFormEntity());

		String querySeparator = -1 == url.toString().indexOf('?') ? "?" : "&";
		String uri = String.format("%s%s%s", url.toString(), querySeparator,
				queryString);
		Log.d(TAG, uri);
		return new URI(uri);

	}
	

}

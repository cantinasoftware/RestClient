RestClient
==========

RestClient is a basic REST client for Android.

Its main features are:
  - supports asynchroneous GET / POST / PUT / DELETE HTTP verbs
  - handles multipart file uploads
  - allows anything in the request body
  - easy and straightforward syntax inspired from RestKit (iOS project)

Installation
============

  1. Clone the git repository
  2. In Eclipse, choose "Import existing projects into workspace" from the File->Import menu
  3. In the Android tab of your project properties, add a reference to the RestClient project

Usage
=====

Configuring the shared client
-----------------------------

The shared client is a singleton RestClient instance which you can configure with a Base URL.

    RestClient client = RestClient.getSharedClient().setBaseURL("http://api.example.com/");

Making requests
---------------

### Asynchroneous nature and Listeners

RestClient only performs asynchroneous requests. In order to get the results of your request,
you have to provide RestClient with an object that implements the RestClient.Request.Listener interface:

    public static interface Listener { 
      public void requestDidLoad(Request request, Response response);
      public void requestDidFail(Request request, Exception error);
    } 


### Basic Syntax

The basic syntax is suited when you only need control over the HTTP parameters. If you need more control,
use the block syntax.


    RestClient.Params params = new RestClient.Params();
    params.add("some-param", "some-value");
    params.add("some-file", new File(Environment.getExternalStorageDirectory(), "image.jpg"), "image/jpeg");
    
    try {
      client.get("/some/resource", params, this);
    } catch (MalformedURLException e) {
      Log.d("RestClient", "Malformed url", e);
    } 

### Block Syntax

The block syntax gives you complete control over the outbound HTTP request:
  - add headers
  - add parameters
  - set body
  - set listener

Here is an example:

    final RestClient.Params params = new RestClient.Params();
    params.add("some-param", "some-value");
    params.add("some-file", new File(Environment.getExternalStorageDirectory(), "image.jpg"), "image/jpeg");
    
    try { 
      client.put("/some/resource, new RestClient.Block() {
    
        @Override
        public void execute(Request request) {
          request.setParams(params);
          request.setListener(Activity.this);
        }
      });
    } catch (MalformedURLException e) { 
      Log.d("RestClient", "Malformed url", e);
    } 

### Using request results

To use request results, you implement the RestClient.Request.Listener interface like this:

    @Override
    public void requestDidLoad(Request request, Response response) { 
      try { 
        Log.d("RestClient", String.format("Request:%s did load response:%s", request.toString(), response.getBodyAsString()));
      } catch (Exception e) { 
        Log.e("RestClient", "error", e);
      } 
    } 
    
    @Override
    public void requestDidFail(Request request, Exception error) { 
      Log.d("RestClient", String.format("Request:%s did fail", request.toString()));
    } 

If you are expecting multiple requests to return a result, you can easily differenciate them by setting the request's userData property like this:

    try {
      client.put("/some/resource, new RestClient.Block() {
      
        @Override
        public void execute(Request request) {
          request.setListener(Activity.this);
          request.setUserData("request-1");
        } 
      });
    } catch (MalformedURLException e) {
      Log.d("RestClient", "Malformed url", e);
    }


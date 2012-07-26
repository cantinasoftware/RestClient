RestClient
==========

RestClient is a basic REST client for Android.

Its main features are:
  - supports the following verbs: GET / POST / PUT / DELETE
  - sends requests asynchroneously
  - handles multipart file uploads
  - lets you set anything as the response body, even a raw file
  - JSON support
  - easy and straightforward syntax inspired from [RestKit](http://restkit.org/) (iOS project)

Dependencies
============

RestClient will run on Android 2.2 and later.
RestClient uses [Apache's HttpClient](http://hc.apache.org/httpcomponents-client-ga/index.html) library, which is bundled in Android.
It also requires the following jars (included in the project):
  - httpmime-4.2.1.jar (http://hc.apache.org/httpcomponents-client-ga/httpmime/)

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

You will almost certainly want to configure timeouts like this:

    RestClient.getSharedClient().setConnectionTimeout(1000).setSocketTimeout(1000);

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
use the block syntax. The basic syntax goes like this:


    RestClient.Params params = new RestClient.Params();
    params.add("some-param", "some-value");
    
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

### Sending files

There are 2 ways to send files with RestClient:
  - as part of a Multipart request, to achieve this just add a File param to your Params object
  - by setting the body of the Request object using block syntax

With params as Multipart:

    // Use params and basic syntax to send a file via multipart
    File file = ... // your file
    RestClient.Params params = new RestClient.Params()
    params.add("my-file", file);
    RestClient.getSharedClient().put("/some/resource", params, this);

Directly in the body of the request:

    // Use block syntax and set the file as the body of the request
    RestClient.getSharedClient().put("some/resource", new RestClient.Block() {
      @Override
      public void execute(RestClient.Request request) {
        request.setListener(this);
        request.setBody(file);
      }
    });

Using request results
---------------------

### The Listener Interface

To use request results, you implement the RestClient.Request.Listener interface like this:

    @Override
    public void requestDidLoad(Request request, Response response) { 
      try { 
        // Do something with the response

      } catch (Exception e) { 
        Log.e("RestClient", "error", e);
      } 
    } 
    
    @Override
    public void requestDidFail(Request request, Exception error) { 
      Log.d("RestClient", String.format("Request:%s did fail", request.toString()));
    } 

### JSON support

Once you get hold of a Response object, it's easy to turn it into a JSONObject:

    JSONObject jsonResponse = response.getBodyAsJSONObject();
    
### UserData

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

In your listener you can then easily spot that request:

    @Override
    public void requestDidLoad(Request request, Response response) {
      if ("request-1".equals(request.getUserData())) {
        // This is my request, do something with the response
      }
    }

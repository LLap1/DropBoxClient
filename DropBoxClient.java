package com.company;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Stack;

import java.net.URLEncoder;

import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;


public class DropBoxClient{

    // Drop Box Account:
    private static DropBoxAccount account;

    // The Authorization Header:
    private static String AuthorizationH;

    // The HttpClient
    private static CloseableHttpClient client;


    public DropBoxClient(DropBoxAccount _account) {

        account = _account;
        // The Authorization Header:
        AuthorizationH = "" +
                "OAuth oauth_version=\"1.0\", " +
                "oauth_signature_method=\"PLAINTEXT\", " +
                "oauth_consumer_key=\""  + account.oauth_consumer_key + "\", " +
                "oauth_token=\"" + account.oauth_token + "\", " +
                "oauth_signature=\"" + account.oauth_signature + "\"";


        // The HttpClient
        client = HttpClients.createDefault();

    }

    //Private Methods:

    //Sets up the default headers + A costume Authorization header
    private static void  _SetHeaders(HttpRequest request, String AuthHeader) {
        request.addHeader("X-Dropbox-App-Name", "Dropbox");
        request.addHeader("X-Dropbox-App-Build-Type", "release");
        request.addHeader("X-Dropbox-App-Version", "82.2.2");
        request.addHeader("X-Dropbox-Path-Root", "null");
        request.addHeader("X-Dropbox-Locale", "en-US");
        request.addHeader("Authorization", AuthHeader);
        request.addHeader("User-Agent", "DropboxAndroidApp/82.2.2");
        request.addHeader("Accept-Encoding", "gzip, deflate");
    }

    // Sends a request, returns the response contents.
    private static HttpEntity _SendRequest(ClassicHttpRequest request){
        try {
            System.out.println(" " + request.getMethod().toUpperCase() + " -> " + request.getUri());
            CloseableHttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            return entity;

        }catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    //Fetches the entity info from the api, returns it as a json object
    private static JSONObject _FetchEntityInfo(String filepath, String params, String AuthH) {
        HttpGet get_request = new HttpGet(
                "https://api.dropbox.com/r16/metadata/dropbox" + filepath + "?" + params
        );
        _SetHeaders(get_request, AuthH);
        try{
            //setting up headers:

            //Send request + get response
            HttpEntity entity = _SendRequest(get_request);
            String res_text = EntityUtils.toString(entity);
            return new JSONObject(res_text);

        }catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    // Fetches the user info from the API, returns it as a Json object
    public static JSONObject _FetchUserInfo() {
        HttpPost post_request = new HttpPost( "https://api.dropboxapi.com/2/users/get_current_account");
        _SetHeaders(post_request, account.bearer_token);

        try {
            HttpEntity entity = _SendRequest(post_request);
            String res_text = EntityUtils.toString(entity);

            System.out.println(res_text);

            return new JSONObject(res_text);

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }


    // Methods:
    // Downloads a file from the Dropbox file system, given a full path.
    public static void DownloadFile(String file_path) {

        try {
            String data = "";

            System.out.println("[!]Downloading: " + file_path);

            // Request #1:  POST -> https://api.dropbox.com/r16/account/info
            System.out.println("[+]Request #1:");
            HttpPost post_request = new HttpPost("https://api.dropboxapi.com/r16/account/info");
            _SetHeaders(post_request, AuthorizationH);
            _SendRequest(post_request);

            // Request #2:  POST -> https://api.dropboxapi.com/2/users/get_current_account
            System.out.println("[+]Request #2:");
             post_request = new HttpPost("https://api.dropboxapi.com/2/users/get_current_account");

            _SetHeaders(post_request, account.bearer_token);
            post_request.addHeader("Content-Type", "application/json");
            data = "null";
            HttpEntity entity = new StringEntity(data);

            post_request.setEntity(entity);
            entity = _SendRequest(post_request);


            // Getting Account id:
            String res_text = EntityUtils.toString(entity);

            System.out.println(res_text);
            JSONObject json_handler = new JSONObject(res_text);
            String account_id = json_handler.get("account_id").toString();


            // Request #3:  POST -> https://api.dropboxapi.com/2/users/get_plan_info
            System.out.println("[+]Request #3:");
            post_request = new HttpPost("https://api.dropboxapi.com/2/users/get_plan_info");
            _SetHeaders(post_request, account.bearer_token);


            data = "{\"account_id\":\"" + account_id + "\"}";

            entity = new StringEntity(data);
            post_request.setEntity(entity);

            _SendRequest(post_request);

            // Request #4: POST -> https://api.dropboxapi.com/r16/metadata/dropbox/<containing folder>
            String[] tmp_full_path = file_path.split("/");
            String containing_folder = "";

            for (int i = 1; i < tmp_full_path.length - 1; i++) {
                containing_folder += "/" + tmp_full_path[i];
            }
            System.out.println("[+]Request #4 (Fetching rev code):");

            json_handler = _FetchEntityInfo(
                    containing_folder,
                    "file_limit=25000&" +
                            "list=false&locale=en_US&" +
                            "include_activity_info=false",
                    AuthorizationH
            );

            // Request #5: (File download) GET https://api-content.dropbox.com/r16/files/dropbox/<full_path>
            System.out.println("[+]Request #5 (File download):");

            //Fetching file rev number:
            json_handler = _FetchEntityInfo(
                    file_path,
                    "file_limit=25000&" +
                            "list=false&locale=en_US&" +
                            "include_activity_info=false",
                    AuthorizationH
            );

            String rev = json_handler.get("rev").toString();
            HttpGet get_request = new HttpGet(
                    "https://api-content.dropbox.com/r16/files/dropbox" + file_path + "?" +
                            "rev=" + rev + "&" + "locale=en_US"
            );

            _SetHeaders(get_request, AuthorizationH);
            entity = _SendRequest(get_request);

            System.out.println("[!]Downloaded: " + file_path + "");

            res_text = EntityUtils.toString(entity);
            System.out.println(res_text);


        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public static void ListUserFiles(){
        System.out.println("[!]Listing User File:");
        _ListUserFiles("/");
    }

    // Lists all the files on a DB account:
    private static void _ListUserFiles(String start){
        System.out.println("[?]Inside " + start + ":");
        JSONObject json_handler = _FetchEntityInfo(start, "list=true&include_activity_info=true", AuthorizationH);

        // The contents of the current folder
        JSONArray contents = json_handler.getJSONArray("contents");

        // A stack of dir full paths, that the function needs to recurse on. added in the main loop.
        Stack<String> dirs_to_recurse = new Stack<String>();

        for (int i = 0; i < contents.length(); i++) {

            JSONObject object = contents.getJSONObject(i);

            String full_path = object.get("path").toString();
            String[] tmp_name = full_path.split("/");
            String entity_name = tmp_name[tmp_name.length - 1];

            String is_dir = object.get("is_dir").toString();
            String size = object.get("size").toString();
            String mime_type = "XXXXXXXX";
            String type = "dir";

            if(is_dir == "false"){
                type = "file";
                mime_type = object.get("mime_type").toString();

            }else{
                dirs_to_recurse.push(full_path);
            }

            String mod_time = object.get("modified").toString();
            System.out.println(
                    "[!]" + entity_name + ":\n" +
                            "[+]Full Path - " + full_path + "\n" +
                            "[+]Type - " + type + "\n" +
                            "[+]Modification Time - " + mod_time + "\n" +
                            "[+]Size In Bytes - " + size + "\n" +
                            "[+]Mime Type - " + mime_type + "\n"
            );
        }
        while(!dirs_to_recurse.isEmpty()){
            _ListUserFiles(dirs_to_recurse.pop());
        }
    }

    public static void GetToken(){


        String token = account.oauth_consumer_key;

        HttpGet req = new HttpGet("https://www.dropbox.com/oauth2/authorize?client_id="+token+"&response_type=code");
        HttpEntity entity = _SendRequest(req);
        try {
            System.out.println(EntityUtils.toString(entity));
        }catch (Exception e){}

    }
    // Sends a request to the dropbox api, to retrive the user-data and displays it:
    public static void GetUserInfo(){

        System.out.println("[!]Getting User Information:");
        JSONObject json_handler = _FetchUserInfo();
        String email = json_handler.get("email").toString();
        String username = json_handler.get("user_name").toString();
        String account_id = json_handler.get("account_id").toString();

        System.out.println(
                "[!]User information:" + "\n" +
                        "[+]Account ID: " + account_id + "\n" +
                        "[+]Email: " + email + "\n" +
                        "[+]Username: " + username + "\n"
        );
    }
}

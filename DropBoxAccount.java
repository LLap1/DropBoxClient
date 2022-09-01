package com.company;

public class DropBoxAccount {
    // Dropbox account, identified by the oauth tokens and the account id

    // User oauth data:
    public final   String oauth_consumer_key;
    public final   String oauth_token;
    public final   String oauth_signature;
    public final   String bearer_token;


    public DropBoxAccount(String _oauth_consumer_key, String _oauth_token, String _oauth_signature, String _bearer_token){
        this.oauth_consumer_key = _oauth_consumer_key;
        this.oauth_token = _oauth_token;
        this.oauth_signature = _oauth_signature;
        this.bearer_token = _bearer_token;

    }

}

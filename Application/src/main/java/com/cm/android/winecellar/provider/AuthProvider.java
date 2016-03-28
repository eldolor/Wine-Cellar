package com.cm.android.winecellar.provider;

import android.content.Context;

import com.cm.android.winecellar.util.Utils;

/**
 * Created by anshugaind on 3/18/16.
 */
public class AuthProvider {

    private boolean mIsLoggedIn;
    private static AuthProvider mAuthProvider;

    private AuthProvider(){

    }

    public static AuthProvider getAuthProvider(){
        synchronized (AuthProvider.class){
            if(AuthProvider.mAuthProvider == null){
               mAuthProvider = new AuthProvider();
            }
        }
        return mAuthProvider;
    }

    public void createPin(Context context, String pin){
        context.getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().putString("PIN", pin).commit();
    }

    public boolean authenticate(Context context, String pin) {
        String storedPin = context.getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).getString("PIN", null);
        //first time
        if(storedPin == null){
            context.getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().putString("PIN", pin).commit();
            mIsLoggedIn = true;
            return true;
        }
        //compare pins
        if (storedPin != null && pin != null && (storedPin.equals(pin))){
            mIsLoggedIn = true;
            return true;
        }
        mIsLoggedIn = false;
        return false;

    }

    public boolean isIsLoggedIn(){
        return mIsLoggedIn;
    }

    public void logout(){
        mIsLoggedIn = false;
    }

}

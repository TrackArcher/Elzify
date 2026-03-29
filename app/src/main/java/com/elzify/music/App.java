package com.elzify.music;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.elzify.music.github.Github;
import com.elzify.music.helper.ThemeHelper;
import com.elzify.music.subsonic.Subsonic;
import com.elzify.music.subsonic.SubsonicPreferences;
import com.elzify.music.util.ClientCertManager;
import com.elzify.music.util.Preferences;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class App extends Application {
    private static App instance;
    private static Context context;
    private static Subsonic subsonic;
    private static Github github;
    private static SharedPreferences preferences;
    private static SharedPreferences encryptedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "encrypted_preferences",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("App", "Could not initialize EncryptedSharedPreferences", e);
            encryptedPreferences = preferences;
        }

        String themePref = preferences.getString(Preferences.THEME, ThemeHelper.DEFAULT_MODE);
        ThemeHelper.applyTheme(themePref);

        ClientCertManager.setupSslSocketFactory(context);
    }

    public static App getInstance() {
        return instance;
    }

    public static Context getContext() {
        return context;
    }

    public static Subsonic getSubsonicClientInstance(boolean override) {
        if (subsonic == null || override) {
            subsonic = getSubsonicClient();
        }
        return subsonic;
    }
    
    public static Subsonic getSubsonicPublicClientInstance(boolean override) {

        /*
        If I do the shortcut that the IDE suggests:
            SubsonicPreferences preferences = getSubsonicPreferences1();
        During the chain of calls it will run the following:
            String server = Preferences.getInUseServerAddress();
        Which could return Local URL, causing issues like generating public shares with Local URL

        To prevent this I just replicated the entire chain of functions here,
        if you need a call to Subsonic using the Server (Public) URL use this function.
         */

        String server = Preferences.getServer();
        String username = Preferences.getUser();
        String password = Preferences.getPassword();
        String token = Preferences.getToken();
        String salt = Preferences.getSalt();
        boolean isLowSecurity = Preferences.isLowScurity();

        SubsonicPreferences preferences = new SubsonicPreferences();
        preferences.setServerUrl(server);
        preferences.setUsername(username);
        preferences.setAuthentication(password, token, salt, isLowSecurity);

        if (subsonic == null || override) {
            
            if (preferences.getAuthentication() != null) {
                if (preferences.getAuthentication().getPassword() != null)
                    Preferences.setPassword(preferences.getAuthentication().getPassword());
                if (preferences.getAuthentication().getToken() != null)
                    Preferences.setToken(preferences.getAuthentication().getToken());
                if (preferences.getAuthentication().getSalt() != null)
                    Preferences.setSalt(preferences.getAuthentication().getSalt());
            }

            
        }
        
        return new Subsonic(preferences);
    }

    public static Github getGithubClientInstance() {
        if (github == null) {
            github = new Github();
        }
        return github;
    }

    public SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return preferences;
    }

    public SharedPreferences getEncryptedPreferences() {
        if (encryptedPreferences == null) {
            return getPreferences();
        }
        return encryptedPreferences;
    }

    public static void refreshSubsonicClient() {
        subsonic = getSubsonicClient();
    }

    private static Subsonic getSubsonicClient() {
        SubsonicPreferences preferences = getSubsonicPreferences();

        if (preferences.getAuthentication() != null) {
            if (preferences.getAuthentication().getPassword() != null)
                Preferences.setPassword(preferences.getAuthentication().getPassword());
            if (preferences.getAuthentication().getToken() != null)
                Preferences.setToken(preferences.getAuthentication().getToken());
            if (preferences.getAuthentication().getSalt() != null)
                Preferences.setSalt(preferences.getAuthentication().getSalt());
        }

        return new Subsonic(preferences);
    }

    @NonNull
    private static SubsonicPreferences getSubsonicPreferences() {
        String server = Preferences.getInUseServerAddress();
        String username = Preferences.getUser();
        String password = Preferences.getPassword();
        String token = Preferences.getToken();
        String salt = Preferences.getSalt();
        boolean isLowSecurity = Preferences.isLowScurity();

        SubsonicPreferences preferences = new SubsonicPreferences();
        preferences.setServerUrl(server);
        preferences.setUsername(username);
        preferences.setAuthentication(password, token, salt, isLowSecurity);

        return preferences;
    }
}

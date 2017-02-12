package net.gotev.dbtest;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

import net.gotev.dbframework.DatabaseManager;
import net.gotev.dbtest.migrations.M1_CreateTestTable;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseManager.Logger logger = null;

        // this way you will have logging only in debug builds
        if (BuildConfig.DEBUG) {
            logger = new DatabaseManager.Logger() {
                @Override
                public void onQuery(String query) {
                    Log.i("DB query", query);
                }

                @Override
                public void onMessage(String message) {
                    Log.i("DB message", message);
                }
            };

            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build());
        }

        DatabaseManager.init(this, "yourdatabase.db", logger, new M1_CreateTestTable());
    }

}

# Android DB Framework
Mini reactive framework to work with SQLite databases on Android. This project is powered by [SQLBrite](https://github.com/square/sqlbrite) and [SQLDelight](https://github.com/square/sqldelight) libraries, plus convenient initialization, transaction creation, common database methods and migration support. [Google AutoValue](https://github.com/google/auto/tree/master/value) is used to eliminate the need to write boilerplate for the database models. [AutoValue Parcel](https://github.com/rharter/auto-value-parcel) extension is used to auto generate the `Parcelable` implementation for DB models.

## Setup
In global gradle config file:
```groovy
dependencies {
    classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    classpath 'com.squareup.sqldelight:gradle-plugin:0.5.1'
}
```
In app's module gradle config file:
```groovy
// place after apply plugin: 'com.android.application'
apply plugin: 'android-apt'
apply plugin: 'com.squareup.sqldelight'

// change the values according to the versions you want to use
def autoValueVersion = "1.2"
def autoValueParcelVersion = "0.2.5"

dependencies {
    compile "net.gotev:dbframework:1.0"
    provided "com.google.auto.value:auto-value:${autoValueVersion}"
    apt "com.google.auto.value:auto-value:${autoValueVersion}"
    apt "com.ryanharter.auto.value:auto-value-parcel:${autoValueParcelVersion}"
}
```

Then, create a directory named `sqldelight` inside your app's `src/main` directory. If your database models package is for example `com.yourcompany.db`, you have to re-create that structure inside `sqldelight` directory like this: `src/main/sqldelight/com/yourcompany/db`. After you've done this, in Android Studio switch to `Project Files` view to be able to see also `sqldelight` directory structure.

In Android Studio open `Preferences` > `Plugins` > `Browse repositories` and search for `SQLDelight`. Install the plugin and restart Android Studio. After this the initial setup is over!

## How to create a table
1. Create a new `.sq` file inside your `sqldelight` directory, for example `src/main/sqldelight/com/yourcompany/db/Test.sq`:
```sql
CREATE TABLE test (
    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT
);
```
2. Rebuild your project. A new interface called `TestModel` will be automatically generated
3. In your Java package, implement the model:
```java
package com.yourcompany.db;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Test implements TestModel, Parcelable {

    private static final Factory<Test> FACTORY = new Factory<>(new TestModel.Creator<Test>() {
        @Override
        public Test create(long _id, @Nullable String name) {
            return new AutoValue_Test(_id, name);
        }
    });

    public static Marshal getMarshal() {
        return new Marshal(null);
    }

}
```
Bear in mind that `AutoValue_Test` might be red when you write it. You need to recompile for the AutoValue class to be generated.

For more information, check [SQLDelight](https://github.com/square/sqldelight) docs.

## Database initialization and migrations
Create an [Android Application](http://developer.android.com/reference/android/app/Application.html) subclass, register it in your manifest and initialize the database framework like this:
```java
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseManager.Logger logger;

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
        } else {
            logger = null;
        }

        // initialize database. It will take care automatically of
        // executing the needed migrations to update the app's DB schema
        DatabaseManager.init(this, "yourdatabase.db", logger, new M1_CreateTestTable());

        // to add further migrations, simply add them at the end like this:
        /*
        DatabaseManager.init(this, "yourdatabase.db", logger,
            new M1_CreateTestTable(),
            new M2_CreateOrdersTable()
        );
        */
    }

}
```

Bear in mind that whenever you add a new table or modify the schema, you have to add a database migration. If you haven't published the app version yet, you can have a single database migration during the development. Just remind yourself to drop the app and reinstall it after schema changes to prevent strange errors.

A migration looks like this:
```java
public class M1_CreateTestTable implements DatabaseMigration {
    @Override
    public void up(SQLiteDatabase db) {
        db.execSQL(TestModel.CREATE_TABLE);
    }

    @Override
    public void down(SQLiteDatabase db) {
        DatabaseManager.getDropTableSql(TestModel.TABLE_NAME);
    }
}
```

## Publishing
To publish on bintray, execute: `./gradlew clean assembleRelease bintrayUpload`

## License

    Copyright (C) 2017 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

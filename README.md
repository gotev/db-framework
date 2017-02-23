# Android DB Framework
Mini reactive framework to work with SQLite databases on Android. It has easy database initialization, transaction creation, common database methods and Db schema migration support.

This project is powered by:
* [SQLDelight](https://github.com/square/sqldelight) to create Java DB models and queries out of plain and simple SQL
* [SQLBrite](https://github.com/square/sqlbrite) to perform queries on the database with Rx
* [Google AutoValue](https://github.com/google/auto/tree/master/value) to automatically generate Java database model implementations
* [AutoValue Parcel](https://github.com/rharter/auto-value-parcel) to automatically generate the `Parcelable` implementation for DB models.
* [RxJava](https://github.com/ReactiveX/RxJava) and [RxAndroid](https://github.com/ReactiveX/RxAndroid)

## Index
* [Setup](#setup)
* [Create a table and its Java model](#create)
* [Database initialization](#initialization)
* [Migrations](#migrations)
* [Query](#query)
* [Insert, update and delete](#insert-update-delete)
* [Debug](#debug)
* [SQLite cheatsheet](#sqlite)
* [Example app](#example)
* [License](#license)

## <a name="setup"></a>Setup
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
    compile "net.gotev:dbframework:1.1"
    provided "com.google.auto.value:auto-value:${autoValueVersion}"
    apt "com.google.auto.value:auto-value:${autoValueVersion}"
    apt "com.ryanharter.auto.value:auto-value-parcel:${autoValueParcelVersion}"
}
```

Then, create a directory named `sqldelight` inside your app's `src/main` directory. If your database models package is for example `com.yourcompany.db`, you have to re-create that structure inside `sqldelight` directory like this: `src/main/sqldelight/com/yourcompany/db`. After you've done this, in Android Studio switch to `Project Files` view to be able to see also `sqldelight` directory structure.

In Android Studio open `Preferences` > `Plugins` > `Browse repositories` and search for `SQLDelight`. Install the plugin and restart Android Studio. After this the initial setup is over!

## <a name="create"></a>Create a table and its Java model
1. Create a new `.sq` file inside your `sqldelight` directory, for example `src/main/sqldelight/com/yourcompany/db/Test.sq`:

  ```sql
CREATE TABLE test (
    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    surname TEXT,
    age INTEGER NOT NULL
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
        public Test create(long _id, @Nullable String name, @Nullable String surname, long age) {
            return new AutoValue_Test(_id, name, surname, age);
        }
    });

    public static Marshal getMarshal() {
        return new Marshal(null);
    }

}
  ```
Bear in mind that `AutoValue_Test` might be red when you write it. You need to recompile for the AutoValue class to be generated.

If you also use the [Retrolambda](https://github.com/evant/gradle-retrolambda) plugin, you can reduce boilerplate even further:
```java
@AutoValue
public abstract class Test implements TestModel, Parcelable {

    private static final Factory<Test> FACTORY = new Factory<>(AutoValue_Test::new);

    public static Marshal getMarshal() {
        return new Marshal(null);
    }

}
```

For exhaustive information, check [SQLDelight](https://github.com/square/sqldelight) docs.

## <a name="initialization"></a>Database initialization
Create an [Android Application](http://developer.android.com/reference/android/app/Application.html) subclass, register it in your manifest and initialize the database framework like this:
```java
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

## <a name="migrations"></a>Migrations
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
[Here](https://github.com/square/sqldelight/issues/89#issuecomment-282075609) you can see a complete example of how you can migrate an existing table schema to a new one by adding or removing columns.

## <a name="query"></a>Query
To query a table, define the SQL SELECT statement in the table's `.sq` file. In case of a JOIN, put the SELECT statement in one of the tables involed in the join operation.

For exhaustive information, check [SQLDelight](https://github.com/square/sqldelight) and [SQLBrite](https://github.com/square/sqlbrite) docs.

For example, let's get all the records by age. Add the following to `Test.sq`, after the `CREATE TABLE` statement:
```sql
get_by_age:
SELECT *
FROM test
WHERE age = ?;
```
Then, rebuild the project. Open the model implementation (in this case `Test.java`) and add:
```java
@UiThread
public static Observable<List<Test>> getByAge(final long age) {

    return DatabaseManager.getObservableList(
                FACTORY.get_by_age(age),
                FACTORY.get_by_ageMapper())
            .observeOn(AndroidSchedulers.mainThread());
}
```

At this point, you can easily perform the query in an activity:
```java
public class MainActivity extends AppCompatActivity {
    private Subscription subscription;

    @Override
    protected void onResume() {
        super.onResume();

        subscription = Test.getByAge(27)
                .subscribe(new Subscriber<List<Test>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        // handle database read error
                    }

                    @Override
                    public void onNext(List<Test> test) {
                        // do something with the object
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
    }
}
```

If you use the excellent [RxLifecycle](https://github.com/trello/RxLifecycle) library, you don't even have to care of unsubscribing the observable:
```java
public class MainActivity extends RxAppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();

        Test.getByAge(27)
            .compose(RxLifecycleAndroid.bindActivity(lifecycle()))
            .subscribe(new Subscriber<List<Test>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        // handle database read error
                    }

                    @Override
                    public void onNext(List<Test> test) {
                        // do something with the object
                    }
                });
    }
}
```

## <a name="insert-update-delete"></a>Insert, update and delete
Insert, update and delete operations have to be performed with transactions, to be sure the DB is consistent. Those operations have to be performed in the background. I advise you to implement an `IntentService` for doing so, or to use one of the multitude of background job scheduling libraries. Here there's an example with a very basic `IntentService`:

```java
public class PopulateTestTableService extends IntentService {

    public PopulateTestTableService() {
        super("PopulateTestTableService");
    }

    public static void start(Context context) {
        context.startService(new Intent(context, PopulateTestTableService.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        List<TestModel.Marshal> testUsers = new ArrayList<>();

        testUsers.add(Test.getMarshal().name("John").surname("Smith").age(27));
        testUsers.add(Test.getMarshal().name("Mario").surname("Rossi").age(23));
        testUsers.add(Test.getMarshal().name("Stephen").surname("White").age(27));
        testUsers.add(Test.getMarshal().name("Josh").surname("Blank").age(18));
        testUsers.add(Test.getMarshal().name("Alfred").surname("Batman").age(60));

        try {
            TransactionBuilder transactionBuilder = new TransactionBuilder("populate test table");

            for (TestModel.Marshal record : testUsers) {
                transactionBuilder.add(save(record));
            }

            transactionBuilder.execute();

        } catch (Throwable exception) {
            Log.e("Populate", "Error while populating test table", exception);
        }

    }

    private TransactionStatement save(TestModel.Marshal record) {
        return DatabaseManager.save(TestModel.TABLE_NAME, TestModel._ID,
                record.asContentValues(), true);
    }
}
```
To work, the `IntentService` has to be registered in the manifest:
```xml
<service
    android:name=".PopulateTestTableService"
    android:exported="false" />
```

## <a name="debug"></a>Debug
### Facebook Stetho
If you integrate [Facebook Stetho](http://facebook.github.io/stetho/) in your debug builds, you can easily browse the SQLite database and perform queries on it with your Chrome Developer Tools:

1. Open in Chrome: [chrome://inspect](chrome://inspect)
2. Click on `inspect` under your device
3. Click on `Resources` tab, then expand `Web SQL` and select `yourdatabase.db`

You may also find useful [stetho no-op for production](https://github.com/iGenius-Srl/stetho-no-op) library, to disable stetho in production.

### Connecting with ADB
>Note: This method requires ROOT on the device you're adbing to

1. List devices connected to adb: `adb devices`

  ```shell
List of devices attached
emulator-5554   device
  ```

2. Connect to the device (e.g. `emulator-5554`): `adb -s emulator-5554 shell`
3. Become superuser: `su`
4. Open your app's database: `sqlite3 data/data/com.youcompany.app/databases/yourdatabase.db`
5. You can perform all the queries you want

## <a name="sqlite"></a>SQLite cheatsheet
#### List the tables in your database:
```
.tables
```

#### List how the table looks
```
.schema tablename
```

#### Print the entire table
```sql
SELECT * FROM tablename;
```

#### List all of the available SQLite prompt commands
```
.help
```

## <a name="example"></a>Example app
In the `example` directory you can find a complete example app which uses `DB Framework` and applies all the concepts documented in this README, together with `Retrolambda`, `ButterKnife`, `RxLifecycle` and `Stetho`.

## <a name="license"></a>License

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

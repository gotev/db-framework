# DB Framework
Mini reactive framework to work with SQLite databases on Android. This project is powered by [SQLBrite](https://github.com/square/sqlbrite) and [SQLDelight](https://github.com/square/sqldelight) libraries, plus convenient initialization, transaction creation, common database methods and migration support. [Google AutoValue](https://github.com/google/auto/tree/master/value) is used to eliminate the need to write boilerplate for the database models.

# Setup
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

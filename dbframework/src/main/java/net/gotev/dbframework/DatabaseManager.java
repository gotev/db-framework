package net.gotev.dbframework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.QueryObservable;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

/**
 * This is to have thread safe database access, a convenient initialization and common methods.
 *
 * Ideas from:
 * - http://www.dmytrodanylyk.com/concurrent-database-access/
 * - http://alexsimo.com/delightful-persistence-android/
 *
 * @author gotev (alex@gotev.net)
 */

public class DatabaseManager {

    public interface Logger {
        void onQuery(String query);
        void onMessage(String message);
    }

    private static AtomicInteger openCount = new AtomicInteger();
    private static DatabaseManager instance;
    private static DatabaseHelper openHelper;
    private static BriteDatabase database;
    private static SqlBrite sqlBrite;
    private static Logger log;

    private DatabaseManager() {
        sqlBrite = new SqlBrite.Builder()
                .logger(new SqlBrite.Logger() {
                    @Override
                    public void log(String message) {
                        logMessage("SQLBrite - " + message);
                    }
                }).build();
    }

    public static synchronized void deinit() {
        if (instance == null)
            return;

        instance.closeDatabase();
        instance = null;
        openHelper = null;
        database = null;
        sqlBrite = null;
        log = null;
        openCount = new AtomicInteger();
    }

    public static synchronized DatabaseManager getInstance() {
        if (null == instance) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName()
                    + " is not initialized, call init method first.");
        }
        return instance;
    }

    public static synchronized void init(final Context context, final String dbName,
                                         Logger logger, DatabaseMigration... migrations) {
        if (null == instance) {
            instance = new DatabaseManager();
            log = logger;
        }
        openHelper = DatabaseHelper.init(context, dbName, logger, migrations);
    }

    static void logMessage(String message) {
        if (log != null)
            log.onMessage(message);
    }

    public synchronized BriteDatabase openDatabase() {
        if (openCount.incrementAndGet() == 1) {
            database = sqlBrite.wrapDatabaseHelper(openHelper, Schedulers.io());
            database.setLoggingEnabled(log != null);
        }
        return database;
    }

    public synchronized void closeDatabase() {
        if (openCount.decrementAndGet() == 0) {
            database.close();
        }
    }

    public QueryObservable getObservableQuery(SqlDelightStatement stmt) {
        return openDatabase().createQuery(stmt.tables, stmt.statement, stmt.args);
    }

    public <T> T executeQuery(SqlDelightStatement stmt, RowMapper<T> mapper, T defaultValue) {
        return executeQuery(stmt.statement, stmt.args, mapper, defaultValue);
    }

    public <T> T executeQuery(String statement, String[]args, RowMapper<T> mapper, T defaultValue) {

        Cursor cursor = openDatabase().query(statement, args);

        if (cursor != null) {
            T value = null;

            try {
                if (cursor.moveToNext()) {
                    value = mapper.map(cursor);
                }
            } finally {
                cursor.close();
            }

            return value == null ? defaultValue : value;
        }

        return defaultValue;

    }

    public <T> List<T> executeListQuery(SqlDelightStatement stmt, RowMapper<T> mapper) {

        Cursor cursor = openDatabase().query(stmt.statement, stmt.args);

        if (cursor != null && cursor.getCount() > 0) {
            List<T> list = new ArrayList<>(cursor.getCount());

            try {
                while (cursor.moveToNext()) {
                    list.add(mapper.map(cursor));
                }
            } finally {
                cursor.close();
            }

            return list;
        }

        return new ArrayList<>(1);
    }

    public long countRows(BriteDatabase db, String tableName) {
        String query = "SELECT COUNT(*) FROM " + tableName;

        Cursor cursor = db.query(query);

        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return 0;
    }

    public static TransactionStatement save(final String tableName, final String primaryKeyName,
                                            final ContentValues recordToSave,
                                            final boolean primaryKeyAutoGenerated) {

        return new TransactionStatement() {
            @Override
            public void onStatement(BriteDatabase db) throws Throwable {
                if (tableName == null || tableName.isEmpty())
                    throw new IllegalArgumentException("table name not defined in syncTableWithList");

                if (primaryKeyName == null || primaryKeyName.isEmpty())
                    throw new IllegalArgumentException("primary key name not defined in syncTableWithList");

                if (recordToSave == null || recordToSave.size() == 0)
                    throw new IllegalArgumentException("recordToSave must not be null or empty!");

                // make a copy of the object to save, to not change the original one
                ContentValues record = new ContentValues(recordToSave);

                if (!record.containsKey(primaryKeyName)) {
                    if (primaryKeyAutoGenerated) {
                        db.insert(tableName, record, CONFLICT_REPLACE);
                        logMessage("Successfully added record in " + tableName);
                    } else {
                        throw new IllegalArgumentException("This table does not have an autoGenerated primary, but no primary key provided in ContentValues record!");
                    }

                } else {
                    long id = record.getAsLong(primaryKeyName);

                    record.remove(primaryKeyName);
                    int modifiedRows = db.update(tableName, record, primaryKeyName + " = ? ", Long.toString(id));

                    if (modifiedRows > 0) {
                        logMessage(String.format(Locale.getDefault(),
                                "Successfully updated record with ID %d in %s", id, tableName));

                    } else {
                        if (!primaryKeyAutoGenerated)
                            record.put(primaryKeyName, id);
                        db.insert(tableName, record);
                        logMessage("Successfully added record in " + tableName);
                    }
                }
            }
        };
    }

    public TransactionStatement syncTableWithList(final String tableName,
                                                  final String primaryKeyName,
                                                  final List<ContentValues> newRecords,
                                                  final SqlDelightStatement getAllTheExistingRecordPrimaryKeysQuery,
                                                  final boolean primaryKeyAutoGenerated) {

        return new TransactionStatement() {
            @Override
            public void onStatement(BriteDatabase db) throws Throwable {
                if (tableName == null || tableName.isEmpty())
                    throw new IllegalArgumentException("table name not defined in syncTableWithList");

                if (primaryKeyName == null || primaryKeyName.isEmpty())
                    throw new IllegalArgumentException("primary key name not defined in syncTableWithList");

                if (newRecords == null)
                    throw new IllegalArgumentException("new records list must not be null");

                if (getAllTheExistingRecordPrimaryKeysQuery == null)
                    throw new IllegalArgumentException("getAllTheExistingRecordPrimaryKeysQuery must not be null");

                long existingRecords = countRows(db, tableName);

                if (existingRecords == 0) {
                    logMessage(tableName + " is empty");

                    for (ContentValues newRecord : newRecords) {
                        logMessage(String.format(Locale.getDefault(),
                                "Inserting record with ID %d in %s",
                                newRecord.getAsLong(primaryKeyName), tableName));

                        if (primaryKeyAutoGenerated)
                            newRecord.remove(primaryKeyName);
                        db.insert(tableName, newRecord);
                    }

                } else {
                    List<Long> existingIDs = getExistingRecordIDs(getAllTheExistingRecordPrimaryKeysQuery);

                    for (Long idToDelete : getIDsToDelete(primaryKeyName, existingIDs, newRecords)) {
                        logMessage(String.format(Locale.getDefault(),
                                "Deleting record with ID %d from %s", idToDelete, tableName));
                        db.delete(tableName, primaryKeyName + " = ?", Long.toString(idToDelete));
                    }

                    // insert or update
                    for (ContentValues newRecord : newRecords) {
                        save(tableName, primaryKeyName, newRecord, primaryKeyAutoGenerated).onStatement(db);
                    }

                }
            }
        };
    }

    /**
     * The VACUUM command rebuilds the database file, repacking it into a minimal amount of
     * disk space. A VACUUM will fail if there is an open transaction, or if there are one or more
     * active SQL statements when it is run.
     *
     * https://sqlite.org/lang_vacuum.html
     */
    public void vacuum() {
        logMessage("Compacting database");
        openDatabase().getWritableDatabase().execSQL("VACUUM");
    }

    private List<Long> getExistingRecordIDs(SqlDelightStatement getAllTheExistingRecordPrimaryKeysQuery) {
        try {
            return executeListQuery(
                    getAllTheExistingRecordPrimaryKeysQuery, new RowMapper<Long>() {
                        @NonNull
                        @Override
                        public Long map(@NonNull Cursor cursor) {
                            return cursor.getLong(0);
                        }
                    });
        } catch (AssertionError error) {
            return new ArrayList<>(1);
        }
    }

    private static boolean isIdContained(String primaryKeyName, long recordId, List<ContentValues> values) {
        for (ContentValues value : values) {
            if (value.containsKey(primaryKeyName) && value.getAsLong(primaryKeyName) == recordId)
                return true;
        }

        return false;
    }

    private static List<Long> getIDsToDelete(String primaryKeyName, List<Long> existing, List<ContentValues> newRecords) {
        List<Long> toDelete = new ArrayList<>(existing.size());

        for (Long record : existing) {
            if (!isIdContained(primaryKeyName, record, newRecords))
                toDelete.add(record);
        }

        return toDelete;
    }

    @UiThread
    public static <T> Observable<List<T>> getObservableList(SqlDelightStatement statement,
                                                            final RowMapper<T> mapper) {
        return getInstance().openDatabase().createQuery(statement.tables, statement.statement, statement.args)
                .mapToList(new Func1<Cursor, T>() {
                    @Override
                    public T call(Cursor cursor) {
                        return mapper.map(cursor);
                    }
                });
    }

    @UiThread
    public static <T> Observable<T> getObservable(SqlDelightStatement statement,
                                                  final RowMapper<T> mapper) {
        return getInstance().openDatabase().createQuery(statement.tables, statement.statement, statement.args)
                .mapToOne(new Func1<Cursor, T>() {
                    @Override
                    public T call(Cursor cursor) {
                        return mapper.map(cursor);
                    }
                });
    }

    @UiThread
    public static <T> Observable<T> getObservableWithDefault(SqlDelightStatement statement,
                                                  final RowMapper<T> mapper, T defaultValue) {
        return getInstance().openDatabase().createQuery(statement.tables, statement.statement, statement.args)
                .mapToOneOrDefault(new Func1<Cursor, T>() {
                    @Override
                    public T call(Cursor cursor) {
                        return mapper.map(cursor);
                    }
                }, defaultValue);
    }

    public static String getDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS `" + tableName + "`;";
    }

    public static String getTruncateTableSql(String tableName) {
        return "DELETE FROM `" + tableName + "`; VACUUM;";
    }

}

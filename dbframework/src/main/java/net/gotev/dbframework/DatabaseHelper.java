package net.gotev.dbframework;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;

import static net.gotev.dbframework.DatabaseManager.logMessage;


/**
 * Helper to create, update and downgrade SQLite database schema.
 *
 * @author gotev (alex@gotev.net)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;
    private static DatabaseMigration[] dbMigrations;

    static DatabaseHelper init(final Context context, final String dbName,
                               DatabaseManager.Logger logger, DatabaseMigration... migrations) {
        if (migrations == null || migrations.length == 0)
            throw new IllegalArgumentException("You must have at least one migration!");

        if (null == instance) {
            instance = new DatabaseHelper(context, dbName, logger, migrations);
        }
        return instance;
    }

    private DatabaseHelper(Context context, final String dbName,
                           final DatabaseManager.Logger logger, DatabaseMigration... migrations) {
        super(context, dbName, logger == null ? null : new SQLiteDatabase.CursorFactory() {
            @Override
            public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
                logger.onQuery(query.toString().replace("SQLiteQuery: ", "").replaceAll("\\n", " "));
                return new SQLiteCursor(masterQuery, editTable, query);
            }
        }, migrations.length);
        dbMigrations = migrations;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        logMessage("Creating database");

        for (DatabaseMigration migration : dbMigrations) {
            migration.up(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logMessage("Upgrading database schema from version " + oldVersion + " to " + newVersion);

        for (int i = oldVersion; i < newVersion; i++) {
            logMessage("Upgrading database from version " + i + " to " + (i + 1));

            dbMigrations[i].up(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logMessage("Downgrading database schema from version " + oldVersion + " to " + newVersion);

        for (int i = oldVersion; i > newVersion; i++) {
            logMessage("Downgrading database schema from version " + i + " to " + (i - 1));

            dbMigrations[i - 1].down(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

}

package net.gotev.dbframework;

import android.database.sqlite.SQLiteDatabase;

/**
 * Interface for a database migration
 *
 * @author gotev (alex@gotev.net)
 */
public interface DatabaseMigration {
    /**
     * SQL to execute to apply the migration.
     * @param db database instance
     */
    void up(SQLiteDatabase db);

    /**
     * SQL to execute to apply to revert the migration.
     * @param db database instance
     */
    void down(SQLiteDatabase db);
}

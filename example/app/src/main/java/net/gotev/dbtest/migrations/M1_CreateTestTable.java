package net.gotev.dbtest.migrations;

import android.database.sqlite.SQLiteDatabase;

import net.gotev.dbframework.DatabaseManager;
import net.gotev.dbframework.DatabaseMigration;
import net.gotev.dbtest.models.TestModel;

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

package net.gotev.dbtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.gotev.dbframework.DatabaseManager;
import net.gotev.dbframework.TransactionBuilder;
import net.gotev.dbframework.TransactionStatement;
import net.gotev.dbtest.models.Test;
import net.gotev.dbtest.models.TestModel;

import java.util.ArrayList;
import java.util.List;

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

package net.gotev.dbframework;

import com.squareup.sqlbrite.BriteDatabase;

import java.util.ArrayList;
import java.util.List;

import static net.gotev.dbframework.DatabaseManager.logMessage;

/**
 * Helper class to build complex transactions and execute them.
 *
 * @author gotev (alex@gotev.net)
 */
public class TransactionBuilder {

    private List<TransactionStatement> mStatements;

    private String mTransactionName;

    private TransactionBuilder() { }

    public TransactionBuilder(String transactionName) {
        if (transactionName == null || transactionName.isEmpty())
            throw new IllegalArgumentException("You must give a name to the transaction!");

        mStatements = new ArrayList<>();
        mTransactionName = transactionName;
    }

    public TransactionBuilder add(TransactionStatement statement) {
        mStatements.add(statement);
        return this;
    }

    public void execute() throws Throwable {
        logMessage("Executing transaction: " + mTransactionName);

        BriteDatabase db = DatabaseManager.getInstance().openDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();

        try {
            for (TransactionStatement stmt : mStatements) {
                stmt.onStatement(db);
            }

            transaction.markSuccessful();
            logMessage("Successful transaction: " + mTransactionName);

        } finally {
            transaction.end();
        }
    }

}

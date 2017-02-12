package net.gotev.dbframework;

import com.squareup.sqlbrite.BriteDatabase;

/**
 * Repesents an operation which can be added to a transaction.
 *
 * @author gotev (alex@gotev.net)
 */

public interface TransactionStatement {
    void onStatement(BriteDatabase db) throws Throwable;
}

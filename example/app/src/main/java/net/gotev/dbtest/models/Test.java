package net.gotev.dbtest.models;

import android.os.Parcelable;
import android.support.annotation.UiThread;

import com.google.auto.value.AutoValue;

import net.gotev.dbframework.DatabaseManager;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@AutoValue
public abstract class Test implements TestModel, Parcelable {

    private static final Factory<Test> FACTORY = new Factory<>(AutoValue_Test::new);

    public static Marshal getMarshal() {
        return new Marshal(null);
    }

    @UiThread
    public static Observable<List<Test>> getByAge(final long age) {

        return DatabaseManager.getObservableList(FACTORY.get_by_age(age), FACTORY.get_by_ageMapper())
                .observeOn(AndroidSchedulers.mainThread());
    }

}

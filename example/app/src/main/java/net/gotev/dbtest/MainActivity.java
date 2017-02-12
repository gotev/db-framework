package net.gotev.dbtest;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.trello.rxlifecycle.android.RxLifecycleAndroid;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import net.gotev.dbtest.models.Test;
import net.gotev.dbtest.models.TestModel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends RxAppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.textView)
    TextView textView;

    @BindView(R.id.populate)
    Button populate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
    }

    @OnClick(R.id.populate)
    public void onTextViewClick() {
        PopulateTestTableService.start(MainActivity.this);
        populate.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Test.getByAge(27)
                .compose(RxLifecycleAndroid.bindActivity(lifecycle()))
                .subscribe(list -> {
                    textView.setText("");
                    for (TestModel person : list) {
                        textView.append("\n" + person.name() + " " + person.surname());
                    }
                }, error -> Log.e("DB error", "Error while getting records", error));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

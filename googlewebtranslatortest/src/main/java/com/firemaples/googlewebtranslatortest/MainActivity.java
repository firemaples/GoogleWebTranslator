package com.firemaples.googlewebtranslatortest;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;

import com.firemaples.googlewebtranslator.GoogleTranslator;
import com.firemaples.googlewebtranslator.GoogleWebTranslator;
import com.firemaples.googlewebtranslator.Language;
import com.firemaples.googlewebtranslator.TranslatedResult;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

//    private GoogleWebTranslator translator;

    private GoogleTranslator googleTranslator;

    private GoogleWebTranslator.OnTranslationCallback onTranslationCallback = new GoogleWebTranslator.OnTranslationCallback() {

        @Override
        public void onInitialized(GoogleWebTranslator translator) {
            Log.i(TAG, "Translator initialized");
        }

        @Override
        public void onInitializationFailed(GoogleWebTranslator translator, int errorCode, String desc) {
            Log.e(TAG, "Translator initialization failed");
        }

        @Override
        public void onTranslationSuccess(GoogleWebTranslator translator, TranslatedResult result) {
            Log.i(TAG, "Translation success: " + result.text);
        }

        @Override
        public void onTranslationFailed(GoogleWebTranslator translator, Throwable throwable) {
            Log.e(TAG, "Translation failed: " + throwable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        translator = GoogleWebTranslator.init(this);
        googleTranslator = new GoogleTranslator(this);

//        translator.addOnTranslationCallback(onTranslationCallback);

        setViews();
    }

    private void setViews() {
        ViewGroup rootView = findViewById(R.id.view_root);
//        rootView.addView(translator.getNonParentWebView(),
//                new ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT));

        googleTranslator.setup(rootView);

//        translator.setTargetLanguage(Language.Chinese_Traditional);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        translator.removeOnTranslationCallback(onTranslationCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_translate) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            final EditText et = new EditText(this);
            ab.setView(et);
            ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String text = et.getText().toString();
//                    translator.translate(text);
                    googleTranslator.translate(text, Language.Chinese_Traditional.getLangCode());
                }
            });
            ab.setNegativeButton(android.R.string.cancel, null);
            ab.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

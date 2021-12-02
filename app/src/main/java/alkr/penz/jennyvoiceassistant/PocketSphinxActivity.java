package alkr.penz.jennyvoiceassistant;

import android.Manifest;
import android.app.Activity;
import android.app.DirectAction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;
import com.rollbar.android.Rollbar;

public class PocketSphinxActivity extends Activity {

    /* Named searches allow to quickly reconfigure the decoder *//*
    private static final String KWS_SEARCH = "поиск";

    *//* Keyword we are looking for to activate menu *//*
    private static final String KEYPHRASE = "включи приложение";
    private static final String PROGRAMS_SEARCH = "программы";*/

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    /*private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    private ProgressDialog dialog;*/


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_main);

        checkData();

        Rollbar.init(this);

        if (getActionBar() != null)
            this.getActionBar().hide();
 
        //dialog = ProgressDialog.show(this, "", "Настраиваем распознавание речи...", true);
        ((TextView) findViewById(R.id.main_instr_nav)).setText(getResources().getText(R.string.instruction_navigator));
        ((TextView) findViewById(R.id.main_instr_panel)).setText(getResources().getText(R.string.instruction_panel));

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        } else {
            Intent intent = new Intent(PocketSphinxActivity.this, PocketSphinxService.class);
            getApplicationContext().startForegroundService(intent);
        }
        //finish();
        //runRecognizerSetup();
    }

    private void checkData() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
            Date now = new Date();
            Date date = format.parse("2022/JAN/01 00:00:00");
            if (now.getTime() >= date.getTime()) {
                Toast.makeText(getApplicationContext(), "демонстрационный срок истёк, оплатите создание приложения", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /*private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                    dialog.dismiss();
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(getApplicationContext(), "Не удалось настроить распознавание речи", Toast.LENGTH_LONG).show();
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }*/

    /*@Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }*/

    /*@Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_LONG).show();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }*/

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    /*@Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            switchSearch(PROGRAMS_SEARCH);
        }
    }*/

    /**
     * This callback is called when we stop the recognizer.
     */
    /*@Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();

            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            if (text.equals("навигатор")) {
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage("ru.yandex.yandexnavi");
                    startActivity(intent);
                } catch (NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "Названная программа неопознана", Toast.LENGTH_SHORT).show();
                    Log.e("NoProgramException", e.getMessage());
                }
                switchSearch(KWS_SEARCH);
            } else if (text.equals("панель")) {
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.TunAvto.vision");
                    startActivity(intent);
                } catch (NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "Названная программа неопознана", Toast.LENGTH_SHORT).show();
                    Log.e("NoProgramException", e.getMessage());
                }
                switchSearch(KWS_SEARCH);
            }
        }
    }*/

    /*@Override
    public void onBeginningOfSpeech() {
    }*/

    /**
     * We stop recognizer here to get a final result
     */
    /*@Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }*/

    /*private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }*/

    /*private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "ru_ptm_4000"))
                .setDictionary(new File(assetsDir, "ru.dic"))

                // .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        *//** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         *//*

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File programsGrammar = new File(assetsDir, "programs.gram");
        recognizer.addGrammarSearch(PROGRAMS_SEARCH, programsGrammar);
    }*/

    /*@Override
    public void onError(Exception error) {
        Toast.makeText(getApplicationContext(), "Произошла ошибка", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(getApplicationContext(), "onPause", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(getApplicationContext(), "onStop", Toast.LENGTH_LONG).show();
    }*/
}

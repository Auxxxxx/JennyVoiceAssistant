package alkr.penz.jennyvoiceassistant;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class PocketSphinxService extends Service implements RecognitionListener {

    private static final String KWS_SEARCH = "поиск";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "включи приложение";
    private static final String PROGRAMS_SEARCH = "программы";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    private ProgressDialog dialog;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "сервис стартанул", Toast.LENGTH_SHORT).show();
        if (recognizer == null) {
            runRecognizerSetup();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    //Assets assets = new Assets(PocketSphinxActivity.this);
                    Assets assets = new Assets(getBaseContext());
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
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
                    Toast.makeText(getApplicationContext(), "слушаю", Toast.LENGTH_SHORT).show();
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "ru_ptm_4000"))
                .setDictionary(new File(assetsDir, "ru.dic"))

                // .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File programsGrammar = new File(assetsDir, "programs.gram");
        recognizer.addGrammarSearch(PROGRAMS_SEARCH, programsGrammar);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            switchSearch(PROGRAMS_SEARCH);
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
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
    }
    @Override
    public void onError(Exception error) {
        Toast.makeText(getApplicationContext(), "Произошла ошибка", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}

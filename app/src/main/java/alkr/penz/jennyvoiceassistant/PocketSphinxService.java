package alkr.penz.jennyvoiceassistant;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class PocketSphinxService extends Service implements RecognitionListener {

    private static final String TAG = "dbugging";
    private static final String KWS_SEARCH = "поиск";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "включи приложение";
    private static final String PROGRAMS_SEARCH = "программы";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String CHANNEL_ID = "alkr.penz.jennyvoiceassistant";
    private static final int ONGOING_NOTIFICATION_ID = 3;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    private ProgressDialog dialog;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Message", NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        Intent notificationIntent = new Intent(this, PocketSphinxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("title")
                .setContentText("message")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

// Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        if (recognizer == null) {
            runRecognizerSetup();
            Toast.makeText(getApplicationContext(), "сервис стартанул", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "уже запущено", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "switchSearch");
        recognizer.stop();
        Log.d(TAG, "recognizer.stop();");

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH)) {
            recognizer.startListening(searchName);
            Log.d(TAG, "recognizer.startListening(KWS_SEARCH);");
        }
        else {
            recognizer.startListening(searchName, 10000);
            Log.d(TAG, "recognizer.startListening(searchName, 10000);");
        }
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
        if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
            switchSearch(KWS_SEARCH);
            Log.d(TAG, "onEndOfSpeech");
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Log.d(TAG, "onPartialResult(Hypothesis hypothesis)" + text);
        if (text.equals(KEYPHRASE)) {
            switchSearch(PROGRAMS_SEARCH);
            Log.d(TAG, "onPartialResult(Hypothesis hypothesis) switchSearch(PROGRAMS_SEARCH);");
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        Log.d(TAG, "onresult");
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
                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.TunAvto.Vision");
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
        Log.d(TAG, "error");
        Toast.makeText(getApplicationContext(), "Произошла ошибка", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_LONG).show();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }
}

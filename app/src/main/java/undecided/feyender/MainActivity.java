package undecided.feyender;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;
import com.github.kittinunf.fuel.util.Base64;
import java.util.Locale;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import kotlin.Pair;

public class MainActivity extends AppCompatActivity
{

    TextToSpeech t1;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiating Text to Speech
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status != TextToSpeech.ERROR)
                {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
    }

    public final static int MY_REQUEST_CODE = 1;

    //Asking the default camera to take the picture
    public void takePicture(View view)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }
    //Receiving image from the camera app
    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data)
    {
        final ArrayList<String> dangerItems = new ArrayList<String>();
        dangerItems.add("knife");
        dangerItems.add("weapon");
        dangerItems.add("flame");
        dangerItems.add("heat");
        dangerItems.add("gun");
        dangerItems.add("fire");

        if(requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK)
        {


            // Convert image data to bitmap
            Bitmap picture = (Bitmap)data.getExtras().get("data");
            // Set the bitmap as the source of the ImageView
            ((ImageView)findViewById(R.id.previewImage)).setImageBitmap(picture);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.JPEG, 90, byteStream);
            String base64Data = Base64.encodeToString(byteStream.toByteArray(), Base64.URL_SAFE);
            String requestURL = "https://vision.googleapis.com/v1/images:annotate?key=" + getResources().getString(R.string.mykey);

            // Create an array containing the LABEL_DETECTION feature
            JSONArray features = new JSONArray();
            JSONObject feature = new JSONObject();
            try {
                feature.put("type", "LABEL_DETECTION");
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
            features.put(feature);

            // Create an object containing the Base64-encoded image data
            JSONObject imageContent = new JSONObject();
            try {
                imageContent.put("content", base64Data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Put the array and object into a single request and then put the request into an array of requests
            JSONArray requests = new JSONArray();
            JSONObject request = new JSONObject();
            try {
                request.put("image", imageContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                request.put("features", features);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            requests.put(request);
            JSONObject postData = new JSONObject();
            try {
                postData.put("requests", requests);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Convert the JSON into a string
            String body = postData.toString();

            Fuel.post(requestURL).header(
                        new Pair<String, Object>("content-length", body.length()),
                        new Pair<String, Object>("content-type", "application/json")
                )
                .body(body.getBytes())
                .responseString(new Handler<String>() {
                    @Override
                    public void success(@NotNull Request request,
                                        @NotNull Response response,
                                        String data) {


                        // Access the labelAnnotations arrays
                        JSONArray labels = null;
                        try {
                            labels = new JSONObject(data)
                                    .getJSONArray("responses")
                                    .getJSONObject(0)
                                    .getJSONArray("labelAnnotations");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String results = "";
                        String danger = "danger";

                        // Extract the description key for the first item in the JSON.
                        try {
                            results = labels.getJSONObject(0).getString("description");

                            //works different for danger items
                            if ( dangerItems.contains(labels.getJSONObject(0).getString("description")) ){
                                ((TextView)findViewById(R.id.resultsText)).setTextColor(Color.RED);
                                //results = "ah" + " " + results + " " + danger;
                                Toast.makeText(getApplicationContext(), results,Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(), danger,Toast.LENGTH_SHORT).show();
                                t1.speak(danger, TextToSpeech.QUEUE_FLUSH, null);
                                Thread.sleep(750);
                                t1.speak(results, TextToSpeech.QUEUE_FLUSH, null);
                                Thread.sleep(750);
                                t1.speak(danger, TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else {
                                ((TextView)findViewById(R.id.resultsText)).setTextColor(Color.WHITE);
                                Toast.makeText(getApplicationContext(), results,Toast.LENGTH_SHORT).show();
                                t1.speak(results, TextToSpeech.QUEUE_FLUSH, null);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        // Display the annotations inside the TextView
                        ((TextView)findViewById(R.id.resultsText)).setText(results);

                    }

                    @Override
                    public void failure(@NotNull Request request,
                                        @NotNull Response response,
                                        @NotNull FuelError fuelError) {}
                });

        }
    }
}

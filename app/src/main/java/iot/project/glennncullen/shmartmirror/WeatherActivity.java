package iot.project.glennncullen.shmartmirror;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class WeatherActivity extends AppCompatActivity {

    // string for debug
    static final String LOG_TAG = NewsActivity.class.getCanonicalName();

    // singleton Handler
    private Handler handler;

    // View components
    Button weatherLogoutBtn;
    TextView weatherDayLbl;
    TextView weatherTempLbl;
    TextView weatherDescriptionLbl;
    TextView weatherLocationLbl;
    TextView windSpeedLbl;
    TextView windDirectionLbl;
    ImageView weatherImg;

    // Strings for icon display and day
    String icon;
    String day;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // get instance of Handler singleton and set title of actionbar to Weather
        handler = Handler.getInstance(getApplicationContext(), this);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Weather");

        // when weatherLogoutBtn is clicked, disableInteraction() and logout()
        weatherLogoutBtn = (Button) findViewById(R.id.weatherLogoutBtn);
        weatherLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableInteraction();
                logout();
            }
        });

        // initialise components
        weatherDayLbl = (TextView) findViewById(R.id.weatherDayLbl);
        weatherTempLbl = (TextView) findViewById(R.id.weatherTempLbl);
        weatherDescriptionLbl = (TextView) findViewById(R.id.weatherDescriptionLbl);
        weatherLocationLbl = (TextView) findViewById(R.id.weatherLocationLbl);
        windSpeedLbl = (TextView) findViewById(R.id.windSpeedLbl);
        windDirectionLbl = (TextView) findViewById(R.id.windDirectionLbl);
        weatherImg = (ImageView) findViewById(R.id.weatherImg);

    } // END OF CREATE


    /**
     * if message doesn't contain 'temperature' field, then return.
     * using message json, build components of cardView using the
     * information provided.
     *
     * Expand on day from three letters to full words.
     *
     * choose correct image based on icon in message.
     *
     * @param message json object from subscribe
     */
    public void update(JSONObject message){
        if(!message.has("temperature")) return;
        Log.i(LOG_TAG, "Trying to update weather with: " + message.toString());
        try {
            weatherTempLbl.setText((String) String.valueOf(message.get("temperature")));
            weatherDescriptionLbl.setText((String) message.get("description"));
            weatherLocationLbl.setText((String) message.get("location"));
            windSpeedLbl.setText((String) String.valueOf(message.get("wind_speed")) + " kmph");
            windDirectionLbl.setText((String) message.get("wind_direction"));
            day = (String) message.get("day");
            icon = (String) message.get("icon");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (day){
            case "Now":
                weatherDayLbl.setText(day);
                break;
            case "Mon":
                weatherDayLbl.setText("Monday");
                break;
            case "Tue":
                weatherDayLbl.setText("Tuesday");
                break;
            case "Wed":
                weatherDayLbl.setText("Wednesday");
                break;
            case "Thu":
                weatherDayLbl.setText("Thursday");
                break;
            case "Fri":
                weatherDayLbl.setText("Friday");
                break;
            case "Sat":
                weatherDayLbl.setText("Saturday");
                break;
            case "Sun":
                weatherDayLbl.setText("Sunday");
                break;
        }

        switch (icon){
            case "01d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.sunny));
                break;
            case "01n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.clear_night));
                break;
            case "02d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.partly_cloudy));
                break;
            case "02n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.partly_cloudy));
                break;
            case "03d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.cloudy));
                break;
            case "03n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.cloudy_night));
                break;
            case "04d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.cloudy));
                break;
            case "04n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.cloudy_night));
                break;
            case "09d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "09n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "10d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "10n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "11d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "11n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.rain));
                break;
            case "13d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.snow));
                break;
            case "13n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.snow));
                break;
            case "50d":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.fog));
                break;
            case "50n":
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.fog));
                break;
            default:
                weatherImg.setImageDrawable(getApplicationContext().getDrawable(R.drawable.clear_day));
                break;
        }
    }

    // on back button pressed, do nothing
    @Override
    public void onBackPressed(){
    }

    /**
     * disable interaction with activity
     */
    public void disableInteraction(){
        weatherLogoutBtn.setEnabled(false);
    }

    /**
     * handle logout event by publishing logout
     */
    private void logout(){
        try {
            handler.publish(new JSONObject().put("logout", 1), "/iotappdev/logout/");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to create JSONObject for logout");
            e.printStackTrace();
        }
    }
}

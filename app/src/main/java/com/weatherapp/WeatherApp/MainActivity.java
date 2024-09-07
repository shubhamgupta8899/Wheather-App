package com.weatherapp.WeatherApp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.location.Location;
import android.location.Geocoder;

import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.weatherapp.WeatherApp.databinding.ActivityMainBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class MainActivity extends AppCompatActivity {

   private ActivityMainBinding binding;
   private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
   private static final long LOCATION_UPDATE_INTERVAL = 50000000; // 5 seconds
   private LocationCallback locationCallback;
   private boolean isCitySearchInProgress = false;


   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
      setContentView(binding.getRoot());
      checkLocationPermission();
      initLocationCallback();
      fetchCurrentLocation();
      SearchCity();
   }

   private void checkLocationPermission() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
         if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                 != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
         }
      }
   }


   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted, fetch current location
            fetchCurrentLocation();
         } else {
            // Location permission denied, handle accordingly (show a message, disable location features, etc.)
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
         }
      }
   }

   private void initLocationCallback() {
      locationCallback = new LocationCallback() {
         @Override
         public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null && locationResult.getLastLocation() != null) {
               double latitude = locationResult.getLastLocation().getLatitude();
               double longitude = locationResult.getLastLocation().getLongitude();
               // Use latitude and longitude as needed
               fetchWeatherDataByLocation(latitude, longitude);
            }
         }
      };
   }

   private void fetchWeatherDataByLocation(double latitude, double longitude) {
      Geocoder geocoder = new Geocoder(this, Locale.getDefault());

      try {
         List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

         if (addresses != null && addresses.size() > 0) {
            String cityName = addresses.get(0).getLocality();
            fetchWeatherData(cityName);
         } else {
            // Handle the case where no address is found
            Toast.makeText(this, "City name not found", Toast.LENGTH_SHORT).show();
         }
      } catch (IOException e) {
         e.printStackTrace();
         // Handle the exception
      }
   }

   private void fetchCurrentLocation() {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
              PackageManager.PERMISSION_GRANTED) {
         LocationRequest locationRequest = LocationRequest.create()
                 .setInterval(LOCATION_UPDATE_INTERVAL)
                 .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

         LocationServices.getFusedLocationProviderClient(this)
                 .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
      }
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      if (locationCallback != null) {
         LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
      }
   }
   private void SearchCity() {
      SearchView searchView = binding.searchView;
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
         @Override
         public boolean onQueryTextSubmit(String query) {
            if (query != null) {
               isCitySearchInProgress = true;
               fetchWeatherData(query);
            }
            return true;
         }

         @Override
         public boolean onQueryTextChange(String newText) {

            return true;
         }
      });
   }

   private void fetchWeatherData(String cityName) {
      Retrofit retrofit = new Retrofit.Builder()
              .addConverterFactory(GsonConverterFactory.create())
              .baseUrl("https://api.openweathermap.org/data/2.5/")
              .build();

      ApiInterface apiInterface = retrofit.create(ApiInterface.class);

      Call<WeatherApp> responseCall = apiInterface.getWeatherData(cityName, "fba755363e7763df33bd6f0f8a6fb8fe", "metric");

      responseCall.enqueue(new Callback<WeatherApp>() {
         @SuppressLint("SetTextI18n")
         @Override
         public void onResponse(Call<WeatherApp> call, Response<WeatherApp> response) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {

                  handleResponse(response.body(), cityName);
               }
            });
         }

         @Override
         public void onFailure(Call<WeatherApp> call, Throwable t) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  handleFailure(t);
               }
            });
         }
      });
   }

   private void handleResponse(WeatherApp responseBody, String cityName) {
      if (responseBody != null) {
         double temperature = responseBody.getMain().getTemp();
         long humidity = responseBody.getMain().getHumidity();
         long sunrise = responseBody.getSys().getSunrise();
         long sunset = responseBody.getSys().getSunset();
         double seaLevel = responseBody.getMain().getPressure();
         String condition = responseBody.getWeather().get(0).getDescription().toLowerCase();
         double maxTemp = responseBody.getMain().getTempMax();
         double minTemp = responseBody.getMain().getTempMin();
         double windSpeed = responseBody.getWind().getSpeed();
         int textColor = getColorForWeatherCondition(condition);

         //for color
         binding.temp.setTextColor(textColor);
         binding.weather.setTextColor(textColor);
         binding.condition.setTextColor(textColor);
         binding.maxTemp.setTextColor(textColor);
         binding.minTemp.setTextColor(textColor);
         binding.humadity.setTextColor(textColor);
         binding.sunrise.setTextColor(textColor);
         binding.sunset.setTextColor(textColor);
         binding.windspeed.setTextColor(textColor);
         binding.sea.setTextColor(textColor);
         binding.condition.setTextColor(textColor);
         binding.day.setTextColor(textColor);
         binding.today.setTextColor(textColor);
         binding.date.setTextColor(textColor);
         binding.autoCompleteTextView.setTextColor(textColor);
         binding.time.setTextColor(textColor);

         binding.temp.setText(temperature + " °C");
         binding.weather.setText(String.valueOf(condition));
         binding.maxTemp.setText("Max Temp : " + maxTemp + "°C");
         binding.minTemp.setText("Min Temp : " + minTemp + "°C");
         binding.humadity.setText(humidity + "%");
         binding.sunrise.setText(time(sunrise));
         binding.sunset.setText(time(sunset));
         binding.windspeed.setText(windSpeed + " M/S");
         binding.sea.setText("Sea Level: " + seaLevel + " hPa");
         binding.condition.setText(String.valueOf(condition));
         binding.day.setText(dayName(System.currentTimeMillis()));
         binding.today.setText("Today");
         binding.date.setText(date());
         binding.autoCompleteTextView.setText(cityName);
         binding.time.setText(currentTime());

         setTextViewColorBasedOnCondition(binding.textHumadity, condition);
         setTextViewColorBasedOnCondition(binding.textWindspeed, condition);
         setTextViewColorBasedOnCondition(binding.textCondition, condition);
         setTextViewColorBasedOnCondition(binding.textSunrise, condition);
         setTextViewColorBasedOnCondition(binding.textSunset, condition);
         setTextViewColorBasedOnCondition(binding.textSea, condition);

         changeImagesAccordingToWeatherCondition(condition);
      }
   }

   private void setTextViewColorBasedOnCondition(TextView textView, String condition) {
      int textColor = getColorForWeatherCondition(condition);
      textView.setTextColor(textColor);
   }

   private int getColorForWeatherCondition(String condition) {
      int colorResId;

      switch (condition) {
         case "mostly sunny":
         case "sunny":
            colorResId = R.color.black; // Define this color in your resources
            break;

         case "partly cloudy":
         case "clouds":
         case "haze":
            colorResId = R.color.white; // Define this color in your resources
            break;

         case "rain":
         case "light rain":
         case "moderate rain":
         case "heavy rain":
            colorResId = R.color.black; // Define this color in your resources
            break;

         case "snow":
         case "light snow":
         case "moderate snow":
         case "heavy snow":
            colorResId = R.color.black; // Define this color in your resources
            break;

         case "night":
         case "light night":
         case "star night":
            colorResId = R.color.white; // Define this color in your resources
            break;

         // Default case
         default:
            colorResId = R.color.black; // Define this color in your resources
            break;
      }

      return getResources().getColor(colorResId);
   }

   private void handleFailure(Throwable t) {
      // Handle failure
      Log.e(TAG, "onFailure: " + t.getMessage(), t);
   }

   private void changeImagesAccordingToWeatherCondition(String conditions) {
      Drawable backgroundDrawable;
      int animationResource;

      int currentHour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
      // Check if it's daytime or nighttime
      boolean isDaytime = currentHour >= 6 && currentHour < 18;

      switch (conditions) {
         case "sunny":
         case "clear sky":
            backgroundDrawable = getResources().getDrawable(isDaytime ? R.drawable.sunny_bg : R.drawable.night_background);
            animationResource = isDaytime ? R.raw.sun : R.raw.cloud;
            break;

         case "partly cloudy":
         case "clouds":
         case "haze":
            backgroundDrawable = getResources().getDrawable(R.drawable.cloud_bg);
            animationResource = R.raw.cloud;
            setImageViewColorFilter(R.id.humadityImage, R.color.white);
            setImageViewColorFilter(R.id.windspeedImage, R.color.white);
            setImageViewColorFilter(R.id.conditionImage, R.color.white);
            setImageViewColorFilter(R.id.sunriseImage, R.color.white);
            setImageViewColorFilter(R.id.sunsetImage, R.color.white);
            setImageViewColorFilter(R.id.seaImage, R.color.white);
            break;

         case "rain":
         case "light rain":
         case "moderate rain":
         case "heavy rain":
            backgroundDrawable = getResources().getDrawable(R.drawable.rain_bg);
            animationResource = R.raw.rain;
//            setImageViewColorFilter(R.id.humadityImage, R.color.white);
//            setImageViewColorFilter(R.id.windspeedImage, R.color.white);
//            setImageViewColorFilter(R.id.conditionImage, R.color.white);
//            setImageViewColorFilter(R.id.sunriseImage, R.color.white);
//            setImageViewColorFilter(R.id.sunsetImage, R.color.white);
//            setImageViewColorFilter(R.id.seaImage, R.color.white);
            break;

         case "snow":
         case "light snow":
         case "moderate snow":
         case "heavy snow":
            backgroundDrawable = getResources().getDrawable(R.drawable.snow_bg);
            animationResource = R.raw.snow;
            break;

         case "night":
         case "light night":
         case "star night":
         case "broken clouds":
            backgroundDrawable = getResources().getDrawable(R.drawable.night_background);
            animationResource = R.raw.sun;
            setImageViewColorFilter(R.id.humadityImage, R.color.white);
            setImageViewColorFilter(R.id.windspeedImage, R.color.white);
            setImageViewColorFilter(R.id.conditionImage, R.color.white);
            setImageViewColorFilter(R.id.sunriseImage, R.color.white);
            setImageViewColorFilter(R.id.sunsetImage, R.color.white);
            setImageViewColorFilter(R.id.seaImage, R.color.white);
            break;

         case "overcast clouds":
            backgroundDrawable = getResources().getDrawable(R.drawable.overcast_background);
            animationResource = R.raw.sun;
            setImageViewColorFilter(R.id.humadityImage, R.color.white);
            setImageViewColorFilter(R.id.windspeedImage, R.color.white);
            setImageViewColorFilter(R.id.conditionImage, R.color.white);
            setImageViewColorFilter(R.id.sunriseImage, R.color.white);
            setImageViewColorFilter(R.id.sunsetImage, R.color.white);
            setImageViewColorFilter(R.id.seaImage, R.color.white);
            break;

         case "fog":
         case "smoke":
            backgroundDrawable = getResources().getDrawable(R.drawable.fog_background);
            animationResource = R.raw.rain;
            break;

//         case "clear sky":
//            backgroundDrawable = getResources().getDrawable(R.drawable.night_background);
//            animationResource = R.raw.cloud;
//            setImageViewColorFilter(R.id.humadityImage, R.color.white);
//            setImageViewColorFilter(R.id.windspeedImage, R.color.white);
//            setImageViewColorFilter(R.id.conditionImage, R.color.white);
//            setImageViewColorFilter(R.id.sunriseImage, R.color.white);
//            setImageViewColorFilter(R.id.sunsetImage, R.color.white);
//            setImageViewColorFilter(R.id.seaImage, R.color.white);
//            break;

         case "scattered clouds":
            backgroundDrawable = getResources().getDrawable(R.drawable.sunny_bg);
            animationResource = R.raw.sun;
            break;


         case "mist":
            backgroundDrawable = getResources().getDrawable(R.drawable.fog_background);
            animationResource = R.raw.sun;
            break;

         // Default case
         default:
            backgroundDrawable = getResources().getDrawable(R.drawable.sunny_bg);
            animationResource = R.raw.sun;
            break;
      }

      binding.getRoot().setBackground(backgroundDrawable);
      binding.lottieAnimationView.setAnimation(animationResource);
      binding.lottieAnimationView.playAnimation();
   }

   private void setImageViewColorFilter(int imageViewId, int colorResId) {
      ImageView imageView = findViewById(imageViewId);
      int color = getResources().getColor(colorResId);
      imageView.setColorFilter(color);
   }

   private String date() {
      SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
      return sdf.format(new Date());
   }

   private String time(long timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
      return sdf.format(new Date(timestamp * 1000));
   }

   private String dayName(long timestamp) {
      SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
      return sdf.format(new Date());
   }

   private String currentTime() {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
      return sdf.format(new Date());
   }
}
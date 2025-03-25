package com.example.freshtrack;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.notifications.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.File;
import com.example.freshtrack.api.PredictionApi;
import com.example.freshtrack.api.PredictionResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import java.util.Map;
import java.util.HashMap;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.core.app.ActivityCompat;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;

public class AddListActivity extends AppCompatActivity {

    private EditText etFoodName;
    private EditText etExpiryDate;
    private Spinner spinnerCategory;
    private EditText etWeight;
    private EditText etCount;
    private Button btnSave;
    private Button btnScanCamera;
    private FirebaseModel firebaseModel;
    private FirebaseAuth mAuth;
    private TextRecognizer textRecognizer;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private long selectedExpiryDate;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            // If not logged in, redirect to authentication
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivityAuthentication.class));
            finish();
            return;
        }

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Item");
        }

        // Initialize Firebase
        firebaseModel = new FirebaseModel();

        // Initialize views
        etFoodName = findViewById(R.id.etFoodName);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etWeight = findViewById(R.id.etWeight);
        etCount = findViewById(R.id.etCount);
        btnSave = findViewById(R.id.btnSave);
        btnScanCamera = findViewById(R.id.btnScanCamera);

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize image picker launcher
        setupImagePicker();

        // Setup category spinner
        setupCategorySpinner();

        // Setup date picker
        etExpiryDate.setOnClickListener(v -> showDatePicker());

        // Setup save button
        btnSave.setOnClickListener(v -> saveItem());

        // Setup camera button
        btnScanCamera.setOnClickListener(v -> {
            checkCameraPermissionAndOpen();
        });

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        // Double-check permission before proceeding
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Create a file to save the image
        File photoFile = null;
        try {
            photoFile = createImageFile();
            Log.d("Camera", "Created image file: " + photoFile.getAbsolutePath());
        } catch (IOException ex) {
            Log.e("AddListActivity", "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Continue only if the file was successfully created
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    "com.example.freshtrack.fileprovider",
                    photoFile);
            Log.d("Camera", "Photo URI: " + photoUri.toString());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            imagePickerLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan text", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImage(Uri imageUri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        // Process the recognized text
                        String recognizedText = text.getText();
                        // Try to extract information from the recognized text
                        String[] lines = recognizedText.split("\n");
                        if (lines.length > 0) {
                            // Set food name from first line
                            etFoodName.setText(lines[0]);
                            
                            // Try to find weight and count information
                            for (String line : lines) {
                                line = line.toLowerCase();
                                // Look for weight information
                                if (line.contains("weight") || line.contains("lbs") || line.contains("pounds")) {
                                    String weight = line.replaceAll("[^0-9.]", "");
                                    if (!weight.isEmpty()) {
                                        etWeight.setText(weight);
                                    }
                                }
                                // Look for count information
                                if (line.contains("count") || line.contains("quantity") || line.contains("qty")) {
                                    String count = line.replaceAll("[^0-9]", "");
                                    if (!count.isEmpty()) {
                                        etCount.setText(count);
                                    }
                                }
                                // Look for date information
                                if (line.matches(".*\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}.*")) {
                                    try {
                                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                                        String dateStr = line.replaceAll("[^0-9/]", "");
                                        sdf.parse(dateStr);
                                        etExpiryDate.setText(dateStr);
                                    } catch (Exception e) {
                                        // Ignore invalid date formats
                                    }
                                }
                            }
                            
                            Toast.makeText(this, "Text recognized successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No text found in image", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to recognize text: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCategorySpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.food_categories, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerCategory.setAdapter(adapter);
    }

    private void showDatePicker() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    // Create Calendar instance with selected date
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Format date as MM/dd/yyyy
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    String formattedDate = dateFormat.format(selectedCalendar.getTime());
                    
                    // Set the formatted date
                    etExpiryDate.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void saveItem() {
        String foodName = etFoodName.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String weight = etWeight.getText().toString().trim();
        String count = etCount.getText().toString().trim();

        if (foodName.isEmpty()) {
            etFoodName.setError("Please enter food name");
            return;
        }

        if (expiryDate.isEmpty()) {
            etExpiryDate.setError("Please select expiry date");
            return;
        }

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivityAuthentication.class));
            finish();
            return;
        }

        // Get the current user's ID
        String userId = currentUser.getUid();
        
        // Parse the expiry date string to timestamp
        long dateAdded = System.currentTimeMillis();
        long expiryTimestamp;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            expiryTimestamp = sdf.parse(expiryDate).getTime();
        } catch (Exception e) {
            Log.e("AddListActivity", "Error parsing date", e);
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create FoodItem with all fields
        FoodItem foodItem = new FoodItem(
            foodName,              // name
            dateAdded,            // dateAdded (current timestamp)
            expiryTimestamp,      // expiryDate
            userId,               // userId (from current user)
            category,             // category (from spinner)
            1,                    // quantity (default)
            "piece",              // unit (default)
            "",                   // notes (empty by default)
            weight,               // weight (from input)
            count                 // count (from input)
        );
        
        // Add log before saving
        Log.d("AddListActivity", "Attempting to save food item: " + foodName + " for user: " + userId);
        
        firebaseModel.addFoodItem(foodItem)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AddListActivity", "Food item saved successfully");
                    // Schedule notification for the new item
                    String notificationId = firebaseModel.getNewNotificationId();
                    if (notificationId != null) {
                        NotificationHelper notificationHelper = new NotificationHelper(this);
                        notificationHelper.scheduleNotification(
                                userId,
                                foodName,
                                notificationId,
                                expiryTimestamp
                        );
                    }
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e("AddListActivity", "Error saving food item", e);
                Toast.makeText(this, "Error saving food item: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
    }

    private boolean isExpiringSoon(FoodItem item) {
        // Get current time
        long currentTime = System.currentTimeMillis();
        
        // Check if expiry date is within the next 3 days (or your preferred threshold)
        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L;
        return (item.getExpiryDate() - currentTime) <= threeDaysInMillis;
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnDashboard = bottomNav.findViewById(R.id.btnDashboard);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnNotifications = bottomNav.findViewById(R.id.btnNotifications);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, MainActivityHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            // Already on Add, do nothing
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, NotificationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (photoUri != null) {
                        // We have a full-size photo from the camera
                        // Convert content URI to file path
                        String imagePath = getRealPathFromURI(photoUri);
                        if (imagePath != null) {
                            Log.d("Camera", "Image path: " + imagePath);
                            processImageForTextRecognition(imagePath);
                            // Reset photoUri after processing
                            photoUri = null;
                        }
                    } else {
                        // Handle gallery selection
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            String imagePath = getPathFromUri(uri);
                            if (imagePath != null) {
                                processImageForTextRecognition(imagePath);
                            } else {
                                Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });
    }

    private String getRealPathFromURI(Uri contentUri) {
        try {
            if (contentUri.getPath().startsWith("/storage/")) {
                // If it's already a file path, return it directly
                return contentUri.getPath();
            }
            
            // For content URIs, use FileProvider to get a file path
            if (contentUri.toString().startsWith("content://")) {
                // For camera photos taken with our app, we can directly use the path
                if (photoUri != null && contentUri.equals(photoUri)) {
                    // Get the path from the URI directly
                    String path = contentUri.getPath();
                    if (path != null && new File(path).exists()) {
                        return path;
                    }
                    
                    // Try to get the path from the URI's last path segment
                    String fileName = contentUri.getLastPathSegment();
                    if (fileName != null) {
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        File[] files = storageDir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.getName().contains(fileName)) {
                                    return file.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
                
                // For other content URIs, try to create a temporary file
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                    File tempFile = saveBitmapToFile(bitmap);
                    if (tempFile != null) {
                        return tempFile.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e("AddListActivity", "Error creating bitmap from content URI", e);
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e("AddListActivity", "Error getting real path from URI", e);
            
            // Fallback: try to use the file path directly from the URI
            try {
                File file = new File(contentUri.getPath());
                if (file.exists()) {
                    return file.getAbsolutePath();
                }
            } catch (Exception ex) {
                Log.e("AddListActivity", "Fallback path retrieval failed", ex);
            }
            
            return null;
        }
    }

    private String getPathFromUri(Uri uri) {
        try {
            String[] projection = {android.provider.MediaStore.Images.Media.DATA};
            android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        } catch (Exception e) {
            Log.e("AddListActivity", "Error getting path from URI", e);
            return null;
        }
    }

    private void processImageForTextRecognition(String imagePath) {
        // Show loading state
        Toast.makeText(this, "Analyzing image...", Toast.LENGTH_SHORT).show();
        Log.d("API", "Processing image: " + imagePath);

        // Variable to hold the final path
        String finalPath = imagePath;

        // If imagePath is null, try to use photoUri directly
        if (imagePath == null && photoUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    // Save bitmap to a temporary file
                    File tempFile = saveBitmapToFile(bitmap);
                    if (tempFile != null) {
                        imagePath = tempFile.getAbsolutePath();
                        finalPath = tempFile.getAbsolutePath();
                        Log.d("API", "Created temporary file from URI: " + imagePath);
                    }
                }
            } catch (Exception e) {
                Log.e("API", "Error creating bitmap from URI", e);
            }
        }

        // Create a final copy for use in callbacks
        final String finalImagePath = finalPath;

        // Create file object from image path
        File file = new File(imagePath);
        if (!file.exists()) {
            Log.e("API", "File does not exist: " + imagePath);
            
            // Try to use the photoUri directly as a fallback
            if (photoUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                    processImageLocally(bitmap);
                    return;
                } catch (Exception e) {
                    Log.e("API", "Error processing URI directly", e);
                }
            }
            
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Send the image to the external API
        try {
            Log.d("API", "Sending image to external API: " + imagePath);
            
            // Skip connectivity test and directly send the image
            sendImageToApi(file, finalImagePath);
        } catch (Exception e) {
            Log.e("API", "Error in connectivity test: " + e.getMessage(), e);
            Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show();
            processImageLocallyFromPath(finalImagePath);
        }
    }

    private void processImageLocally(Bitmap bitmap) {
        try {
            Log.d("API", "Processing bitmap directly");
            
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String text = visionText.getText();
                    if (!text.isEmpty()) {
                        // Extract the first line or word as the food name
                        String foodName = text.split("\n")[0].trim();
                        Log.d("API", "Local text recognition result: " + foodName);
                        
                        // Update food name field
                        etFoodName.setText(foodName);
                        
                        // Calculate and set expiry date based on the food type
                        setExpiryDateForFood(foodName);
                    } else {
                        Toast.makeText(AddListActivity.this, 
                            "No text found in image", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("API", "Local text recognition failed", e);
                    Toast.makeText(AddListActivity.this, 
                        "Failed to process image", Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e("API", "Error in local processing", e);
        }
    }

    private void setExpiryDateForFood(String foodName) {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        
        // Add days based on food type
        int daysToAdd = getFoodExpiryDays(foodName.toLowerCase());
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
        
        // Set the calculated date
        this.selectedExpiryDate = calendar.getTimeInMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        etExpiryDate.setText(sdf.format(calendar.getTime()));
    }

    private int getFoodExpiryDays(String foodName) {
        // Define expiry days for different foods
        Map<String, Integer> expiryDays = new HashMap<>();
        expiryDays.put("apple", 14);
        expiryDays.put("banana", 5);
        expiryDays.put("orange", 10);
        expiryDays.put("avocado", 7);
        expiryDays.put("mango", 7);
        expiryDays.put("strawberry", 5);
        expiryDays.put("tomato", 7);
        expiryDays.put("cucumber", 7);
        expiryDays.put("carrot", 21);
        // Add more foods as needed
        
        return expiryDays.getOrDefault(foodName, 7); // Default 7 days if not found
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File outputDir = getCacheDir();
            File outputFile = File.createTempFile("camera_image", ".jpg", outputDir);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            
            return outputFile;
        } catch (Exception e) {
            Log.e("AddListActivity", "Error saving bitmap to file", e);
            return null;
        }
    }

    private void processImageLocallyFromPath(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                processImageLocally(bitmap);
            } else {
                Log.e("API", "Failed to decode bitmap from file: " + imagePath);
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("API", "Error in local processing from path", e);
        }
    }

    private void sendImageToApi(File file, String finalImagePath) {
        try {
            // Create RequestBody instance from file
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            
            // Create MultipartBody.Part using file request-body
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            Log.d("API", "Created multipart request body");
            
            // Create OkHttpClient with longer timeouts
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
            
            // Create Retrofit instance
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.176:8000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
            
            Log.d("API", "Making request to: http://192.168.1.176:8000/predict");
            
            // Create API interface instance
            PredictionApi api = retrofit.create(PredictionApi.class);
            
            // Make API call
            api.predictImage(body).enqueue(new Callback<PredictionResponse>() {
                @Override
                public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                    Log.d("API", "Response received, code: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        try {
                            Log.e("API", "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e("API", "Could not read error body");
                        }
                        // Fall back to local processing
                        processImageLocallyFromPath(finalImagePath);
                        return;
                    }
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String prediction = response.body().getPrediction();
                        Log.d("API", "Prediction: " + prediction);
                        
                        if (prediction != null && !prediction.isEmpty()) {
                            // Update food name field
                            etFoodName.setText(prediction);
                            
                            // Calculate and set expiry date based on the food type
                            setExpiryDateForFood(prediction);
                        } else {
                            Log.e("API", "Empty prediction received");
                            processImageLocallyFromPath(finalImagePath);
                        }
                    } else {
                        Toast.makeText(AddListActivity.this, 
                            "Error analyzing image", Toast.LENGTH_SHORT).show();
                        processImageLocallyFromPath(finalImagePath);
                    }
                }
                
                @Override
                public void onFailure(Call<PredictionResponse> call, Throwable t) {
                    Log.e("API", "Request failed", t);
                    Log.e("API", "URL: " + call.request().url());
                    Log.e("API", "Error message: " + t.getMessage());
                    Log.e("API", "Error cause: " + (t.getCause() != null ? t.getCause().getMessage() : "unknown"));
                    Toast.makeText(AddListActivity.this, 
                        "Failed to connect to server", Toast.LENGTH_SHORT).show();
                    
                    // Fall back to local processing
                    processImageLocallyFromPath(finalImagePath);
                }
            });
        } catch (Exception e) {
            Log.e("API", "Error processing image: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            // Fall back to local processing
            processImageLocallyFromPath(finalImagePath);
        }
    }
}
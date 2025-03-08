package com.example.freshtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private ShapeableImageView profileImage;
    private ImageButton btnEditPhoto;
    private TextInputEditText etFirstName, etLastName, etEmail, etMobile;
    private MaterialButton btnSaveProfile;
    private FirebaseModel firebaseModel;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraImageUri;
    private String userId = "testUser123"; // Temporarily hardcoded, replace with actual user ID later
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupImagePicker();
        setupCameraLauncher();
        loadUserProfile();
        setupClickListeners();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        firebaseModel = new FirebaseModel();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        loadImageWithGlide(selectedImageUri);
                    }
                }
            }
        );
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        loadImageWithGlide(selectedImageUri);
                    }
                }
            }
        );
    }

    private void loadImageWithGlide(Uri imageUri) {
        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(profileImage);
    }

    private void setupClickListeners() {
        btnEditPhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Photo"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Picture Options");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Take Photo
                    checkAndRequestCameraPermission();
                    break;
                case 1: // Choose from Gallery
                    checkAndRequestStoragePermission();
                    break;
                case 2: // Remove Photo
                    confirmAndDeletePhoto();
                    break;
            }
        });
        builder.show();
    }

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        "com.example.freshtrack.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(cameraIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
            "JPEG_" + System.currentTimeMillis() + "_",
            ".jpg",
            storageDir
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    openCamera();
                } else {
                    openGallery();
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserProfile() {
        firebaseModel.getUserProfile(userId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etFirstName.setText(documentSnapshot.getString("firstName"));
                etLastName.setText(documentSnapshot.getString("lastName"));
                etEmail.setText(documentSnapshot.getString("email"));
                etMobile.setText(documentSnapshot.getString("mobile"));

                currentProfileImageUrl = documentSnapshot.getString("profileImage");
                if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                    Glide.with(this)
                        .load(currentProfileImageUrl)
                        .centerCrop()
                        .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.profile);
                }
            }
        }).addOnFailureListener(e ->
            Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("email", email);
        profileData.put("mobile", mobile);

        // If there's a new image selected
        if (selectedImageUri != null) {
            // If there's an existing profile photo, delete it first
            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                FirebaseStorage.getInstance().getReferenceFromUrl(currentProfileImageUrl)
                    .delete()
                    .addOnSuccessListener(aVoid -> uploadNewImageAndSaveProfile(profileData))
                    .addOnFailureListener(e -> {
                        // If deletion fails, still try to upload new image
                        uploadNewImageAndSaveProfile(profileData);
                    });
            } else {
                uploadNewImageAndSaveProfile(profileData);
            }
        } else {
            // If no new image is selected, keep the existing image URL
            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                profileData.put("profileImage", currentProfileImageUrl);
            }
            saveProfileData(profileData);
        }
    }

    private void uploadNewImageAndSaveProfile(Map<String, Object> profileData) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
            .child("profile_images")
            .child(userId + "_" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    profileData.put("profileImage", uri.toString());
                    currentProfileImageUrl = uri.toString();
                    saveProfileData(profileData);
                });
            })
            .addOnFailureListener(e -> {
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText("Save Changes");
                Toast.makeText(this, "Failed to upload image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void saveProfileData(Map<String, Object> profileData) {
        firebaseModel.updateUserProfile(userId, profileData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText("Save Changes");
                Toast.makeText(this, "Error updating profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void confirmAndDeletePhoto() {
        new AlertDialog.Builder(this)
            .setTitle("Remove Profile Picture")
            .setMessage("Are you sure you want to remove your profile picture?")
            .setPositiveButton("Remove", (dialog, which) -> deleteProfilePhoto())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteProfilePhoto() {
        // Show loading state
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Deleting...");

        // Reset the profile image to default
        profileImage.setImageResource(R.drawable.profile);
        selectedImageUri = null;

        // If there's an existing profile photo in Firebase Storage, delete it
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(currentProfileImageUrl)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update the user profile to remove the image URL
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImage", "");
                    firebaseModel.updateUserProfile(userId, updates)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                            currentProfileImageUrl = null;
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(this, "Error updating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show())
                        .addOnCompleteListener(task -> {
                            btnSaveProfile.setEnabled(true);
                            btnSaveProfile.setText("Save Changes");
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Changes");
                });
        } else {
            btnSaveProfile.setEnabled(true);
            btnSaveProfile.setText("Save Changes");
        }
    }
} 
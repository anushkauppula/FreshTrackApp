package com.example.freshtrack.api;

import com.google.gson.annotations.SerializedName;

public class PredictionResponse {
    @SerializedName("prediction")
    private String prediction;
    private boolean success;
    private String message;

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 
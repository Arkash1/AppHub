package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Единый конверт ответа сервера (зеркало серверного ApiResponse).
 */
public class ApiResponse<T> {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public T data;

    @SerializedName("error")
    public ApiError error;

    @SerializedName("catalogVersion")
    public long catalogVersion;

    public boolean isSuccess() { return success; }

    public String errorMessage() {
        return error != null ? error.message : null;
    }
}

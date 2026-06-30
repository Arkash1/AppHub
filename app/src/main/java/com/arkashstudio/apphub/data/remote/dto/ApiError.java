package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Ошибка в конверте ответа. */
public class ApiError {
    @SerializedName("code")
    public String code;

    @SerializedName("message")
    public String message;
}

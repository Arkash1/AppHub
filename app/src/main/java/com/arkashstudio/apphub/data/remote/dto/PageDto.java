package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Страница пагинации (зеркало Spring Page). */
public class PageDto<T> {
    @SerializedName("content") public List<T> content;
    @SerializedName("totalElements") public long totalElements;
    @SerializedName("totalPages") public int totalPages;
    @SerializedName("number") public int number;       // текущая страница (0-based)
    @SerializedName("size") public int size;
    @SerializedName("first") public boolean first;
    @SerializedName("last") public boolean last;

    public List<T> items() {
        return content != null ? content : java.util.Collections.emptyList();
    }
}

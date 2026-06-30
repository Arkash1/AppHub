package com.arkashstudio.apphub.data.local;

import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.remote.dto.AppDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Конверсия между сетевыми DTO и Room-сущностями кэша.
 */
public final class EntityMapper {

    private EntityMapper() { }

    public static AppEntity toEntity(AppDto dto) {
        AppEntity e = new AppEntity();
        e.id = dto.id;
        e.packageName = dto.packageName;
        e.title = dto.title;
        e.developer = dto.developer;
        e.shortDesc = dto.shortDesc;
        e.category = dto.category;
        e.iconUrl = dto.iconUrl;
        e.rating = dto.rating;
        e.featured = dto.featured;
        e.latestVersionId = dto.latestVersionId;
        e.latestVersionName = dto.latestVersionName;
        e.latestVersionCode = dto.latestVersionCode;
        e.latestMinSdk = dto.latestMinSdk;
        e.latestFileSize = dto.latestFileSize;
        e.latestSha256 = dto.latestSha256;
        e.updatedAt = dto.updatedAt;
        return e;
    }

    public static List<AppEntity> toEntities(List<AppDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        List<AppEntity> list = new ArrayList<>(dtos.size());
        for (AppDto dto : dtos) {
            list.add(toEntity(dto));
        }
        return list;
    }
}

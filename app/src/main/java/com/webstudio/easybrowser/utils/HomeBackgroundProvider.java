package com.webstudio.easybrowser.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class HomeBackgroundProvider {
    private static final String IMAGE_PARAMS = "?auto=format&fit=crop&crop=entropy&w=1440&h=2560&q=86";
    private static final String UNSPLASH_REF = "?utm_source=easy_browser&utm_medium=referral";

    private static final Photo[] ALL_PHOTOS = {
            unsplashPhoto("photo-1548679847-1d4ff48016c7", "k6GpdsPJSZw", "Fabrizio Conti"),
            unsplashPhoto("photo-1485256807238-97782da2fa07", "d3ci37Gcgxg", "cetteup"),
            unsplashPhoto("photo-1716417511759-dd9c0f353ef9", "2CqhW4RN4Qo", "Benjamin Ashton"),
            unsplashPhoto("photo-1706191465084-0e2f89a148fa", "FTUSP0ZH49I", "Ashish Kumar Senapati"),
            unsplashPhoto("photo-1768210837703-6fe5f5afbaa9", "uBjBr9CvNiw", "Martina Nette"),
            unsplashPhoto("photo-1767450327267-8075d82d4924", "5KsdEQtfmOw", "Kristaps Ungurs"),
            unsplashPhoto("photo-1772289495953-1271fe108a6c", "gSIjbABf9sc", "NIR HIMI"),
            unsplashPhoto("photo-1759434190960-87511b2a5e5c", "IWhpMyaol8c", "Jonny Gios"),
            unsplashPhoto("photo-1760716189755-6a412581a316", "TiHFUrNQL1Y", "Alex Moliski"),
            unsplashPhoto("photo-1635965072050-9b608060234d", "d01xcR3puFA", "Harrison Steen"),
            unsplashPhoto("photo-1690499957380-80a5a7568589", "BLgix9vWFco", "Brad Fickeisen"),
            unsplashPhoto("photo-1771962036583-20b41481f553", "6KrffCi1ceI", "Zihao Wang")
    };

    private static final Photo[] NATURE = {ALL_PHOTOS[0], ALL_PHOTOS[1], ALL_PHOTOS[2], ALL_PHOTOS[9]};
    private static final Photo[] MOUNTAINS = {ALL_PHOTOS[0], ALL_PHOTOS[3], ALL_PHOTOS[6], ALL_PHOTOS[10]};
    private static final Photo[] OCEAN = {ALL_PHOTOS[4], ALL_PHOTOS[8], ALL_PHOTOS[9], ALL_PHOTOS[11]};
    private static final Photo[] SPACE = {ALL_PHOTOS[2], ALL_PHOTOS[5], ALL_PHOTOS[7], ALL_PHOTOS[11]};
    private static final Photo[] CITIES = {ALL_PHOTOS[3], ALL_PHOTOS[6], ALL_PHOTOS[8], ALL_PHOTOS[10]};
    private static final Photo[] ABSTRACT = {ALL_PHOTOS[1], ALL_PHOTOS[5], ALL_PHOTOS[7], ALL_PHOTOS[11]};
    private static final Photo[] AMOLED = {ALL_PHOTOS[2], ALL_PHOTOS[5], ALL_PHOTOS[7], ALL_PHOTOS[10]};
    private static final Photo[] GAMING = {ALL_PHOTOS[3], ALL_PHOTOS[5], ALL_PHOTOS[6], ALL_PHOTOS[8]};
    private static final Photo[] MINIMAL = {ALL_PHOTOS[1], ALL_PHOTOS[4], ALL_PHOTOS[9], ALL_PHOTOS[11]};

    private static final AtomicInteger NEXT_INDEX =
            new AtomicInteger((int) (System.currentTimeMillis() % ALL_PHOTOS.length));

    private HomeBackgroundProvider() {
    }

    private static Photo unsplashPhoto(String imageId, String photoId, String photographer) {
        return new Photo(
                photoId,
                "https://images.unsplash.com/" + imageId + IMAGE_PARAMS,
                "https://unsplash.com/photos/" + photoId + UNSPLASH_REF,
                photographer);
    }

    public static Photo nextPhoto() {
        int index = Math.floorMod(NEXT_INDEX.getAndIncrement(), ALL_PHOTOS.length);
        return ALL_PHOTOS[index];
    }

    public static Photo photoForSeed(String seed) {
        return photoForSeed(ALL_PHOTOS, seed);
    }

    public static Photo photoForDailyMode(String mode, String collection, int dayBucket) {
        if ("collection".equals(mode)) {
            return photoForSeed(collectionPhotos(collection), collection + ":" + dayBucket);
        }
        return photoForSeed(ALL_PHOTOS, "auto:" + dayBucket);
    }

    public static Photo photoForDailyMode(String mode, String collection, int dayBucket,
                                          Set<String> preferredPhotoIds) {
        Photo[] preferred = photosForIds(preferredPhotoIds);
        if (preferred.length > 0) {
            return photoForSeed(preferred, "favorite:" + mode + ":" + collection + ":" + dayBucket);
        }
        return photoForDailyMode(mode, collection, dayBucket);
    }

    public static List<Photo> photosForMode(String mode, String collection,
                                            Set<String> preferredPhotoIds) {
        Photo[] preferred = photosForIds(preferredPhotoIds);
        if (preferred.length > 0) {
            return photosAsList(preferred);
        }
        if ("collection".equals(mode)) {
            return photosAsList(collectionPhotos(collection));
        }
        return photosAsList(ALL_PHOTOS);
    }

    public static List<Photo> allPhotos() {
        return photosAsList(ALL_PHOTOS);
    }

    public static Photo photoById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        for (Photo photo : ALL_PHOTOS) {
            if (id.equals(photo.getId())) {
                return photo;
            }
        }
        return null;
    }

    private static Photo photoForSeed(Photo[] photos, String seed) {
        if (photos == null || photos.length == 0) {
            return nextPhoto();
        }
        if (seed == null || seed.trim().isEmpty()) {
            return nextPhoto();
        }
        int index = Math.floorMod(seed.hashCode(), photos.length);
        return photos[index];
    }

    private static Photo[] collectionPhotos(String collection) {
        if ("mountains".equals(collection)) {
            return MOUNTAINS;
        }
        if ("ocean".equals(collection)) {
            return OCEAN;
        }
        if ("space".equals(collection)) {
            return SPACE;
        }
        if ("cities".equals(collection)) {
            return CITIES;
        }
        if ("abstract".equals(collection)) {
            return ABSTRACT;
        }
        if ("amoled".equals(collection)) {
            return AMOLED;
        }
        if ("gaming".equals(collection)) {
            return GAMING;
        }
        if ("minimal".equals(collection)) {
            return MINIMAL;
        }
        return NATURE;
    }

    private static Photo[] photosForIds(Set<String> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return new Photo[0];
        }
        List<Photo> photos = new ArrayList<>();
        for (Photo photo : ALL_PHOTOS) {
            if (photoIds.contains(photo.getId())) {
                photos.add(photo);
            }
        }
        return photos.toArray(new Photo[0]);
    }

    private static List<Photo> photosAsList(Photo[] photos) {
        if (photos == null || photos.length == 0) {
            return Collections.emptyList();
        }
        List<Photo> list = new ArrayList<>(photos.length);
        Collections.addAll(list, photos);
        return list;
    }

    public static final class Photo {
        private final String id;
        private final String imageUrl;
        private final String sourceUrl;
        private final String photographer;

        Photo(String id, String imageUrl, String sourceUrl, String photographer) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.sourceUrl = sourceUrl;
            this.photographer = photographer;
        }

        public String getId() {
            return id;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public String getPhotographer() {
            return photographer;
        }

        public String getCreditText() {
            return "Photo by " + photographer;
        }
    }
}

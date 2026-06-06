package com.webstudio.easybrowser.utils;

import java.util.concurrent.atomic.AtomicInteger;

public final class HomeBackgroundProvider {
    private static final String IMAGE_PARAMS = "?auto=format&fit=crop&crop=entropy&w=1440&h=2560&q=86";
    private static final String UNSPLASH_REF = "?utm_source=easy_browser&utm_medium=referral";

    private static final Photo[] PHOTOS = {
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

    private static final AtomicInteger NEXT_INDEX =
            new AtomicInteger((int) (System.currentTimeMillis() % PHOTOS.length));

    private HomeBackgroundProvider() {
    }

    private static Photo unsplashPhoto(String imageId, String photoId, String photographer) {
        return new Photo(
                "https://images.unsplash.com/" + imageId + IMAGE_PARAMS,
                "https://unsplash.com/photos/" + photoId + UNSPLASH_REF,
                photographer);
    }

    public static Photo nextPhoto() {
        int index = Math.floorMod(NEXT_INDEX.getAndIncrement(), PHOTOS.length);
        return PHOTOS[index];
    }

    public static Photo photoForSeed(String seed) {
        if (seed == null || seed.trim().isEmpty()) {
            return nextPhoto();
        }
        int index = Math.floorMod(seed.hashCode(), PHOTOS.length);
        return PHOTOS[index];
    }

    public static final class Photo {
        private final String imageUrl;
        private final String sourceUrl;
        private final String photographer;

        Photo(String imageUrl, String sourceUrl, String photographer) {
            this.imageUrl = imageUrl;
            this.sourceUrl = sourceUrl;
            this.photographer = photographer;
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

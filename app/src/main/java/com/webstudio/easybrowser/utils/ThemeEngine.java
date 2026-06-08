package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;

public final class ThemeEngine {
    private ThemeEngine() {
    }

    public static Palette homePalette(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String preset = prefs.getString(SettingsKeys.PREF_THEME_COLOR_PRESET, "blue");
        String pack = prefs.getString(SettingsKeys.PREF_THEME_PACK, "default");
        boolean wallpaperSync = prefs.getBoolean(SettingsKeys.PREF_THEME_WALLPAPER_SYNC, false);
        Palette palette = wallpaperSync ? wallpaperPalette(prefs) : presetPalette(preset);
        return applyPack(palette, pack, wallpaperSync);
    }

    public static int settingsChromeColor(Context context) {
        return homePalette(context).accentSoft;
    }

    public static int homeChromeColor(Context context) {
        return homePalette(context).panelStrongBackground;
    }

    public static int foregroundFor(int background) {
        return useDarkSystemBarIcons(background) ? 0xFF101820 : Color.WHITE;
    }

    public static boolean useDarkSystemBarIcons(int background) {
        double luminance = (0.299 * Color.red(background)
                + 0.587 * Color.green(background)
                + 0.114 * Color.blue(background)) / 255.0;
        return luminance > 0.58;
    }

    public static ColorStateList switchThumbTint(Context context) {
        Palette palette = homePalette(context);
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        palette.accent,
                        0xFFEEF2F4
                });
    }

    public static ColorStateList switchTrackTint(Context context) {
        Palette palette = homePalette(context);
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        blend(palette.accent, Color.WHITE, 0.58f),
                        0xFFB8C3CA
                });
    }

    public static void applyChrome(AppCompatActivity activity, Toolbar toolbar) {
        if (activity == null) {
            return;
        }
        int chrome = settingsChromeColor(activity);
        SystemBarUtils.apply(activity,
                chrome,
                chrome,
                useDarkSystemBarIcons(chrome));
        if (toolbar == null) {
            return;
        }
        int foreground = foregroundFor(chrome);
        toolbar.setBackgroundColor(chrome);
        toolbar.setTitleTextColor(foreground);
        tintToolbarDrawable(toolbar.getNavigationIcon(), foreground,
                toolbar::setNavigationIcon);
        Menu menu = toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            if (icon != null) {
                Drawable tinted = DrawableCompat.wrap(icon.mutate());
                DrawableCompat.setTint(tinted, foreground);
                menu.getItem(i).setIcon(tinted);
            }
        }
    }

    private interface DrawableTarget {
        void set(Drawable drawable);
    }

    private static void tintToolbarDrawable(Drawable drawable, int color, DrawableTarget target) {
        if (drawable == null || target == null) {
            return;
        }
        Drawable tinted = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(tinted, color);
        target.set(tinted);
    }

    private static Palette presetPalette(String preset) {
        if ("green".equals(preset)) {
            return new Palette(0xFF35B88F, 0xFFDDF8EE, 0xFF122C28);
        }
        if ("purple".equals(preset)) {
            return new Palette(0xFF8B7CF6, 0xFFE8E3FF, 0xFF221E3D);
        }
        if ("orange".equals(preset)) {
            return new Palette(0xFFE96B3C, 0xFFFFE4D6, 0xFF351C12);
        }
        if ("red".equals(preset)) {
            return new Palette(0xFFE05564, 0xFFFFE1E5, 0xFF36161B);
        }
        if ("amoled_black".equals(preset)) {
            return new Palette(0xFF82D8FF, 0xFF1B2730, 0xFF050607);
        }
        return new Palette(0xFF3B82B8, 0xFFDCEBFA, 0xFF101820);
    }

    private static Palette wallpaperPalette(SharedPreferences prefs) {
        String mode = prefs.getString(SettingsKeys.PREF_WALLPAPER_MODE, "auto");
        if ("user".equals(mode)) {
            String uri = prefs.getString(SettingsKeys.PREF_WALLPAPER_USER_URI, "user");
            return paletteFromAccent(colorFromSeed(uri));
        }
        if ("collection".equals(mode)) {
            return paletteFromAccent(collectionAccent(
                    prefs.getString(SettingsKeys.PREF_WALLPAPER_COLLECTION, "nature")));
        }
        int dayBucket = (int) (System.currentTimeMillis() / 86_400_000L);
        int[] autoAccents = {
                0xFF3B82B8,
                0xFF4DAA89,
                0xFFE0A84D,
                0xFF8B7CF6,
                0xFF2F9AC6,
                0xFFE05B7A
        };
        return paletteFromAccent(autoAccents[Math.floorMod(dayBucket, autoAccents.length)]);
    }

    private static int collectionAccent(String collection) {
        if ("mountains".equals(collection)) {
            return 0xFF6E8FA3;
        }
        if ("ocean".equals(collection)) {
            return 0xFF2F9AC6;
        }
        if ("space".equals(collection)) {
            return 0xFF8B7CF6;
        }
        if ("cities".equals(collection)) {
            return 0xFFE0A84D;
        }
        if ("abstract".equals(collection)) {
            return 0xFFE85EA8;
        }
        if ("amoled".equals(collection)) {
            return 0xFF82D8FF;
        }
        if ("gaming".equals(collection)) {
            return 0xFF52E0A3;
        }
        if ("minimal".equals(collection)) {
            return 0xFF9AA8AC;
        }
        return 0xFF4DAA89;
    }

    private static int colorFromSeed(String seed) {
        int hue = Math.floorMod(seed != null ? seed.hashCode() : 0, 360);
        return Color.HSVToColor(new float[]{hue, 0.58f, 0.82f});
    }

    private static Palette paletteFromAccent(int accent) {
        return new Palette(accent, soften(accent), blend(accent, Color.BLACK, 0.76f));
    }

    private static Palette applyPack(Palette base, String pack, boolean preserveAccent) {
        if ("amoled".equals(pack)) {
            return base.withSurfaces(0xF2050607, 0xFF101418, 0xFF1C2730,
                    0xFFFFFFFF, 0xFF7E8B96);
        }
        if ("glass".equals(pack) || "material_you".equals(pack)) {
            return base.withSurfaces(0x8F101820, 0xC4101820, 0x40FFFFFF,
                    0xFFFFFFFF, 0xDCE6EEF4);
        }
        if ("nature".equals(pack)) {
            Palette themed = preserveAccent ? base : base.withAccent(0xFF4DAA89);
            return themed.withSurfaces(0x8A102019,
                    0xC4102019, 0x45C7F2DD, 0xFFFFFFFF, 0xDDEAF6EC);
        }
        if ("space".equals(pack)) {
            return base.withSurfaces(0xD70B1022, 0xF0101628, 0x4497B8FF,
                    0xFFFFFFFF, 0xDADCE8FF);
        }
        if ("gaming".equals(pack)) {
            Palette themed = preserveAccent ? base : base.withAccent(0xFF52E0A3);
            return themed.withSurfaces(0xD70A1115,
                    0xF00E171C, 0x4452E0A3, 0xFFFFFFFF, 0xD7DDF7EA);
        }
        if ("cyberpunk".equals(pack)) {
            Palette themed = preserveAccent ? base : base.withAccent(0xFFFFD447);
            return themed.withSurfaces(0xD7111022,
                    0xF01B1530, 0x55FF4FD8, 0xFFFFFFFF, 0xDFEDE3FF);
        }
        return base;
    }

    public static final class Palette {
        public final int accent;
        public final int accentSoft;
        public final int searchBackground;
        public final int searchIconBackground;
        public final int panelBackground;
        public final int panelStrongBackground;
        public final int panelBorder;
        public final int onSurface;
        public final int onSurfaceMuted;

        private Palette(int accent, int accentSoft, int darkSurface) {
            this(accent, accentSoft, darkSurface, adjust(darkSurface, 0.62f),
                    0x8F101820, 0xC4101820, 0x40FFFFFF,
                    Color.WHITE, 0xDDE6EEF4);
        }

        private Palette(int accent, int accentSoft, int searchBackground,
                        int searchIconBackground, int panelBackground,
                        int panelStrongBackground, int panelBorder,
                        int onSurface, int onSurfaceMuted) {
            this.accent = accent;
            this.accentSoft = accentSoft;
            this.searchBackground = searchBackground;
            this.searchIconBackground = searchIconBackground;
            this.panelBackground = panelBackground;
            this.panelStrongBackground = panelStrongBackground;
            this.panelBorder = panelBorder;
            this.onSurface = onSurface;
            this.onSurfaceMuted = onSurfaceMuted;
        }

        private Palette withAccent(int newAccent) {
            return new Palette(newAccent, soften(newAccent), searchBackground, searchIconBackground,
                    panelBackground, panelStrongBackground, panelBorder, onSurface, onSurfaceMuted);
        }

        private Palette withSurfaces(int panel, int strongPanel, int border,
                                     int text, int mutedText) {
            return new Palette(accent, accentSoft, searchBackground, searchIconBackground,
                    panel, strongPanel, border, text, mutedText);
        }
    }

    private static int adjust(int color, float factor) {
        return Color.rgb(
                Math.round(Color.red(color) * factor),
                Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    private static int soften(int color) {
        return blend(color, Color.WHITE, 0.78f);
    }

    private static int blend(int color, int target, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        float source = 1f - clamped;
        return Color.rgb(
                Math.round(Color.red(color) * source + Color.red(target) * clamped),
                Math.round(Color.green(color) * source + Color.green(target) * clamped),
                Math.round(Color.blue(color) * source + Color.blue(target) * clamped));
    }
}

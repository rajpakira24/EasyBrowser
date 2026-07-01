package com.webstudio.easybrowser.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public final class EasyMotion {
    public static final long DURATION_SHORT = 160L;
    public static final long DURATION_MEDIUM = 260L;
    public static final long DURATION_LONG = 360L;
    public static final long STAGGER_SHORT = 55L;

    public static final TimeInterpolator STANDARD =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    public static final TimeInterpolator STANDARD_ACCELERATE =
            new PathInterpolator(0.3f, 0f, 1f, 1f);
    public static final TimeInterpolator EMPHASIZED =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    private EasyMotion() {
    }

    public static void configurePremiumItemAnimator(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(DURATION_MEDIUM);
        itemAnimator.setRemoveDuration(190L);
        itemAnimator.setMoveDuration(300L);
        itemAnimator.setChangeDuration(190L);
        itemAnimator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(itemAnimator);
    }

    public static Transition premiumLayoutTransition() {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(DURATION_MEDIUM);
        transition.setInterpolator(STANDARD);
        return transition;
    }

    public static void prepareFadeSlide(View view, int translationY) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(translationY);
        view.setScaleX(0.985f);
        view.setScaleY(0.985f);
    }

    public static void fadeSlideIn(View view, long delayMs, long durationMs) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        view.animate().cancel();
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(durationMs)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static void animateHomepageFade(View view, long delayMs, int translationY) {
        prepareFadeSlide(view, translationY);
        fadeSlideIn(view, delayMs, DURATION_LONG);
    }

    public static void animateTabOpen(View surface) {
        if (surface == null) {
            return;
        }
        surface.animate().cancel();
        surface.setAlpha(0.88f);
        surface.setScaleX(0.992f);
        surface.setScaleY(0.992f);
        surface.setTranslationY(dp(surface.getContext(), 8));
        surface.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static void animateTabCloseThenOpen(View surface, @Nullable Runnable endAction) {
        if (surface == null) {
            run(endAction);
            return;
        }
        surface.animate().cancel();
        surface.animate()
                .alpha(0.86f)
                .scaleX(0.985f)
                .scaleY(0.985f)
                .translationY(dp(surface.getContext(), 10))
                .setDuration(120L)
                .setInterpolator(STANDARD_ACCELERATE)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        surface.animate().setListener(null);
                        run(endAction);
                        animateTabOpen(surface);
                    }
                })
                .start();
    }

    public static void animateTabChipOpen(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.setScaleX(0.86f);
        view.setScaleY(0.86f);
        view.setTranslationY(dp(view.getContext(), 8));
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(240L)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static void animateDismiss(View view, @Nullable Runnable endAction) {
        if (view == null) {
            run(endAction);
            return;
        }
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .translationY(dp(view.getContext(), 10))
                .setDuration(DURATION_SHORT)
                .setInterpolator(STANDARD_ACCELERATE)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate().setListener(null);
                        view.setAlpha(1f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                        view.setTranslationY(0f);
                        run(endAction);
                    }
                })
                .start();
    }

    public static void animateGroupCreation(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.setTranslationY(dp(view.getContext(), 18));
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(320L)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static void animateBottomBarSelection(View bottomBar, int selectedItemId) {
        if (bottomBar == null) {
            return;
        }
        View selected = bottomBar.findViewById(selectedItemId);
        if (selected == null) {
            return;
        }
        selected.animate().cancel();
        selected.setScaleX(0.94f);
        selected.setScaleY(0.94f);
        selected.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240L)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static void animateBottomBarVisibility(View bottomBar, float targetTranslationY,
                                                  boolean show, boolean animated) {
        if (bottomBar == null) {
            return;
        }
        bottomBar.animate().cancel();
        if (!animated) {
            bottomBar.setAlpha(show ? 1f : 0.9f);
            bottomBar.setTranslationY(targetTranslationY);
            return;
        }
        bottomBar.animate()
                .alpha(show ? 1f : 0.88f)
                .translationY(targetTranslationY)
                .setDuration(show ? DURATION_MEDIUM : 190L)
                .setInterpolator(show ? EMPHASIZED : STANDARD_ACCELERATE)
                .start();
    }

    /**
     * Fly {@code view} toward the centre of {@code target} while shrinking and fading it out —
     * used for the download-start chip collapsing into the toolbar/downloads button.
     */
    public static void animateViewIntoTarget(View view, View target, @Nullable Runnable endAction) {
        if (view == null || target == null) {
            run(endAction);
            return;
        }
        int[] viewLoc = new int[2];
        int[] targetLoc = new int[2];
        view.getLocationOnScreen(viewLoc);
        target.getLocationOnScreen(targetLoc);
        float dx = (targetLoc[0] + target.getWidth() / 2f) - (viewLoc[0] + view.getWidth() / 2f);
        float dy = (targetLoc[1] + target.getHeight() / 2f) - (viewLoc[1] + view.getHeight() / 2f);
        view.animate().cancel();
        view.setPivotX(view.getWidth() / 2f);
        view.setPivotY(view.getHeight() / 2f);
        view.animate()
                .translationXBy(dx)
                .translationYBy(dy)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .alpha(0f)
                .setDuration(DURATION_LONG)
                .setInterpolator(STANDARD_ACCELERATE)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate().setListener(null);
                        run(endAction);
                    }
                })
                .start();
    }

    public static void pulse(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240L)
                .setInterpolator(EMPHASIZED)
                .start();
    }

    public static int dp(Context context, int value) {
        if (context == null) {
            return value;
        }
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void run(@Nullable Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}

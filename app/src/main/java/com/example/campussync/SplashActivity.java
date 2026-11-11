package com.example.campussync;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Animated launch screen.
 *
 * Timeline (ms):
 *   0    – white tile pops in (scale 0 → 1 with overshoot)
 *   150  – sync arrows spin in (rotate -270° → 0°, fade + scale)
 *   700  – soft ripple pulses out from behind the logo
 *   650  – "Kaleo" slides up, tagline follows
 *   1300 – arrows do one more quick "sync" spin
 *   2200 – everything zooms out & fades, then MainActivity opens
 */
public class SplashActivity extends Activity {

    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View content = findViewById(R.id.splashContent);
        View tile = findViewById(R.id.splashTile);
        View ring = findViewById(R.id.splashRing);
        View pulse = findViewById(R.id.splashPulse);
        View title = findViewById(R.id.splashTitle);
        View tagline = findViewById(R.id.splashTagline);

        // 1. Tile pop
        tile.setScaleX(0f);
        tile.setScaleY(0f);
        ObjectAnimator tilePop = ObjectAnimator.ofPropertyValuesHolder(tile,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f));
        tilePop.setDuration(600);
        tilePop.setInterpolator(new OvershootInterpolator(1.6f));

        // 2. Ring spin-in
        ObjectAnimator ringIn = ObjectAnimator.ofPropertyValuesHolder(ring,
                PropertyValuesHolder.ofFloat(View.ROTATION, -270f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.4f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.4f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f));
        ringIn.setDuration(900);
        ringIn.setStartDelay(150);
        ringIn.setInterpolator(new DecelerateInterpolator(2f));

        // 3. Ripple pulse
        ObjectAnimator ripple = ObjectAnimator.ofPropertyValuesHolder(pulse,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 2.4f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 2.4f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f));
        ripple.setDuration(1100);
        ripple.setStartDelay(700);
        ripple.setInterpolator(new DecelerateInterpolator());

        // 4. Text slide-up
        ObjectAnimator titleIn = slideUp(title, 650);
        ObjectAnimator tagIn = slideUp(tagline, 820);

        // 5. Second "sync" spin
        ObjectAnimator spin = ObjectAnimator.ofFloat(ring, View.ROTATION, 0f, 360f);
        spin.setDuration(700);
        spin.setStartDelay(1300);
        spin.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        AnimatorSet intro = new AnimatorSet();
        intro.playTogether(tilePop, ringIn, ripple, titleIn, tagIn, spin);
        intro.start();

        // 6. Exit
        content.postDelayed(() -> {
            ObjectAnimator exit = ObjectAnimator.ofPropertyValuesHolder(content,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f));
            exit.setDuration(350);
            exit.setInterpolator(new AccelerateInterpolator());
            exit.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    openMain();
                }
            });
            exit.start();
        }, 2200);

        // Tap to skip
        findViewById(R.id.splashRoot).setOnClickListener(v -> openMain());
    }

    private ObjectAnimator slideUp(View v, long delay) {
        float shift = 28 * getResources().getDisplayMetrics().density;
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, shift, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f));
        a.setDuration(550);
        a.setStartDelay(delay);
        a.setInterpolator(new DecelerateInterpolator(1.8f));
        return a;
    }

    private void openMain() {
        if (finished) return;
        finished = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}

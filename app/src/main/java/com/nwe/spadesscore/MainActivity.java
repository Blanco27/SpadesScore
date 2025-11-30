package com.nwe.spadesscore;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends SpadesAppCompatActivity {
    private final SpadesGame spadesGame = SpadesGame.getInstance();
    private Button btn3Players, btn4Players, btnLanguageEnglish, btnLanguageGerman, start_game_button;

    int COLOR_ACTIVE, COLOR_DEACTIVE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (spadesGame.getLanguages() == null) {
            spadesGame.setLanguages(Languages.ENGLISH);
            setLocale("en");
        }
    }

    @Override
    void initializeUIComponents() {
        btn3Players = findViewById(R.id.btn3Players);
        btn4Players = findViewById(R.id.btn4Players);
        btnLanguageEnglish = findViewById(R.id.btnLanguageEnglish);
        btnLanguageGerman = findViewById(R.id.btnLanguageGerman);
        start_game_button = findViewById(R.id.start_game_button);
    }

    @Override
    void setupUI() {
        if (spadesGame.getPlayerCount() == 4) {
            btn4Players.setSelected(true);
            btn3Players.setSelected(false);

            GradientDrawable bg4 = (GradientDrawable) btn4Players.getBackground();
            bg4.setColor(COLOR_ACTIVE);

            GradientDrawable bg3 = (GradientDrawable) btn3Players.getBackground();
            bg3.setColor(COLOR_DEACTIVE);
        } else {
            btn4Players.setSelected(false);
            btn3Players.setSelected(true);

            GradientDrawable bg4 = (GradientDrawable) btn4Players.getBackground();
            bg4.setColor(COLOR_DEACTIVE);

            GradientDrawable bg3 = (GradientDrawable) btn3Players.getBackground();
            bg3.setColor(COLOR_ACTIVE);
        }

        if (spadesGame.getLanguages() == Languages.ENGLISH) {
            btnLanguageEnglish.setSelected(true);
            btnLanguageGerman.setSelected(false);

            GradientDrawable backgroundActive = (GradientDrawable) btnLanguageEnglish.getBackground();
            backgroundActive.setColor(COLOR_ACTIVE);

            GradientDrawable backgroundDeactive = (GradientDrawable) btnLanguageGerman.getBackground();
            backgroundDeactive.setColor(COLOR_DEACTIVE);
        } else {
            btnLanguageEnglish.setSelected(false);
            btnLanguageGerman.setSelected(true);

            GradientDrawable backgroundActive = (GradientDrawable) btnLanguageEnglish.getBackground();
            backgroundActive.setColor(COLOR_DEACTIVE);

            GradientDrawable backgroundDeactive = (GradientDrawable) btnLanguageGerman.getBackground();
            backgroundDeactive.setColor(COLOR_ACTIVE);
        }

        btn3Players.setOnClickListener(v -> {
            if (spadesGame.getPlayerCount() == 3) {
                return;
            }
            btn3Players.setSelected(true);
            btn4Players.setSelected(false);
            spadesGame.setPlayerCount(3);

            select(btn3Players);
            deselect(btn4Players);
        });
        btn4Players.setOnClickListener(v -> {
            if (spadesGame.getPlayerCount() == 4) {
                return;
            }
            btn4Players.setSelected(true);
            btn3Players.setSelected(false);
            spadesGame.setPlayerCount(4);

            select(btn4Players);
            deselect(btn3Players);
        });
        btnLanguageEnglish.setOnClickListener(v -> {
            if (spadesGame.getLanguages() == Languages.ENGLISH) {
                return;
            }
            btnLanguageEnglish.setSelected(true);
            btnLanguageGerman.setSelected(false);
            spadesGame.setLanguages(Languages.ENGLISH);

            select(btnLanguageEnglish);
            deselect(btnLanguageGerman);
            setLocale("en");
        });
        btnLanguageGerman.setOnClickListener(v -> {
            if (spadesGame.getLanguages() == Languages.GERMAN) {
                return;
            }
            btnLanguageGerman.setSelected(true);
            btnLanguageEnglish.setSelected(false);
            spadesGame.setLanguages(Languages.GERMAN);

            select(btnLanguageGerman);
            deselect(btnLanguageEnglish);
            setLocale("de");
        });
        start_game_button.setOnClickListener(v -> switchToPlayerNamesAction());
    }

    private void select(Button b) {
        animateFill(b, COLOR_DEACTIVE, COLOR_ACTIVE);
    }

    private void deselect(Button b) {
        animateFill(b, COLOR_ACTIVE, COLOR_DEACTIVE);
    }

    private void animateFill(final View view, int fromColor, int toColor) {
        ValueAnimator anim = ValueAnimator.ofArgb(fromColor, toColor);
        anim.setDuration(250);

        anim.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            GradientDrawable bg = (GradientDrawable) view.getBackground();
            bg.setColor(color);
        });

        anim.start();
    }

    private void setLocale(String languageCode) {
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        recreate();
    }

    @Override
    void initContentView() {
        setContentView(R.layout.activity_main);
        COLOR_ACTIVE = ContextCompat.getColor(this, R.color.background_button_enabled);
        COLOR_DEACTIVE = ContextCompat.getColor(this, R.color.background_button_disabled);
    }

    private void switchToPlayerNamesAction() {
        Intent switchToPlayerNamesActivity = new Intent(this, PlayerNamesActivity.class);
        startActivity(switchToPlayerNamesActivity);
    }
}
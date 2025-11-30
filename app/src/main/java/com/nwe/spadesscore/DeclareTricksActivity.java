package com.nwe.spadesscore;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;

/**
 * The DeclareTricksActivity class is responsible for managing the user interface
 * where players declare their trick predictions for the current round of the Spades game.
 * <p>
 * This activity:
 * <ul>
 * <li>Displays the current round number and the scores of all players.
 * <li>Allows players to select their trick predictions using dropdown menus (spinners).
 * <li>Updates the game state with the declared trick predictions.
 * <li>Navigates to the ConfirmTicksActivity to confirm the predictions.
 * <li>Disables the back button to prevent unintended navigation.
 * </ul>
 * <p>
 * The class interacts with the SpadesGame singleton to retrieve and update game data.
 */
public class DeclareTricksActivity extends SpadesAppCompatActivity {

    private final SpadesGame spadesGame = SpadesGame.getInstance();

    int COLOR_ACTIVE, COLOR_DEACTIVE, defaultColor, WARNING_COLOR;
    private TextView round_TextView, combined_tricks_textView,
            current_score_player1_text_view, current_score_player2_text_view,
            current_score_player3_text_view, current_score_player4_text_view,
            player1_name_text_view, player2_name_text_view,
            player3_name_text_view, player4_name_text_view;

    private TrickSpinner spinner1, spinner2, spinner3, spinner4;
    private ProgressBar progressBar;
    private Button start_Button;

    private boolean lastTricksAreValid = true; // Status zwischenspeichern

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initializeUIComponents() {
        round_TextView = findViewById(R.id.round_TextView);
        progressBar = findViewById(R.id.progressBar);
        combined_tricks_textView = findViewById(R.id.combined_tricks_textView);
        current_score_player1_text_view = findViewById(R.id.current_score_player1_text_view);
        current_score_player2_text_view = findViewById(R.id.current_score_player2_text_view);
        current_score_player3_text_view = findViewById(R.id.current_score_player3_text_view);
        current_score_player4_text_view = findViewById(R.id.current_score_player4_text_view);
        player1_name_text_view = findViewById(R.id.player1_name_text_view);
        player2_name_text_view = findViewById(R.id.player2_name_text_view);
        player3_name_text_view = findViewById(R.id.player3_name_text_view);
        player4_name_text_view = findViewById(R.id.player4_name_text_view);
        spinner1 = findViewById(R.id.spinner1);
        spinner2 = findViewById(R.id.spinner2);
        spinner3 = findViewById(R.id.spinner3);
        spinner4 = findViewById(R.id.spinner4);
        start_Button = findViewById(R.id.start_Button);

        spinner1.setMaxAmount(spadesGame.getAmountOfCards());
        spinner2.setMaxAmount(spadesGame.getAmountOfCards());
        spinner3.setMaxAmount(spadesGame.getAmountOfCards());
        spinner4.setMaxAmount(spadesGame.getAmountOfCards());
    }

    @Override
    protected void setupUI() {
        if (spadesGame.getPlayerCount() == 3) {
            current_score_player4_text_view.setVisibility(View.GONE);
            findViewById(R.id.spinner4).setVisibility(View.GONE);
            findViewById(R.id.player4_layout).setVisibility(View.GONE);
        }

        progressBar.setMax(spadesGame.getAmountOfRounds());
        progressBar.setProgress(spadesGame.getCurrentRound());

        GradientDrawable buttonBackground = (GradientDrawable) start_Button.getBackground();
        buttonBackground.setColor(COLOR_ACTIVE);

        defaultColor = combined_tricks_textView.getTextColors().getDefaultColor();
        start_Button.setOnClickListener(v -> confirmTicks());

        // Listener für Spinner-Wertänderung
        spinner1.setOnValueChangedListener(this::updateCombinedTricksTextView);
        spinner2.setOnValueChangedListener(this::updateCombinedTricksTextView);
        spinner3.setOnValueChangedListener(this::updateCombinedTricksTextView);
        spinner4.setOnValueChangedListener(this::updateCombinedTricksTextView);

        updateView();
    }

    @Override
    void initContentView() {
        setContentView(R.layout.activity_declare_tricks);
        COLOR_ACTIVE = ContextCompat.getColor(this, R.color.background_button_enabled);
        COLOR_DEACTIVE = ContextCompat.getColor(this, R.color.background_button_disabled);
        WARNING_COLOR = ContextCompat.getColor(this, R.color.warningText);
    }

    public void updateView() {
        round_TextView.setText(spadesGame.getCurrentRoundString(this));
        updateCombinedTricksTextView();
        current_score_player1_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(0));
        current_score_player2_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(1));
        current_score_player3_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(2));
        player1_name_text_view.setText(spadesGame.getPlayerName(0));
        player2_name_text_view.setText(spadesGame.getPlayerName(1));
        player3_name_text_view.setText(spadesGame.getPlayerName(2));
        if (spadesGame.getPlayerCount() == 4) {
            current_score_player4_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(3));
            player4_name_text_view.setText(spadesGame.getPlayerName(3));
        }
    }

    private @NotNull Unit updateCombinedTricksTextView() {
        int tricks1 = spinner1.getValue();
        int tricks2 = spinner2.getValue();
        int tricks3 = spinner3.getValue();
        int tricks4 = spinner4.getValue();
        int combinedTricks = tricks1 + tricks2 + tricks3 + (spadesGame.getPlayerCount() == 4 ? tricks4 : 0);
        int possibleTricks = spadesGame.getAmountOfCards();

        combined_tricks_textView.setText(spadesGame.getCombinedTrickPredictionString(this, tricks1, tricks2, tricks3, tricks4));

        boolean isValid = combinedTricks != possibleTricks;

        if (!isValid) {
            combined_tricks_textView.setTextColor(WARNING_COLOR);
            start_Button.setEnabled(false);
            if (lastTricksAreValid) {
                animateFill(start_Button, COLOR_ACTIVE, COLOR_DEACTIVE);
                shakeView(start_Button); // Shake-Animation beim Deaktivieren
            }
        } else {
            combined_tricks_textView.setTextColor(defaultColor);
            start_Button.setEnabled(true);
            if (!lastTricksAreValid) {
                animateFill(start_Button, COLOR_DEACTIVE, COLOR_ACTIVE);
            }
        }
        lastTricksAreValid = isValid;
        return null;
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

    private void shakeView(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0, 16, -16, 12, -12, 6, -6, 0);
        shake.setDuration(350);
        shake.start();
    }

    public void confirmTicks() {
        spadesGame.setTickPredictions(
                spinner1.getValue(),
                spinner2.getValue(),
                spinner3.getValue(),
                spinner4.getValue()
        );
        switchToConfirmTicksActivity();
    }

    public void switchToConfirmTicksActivity() {
        Intent switchToConfirmTicksActivity = new Intent(this, ConfirmTicksActivity.class);
        startActivity(switchToConfirmTicksActivity);
    }
}
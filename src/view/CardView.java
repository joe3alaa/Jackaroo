// PASTE THIS ENTIRE CODE INTO your CardView.java file.

package view;

import java.util.function.Consumer;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import model.card.Card;
import model.card.standard.Standard;
import model.card.standard.Suit;

public class CardView extends BorderPane {

    private final Card card;
    private final boolean isSelected;
    
    // Refined, more realistic shadow effects for a premium feel.
    private final DropShadow defaultCardShadow = new DropShadow(12, 3, 3, Color.rgb(0, 0, 0, 0.18));
    private final DropShadow hoverCardEffect = new DropShadow(20, Color.rgb(220, 220, 180, 0.75));
    private final DropShadow selectedCardEffect = new DropShadow(28, Color.GOLD.deriveColor(0, 1.2, 1.5, 1));

    public CardView(Card card) {
        this(card, false, false, null);
    }

    // PASTE and REPLACE the ENTIRE specified constructor in CardView.java

    public CardView(Card card, boolean isPlayable, boolean isSelected, Consumer<Card> onCardClicked) {
        this.card = card;
        this.isSelected = isSelected;

        // --- DYNAMIC CARD DIMENSIONS (REDUCED) ---
        double cardWidth = GUI.CELL_SIZE * 1.4;  // WAS 1.5
        double cardHeight = GUI.CELL_SIZE * 1.9; // WAS 2.1
        setPrefSize(cardWidth, cardHeight);
        setMaxSize(cardWidth, cardHeight);
        setMinSize(cardWidth, cardHeight);
        setPadding(new Insets(6)); // WAS 8

        // --- Render Card Face ---
        if (card instanceof Standard) {
            Standard standardCard = (Standard) card;
            int rank = standardCard.getRank();
            Suit suit = standardCard.getSuit();
            String rankStr = getRankDisplay(rank);
            String suitSymbol = getSuitSymbol(suit);
            Color suitColor = getSuitColor(suit);

            // Typography: Using a modern, clean font with bold weight for impact.
            double rankFontSize = cardHeight * 0.20;
            double suitFontSize = cardHeight * 0.13;
            double centralSymbolFontSize = cardHeight * 0.7;

            // Top-left corner display (Rank and Suit)
            VBox topLeft = new VBox(-cardHeight * 0.08); // Tight negative spacing
            Text rankTextTop = new Text(rankStr);
            rankTextTop.setFont(Font.font("Oswald", FontWeight.BLACK, rankFontSize)); // CHANGE HERE
            rankTextTop.setFill(suitColor);

            Text suitTextTop = new Text(suitSymbol);
            suitTextTop.setFont(Font.font("Roboto", FontWeight.NORMAL, suitFontSize)); // AND HERE
            suitTextTop.setFill(suitColor);

            topLeft.getChildren().addAll(rankTextTop, suitTextTop);
            BorderPane.setAlignment(topLeft, Pos.TOP_LEFT);
            setTop(topLeft);

            // Bottom-right corner display (rotated)
            VBox bottomRight = new VBox(-cardHeight * 0.08);
            Text rankTextBottom = new Text(rankStr);
            rankTextBottom.setFont(Font.font("Oswald", FontWeight.BLACK, rankFontSize));
            rankTextBottom.setFill(suitColor);

            Text suitTextBottom = new Text(suitSymbol);
            suitTextBottom.setFont(Font.font("Roboto", FontWeight.NORMAL, suitFontSize));
            suitTextBottom.setFill(suitColor);

            bottomRight.getChildren().addAll(rankTextBottom, suitTextBottom);
            bottomRight.setRotate(180);
            BorderPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);
            setBottom(bottomRight);

            // Large central symbol: A subtle, elegant watermark.
            Text centralSuitSymbol = new Text(suitSymbol);
            centralSuitSymbol.setFont(Font.font("Roboto", FontWeight.BOLD, centralSymbolFontSize));
            centralSuitSymbol.setFill(suitColor.deriveColor(0, 1, 1, 0.08)); // Very faint for subtlety
            setCenter(centralSuitSymbol);
            BorderPane.setAlignment(centralSuitSymbol, Pos.CENTER);

        } else { // <<< CRITICAL TEXT-WRAPPING FIX >>> For non-standard cards like Jokers
            // Using a Text node instead of a Label gives us proper word-boundary wrapping.
            Text cardNameText = new Text(card.getName());
            cardNameText.setFill(Color.rgb(40, 40, 40));

            // Dynamically adjust font size for long names to ensure they fit well.
            double fontSize = 16;
            if (card.getName().length() > 10) { // e.g., "Marble Burner" is 13 chars
                fontSize = 14;
            }
            cardNameText.setFont(Font.font("Oswald", FontWeight.BOLD, fontSize));

            // Set the wrapping width, which forces the text to wrap at word boundaries.
            cardNameText.setWrappingWidth(cardWidth * 0.85); // A bit wider to be safe
            cardNameText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            setCenter(cardNameText);
        }

        // --- STYLING ---
        getStyleClass().add("card-view");
        
        // Create the base inner shadow that every card has.
        InnerShadow innerShadow = new InnerShadow(10, 0, 0, Color.rgb(0, 0, 0, 0.1));

        if (isSelected) {
            getStyleClass().add("selected");
            // Chain the effects: The selectedCardEffect (DropShadow) is applied first.
            innerShadow.setInput(selectedCardEffect); 
        } else {
            // The defaultCardShadow (DropShadow) is applied first.
            innerShadow.setInput(defaultCardShadow);
        }

        // Set the combined, final effect on the card.
        setEffect(innerShadow);

        // --- Interactivity ---
        if (isPlayable) {
            setUserData(card);
            setCursor(Cursor.HAND);
            setOnMouseClicked((MouseEvent event) -> {
                if (onCardClicked != null) {
                    onCardClicked.accept(card);
                }
            });
            // A subtle "pop" effect on hover for a satisfying physical feel.
            // NOW we call the updated addHoverAnimation method with the correct parameters.
            addHoverAnimation(innerShadow, hoverCardEffect, defaultCardShadow);
        }
        
        // Create and style the tooltip using our CSS class
        Tooltip tooltip = new Tooltip(card.getDescription());
        tooltip.getStyleClass().add("game-tooltip");
        Tooltip.install(this, tooltip);
    }

    private void addHoverAnimation(InnerShadow baseEffect, DropShadow hover, DropShadow standard) {
        ScaleTransition stEnter = new ScaleTransition(Duration.millis(120), this);
        stEnter.setToX(1.04);
        stEnter.setToY(1.04);

        ScaleTransition stExit = new ScaleTransition(Duration.millis(120), this);
        stExit.setToX(1.0);
        stExit.setToY(1.0);

        setOnMouseEntered(e -> {
            if (!isSelected) {
                baseEffect.setInput(hover); // Set the hover DropShadow
                setEffect(baseEffect);
                stEnter.play();
            }
        });
        setOnMouseExited(e -> {
            if (!isSelected) {
                baseEffect.setInput(standard); // Revert to the standard DropShadow
                setEffect(baseEffect);
                stExit.play();
            }
        });
    }

    // --- Helper methods ---
    private String getRankDisplay(int rank) {
        switch (rank) {
            case 1:  return "A";
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            default: return String.valueOf(rank);
        }
    }

    private String getSuitSymbol(Suit suit) {
        switch (suit) {
            case HEART:   return "♥";
            case DIAMOND: return "♦";
            case CLUB:    return "♣";
            case SPADE:   return "♠";
            default:      return "";
        }
    }

    // A refined, richer color palette.
    private Color getSuitColor(Suit suit) {
        switch (suit) {
            case HEART: case DIAMOND: return Color.web("#C62828"); // A deep, classic red.
            case CLUB: case SPADE:    return Color.web("#263238"); // A sophisticated dark blue-gray.
            default: return Color.DARKSLATEGRAY;
        }
    }
}
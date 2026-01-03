// PASTE THIS ENTIRE CODE INTO your PlayerArea.java file.

package view;

import static view.GUI.CELL_SIZE;
import static view.GUI.MARBLE_RADIUS;

import java.util.Map;
import java.util.function.Consumer;

import engine.Game;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Colour;
import model.card.Card;
import model.player.CPU;
import model.player.Marble;
import model.player.Player;
import engine.board.Cell;
import engine.board.SafeZone;

public class PlayerArea {

    private final Player player;
    private final Pane rootPane; // This will be an HBox or VBox
    private final SafeZone playerSafeZone; 

    // UI components managed by this controller
    private final HBox cardDisplayArea;
    private final HBox homeMarblesBox;
    private final HBox safeZoneDisplayBox;

    public PlayerArea(Player player, Pos alignment, Game game) {
        this.player = player;
        this.playerSafeZone = game.getBoard().getSafeZones().stream()
                .filter(sz -> sz.getColour() == player.getColour())
                .findFirst()
                .orElse(null);

        // 1. Create the root pane based on alignment
        if (alignment == Pos.TOP_CENTER || alignment == Pos.BOTTOM_CENTER) {
            this.rootPane = new HBox(8);
            ((HBox)this.rootPane).setAlignment(Pos.CENTER);
            ((HBox)this.rootPane).setMinHeight(CELL_SIZE * 3);
            ((HBox)this.rootPane).setSpacing(15);
        } else {
            this.rootPane = new VBox(5);
            ((VBox)this.rootPane).setAlignment(Pos.CENTER);
            ((VBox)this.rootPane).setMinWidth(CELL_SIZE * 3);
        }
        
        this.rootPane.getStyleClass().add("player-area");
        this.rootPane.getStyleClass().add(player.getColour().toString().toLowerCase() + "-player-area");
        
        rootPane.setPadding(new Insets(10));
        
        Label nameLabel = new Label(player.getName() + " (" + player.getColour().toString() + ")");
        nameLabel.setFont(Font.font("Oswald", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.WHITE);

        cardDisplayArea = new HBox(10); // Spacing between cards
        cardDisplayArea.setAlignment(Pos.CENTER);
        cardDisplayArea.setMinHeight(CELL_SIZE * 2.5);
        cardDisplayArea.setPrefHeight(CELL_SIZE * 2.5);
        cardDisplayArea.setId("cardDisplayArea_" + player.getColour());

        homeMarblesBox = new HBox(4);
        homeMarblesBox.setAlignment(Pos.CENTER_LEFT);
        homeMarblesBox.getChildren().add(new Label("Home: ") {{ setTextFill(Color.LIGHTGRAY); setFont(Font.font("Roboto", 12)); }});
        homeMarblesBox.setId("homeMarblesBox_" + player.getColour());

        safeZoneDisplayBox = new HBox(-4);
        safeZoneDisplayBox.setAlignment(Pos.CENTER_LEFT);
        safeZoneDisplayBox.getChildren().add(new Label("Safe: ") {{ setTextFill(Color.LIGHTGRAY); setFont(Font.font("Roboto", 12)); }});
        safeZoneDisplayBox.setId("safeZoneBox_" + player.getColour());

        // 3. Arrange components within the root pane
        if (rootPane instanceof VBox) {
            // Vertical layout for Left/Right players
            HBox detailsContainer = new HBox(10, homeMarblesBox, safeZoneDisplayBox);
            detailsContainer.setAlignment(Pos.CENTER);
            rootPane.getChildren().addAll(nameLabel, cardDisplayArea, detailsContainer);
        } else {
            // Horizontal layout for Top/Bottom players
            VBox detailsContainer = new VBox(5, homeMarblesBox, safeZoneDisplayBox);
            detailsContainer.setAlignment(Pos.CENTER_LEFT);
            rootPane.getChildren().addAll(nameLabel, cardDisplayArea, detailsContainer);
        }
    }
    
    public Pane getPane() {
        return this.rootPane;
    }

    public Player getPlayer() {
        return this.player;
    }

    // <<< CHANGE: Simplified signature.
    public void update(Game game, Consumer<Card> cardClickHandler, Map<Cell, StackPane> cellToPaneMap) {
        updateHand(game, cardClickHandler);
        updateHomeMarbles();
        updateSafeZone(cellToPaneMap);
    }

    public void setHighlight(boolean isActive) {
        if (isActive) {
            if (!rootPane.getStyleClass().contains("active")) {
                rootPane.getStyleClass().add("active");
            }
        } else {
            rootPane.getStyleClass().remove("active");
        }
    }
    
    private void updateHand(Game game, Consumer<Card> cardClickHandler) {
        cardDisplayArea.getChildren().clear();
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());

        if (player instanceof CPU) {
            for (int i = 0; i < player.getHand().size(); i++) {
                StackPane cardBack = new StackPane();
                double cardWidth = CELL_SIZE * 1.4;
                double cardHeight = CELL_SIZE * 1.9;
                cardBack.setPrefSize(cardWidth, cardHeight);
                cardBack.setMaxSize(cardWidth, cardHeight);
                cardBack.setMinSize(cardWidth, cardHeight);
                cardBack.getStyleClass().add("cpu-card-back");
                cardBack.setEffect(new DropShadow(8, 2, 2, Color.rgb(0, 0, 0, 0.2)));
                cardDisplayArea.getChildren().add(cardBack);
            }
        } else {
            for (Card card : player.getHand()) {
                boolean isSelected = (card == player.getSelectedCard()); // Check this player's selection
                boolean isPlayable = (player == currentPlayer);
                
                CardView cardView = new CardView(card, isPlayable, isSelected, cardClickHandler);
                cardDisplayArea.getChildren().add(cardView);
            }
        }
    }

    private void updateHomeMarbles() {
        // Clear only the marble circles, keeping the "Home: " label.
        if (homeMarblesBox.getChildren().size() > 1) {
            homeMarblesBox.getChildren().remove(1, homeMarblesBox.getChildren().size());
        }
        for (Marble homeMarble : player.getMarbles()) {
            Circle marbleCircle = new Circle(MARBLE_RADIUS / 1.4, GUI.getFXColor(player.getColour()));
            marbleCircle.setStroke(GUI.getFXColor(player.getColour()).darker().darker());
            marbleCircle.setStrokeWidth(1.2);
            marbleCircle.setStyle("-fx-effect: dropshadow(gaussian, " + GUI.getFXColor(player.getColour()).darker().toString().replace("0x", "#") + ", 5, 0.2, 0, 1);");
            homeMarblesBox.getChildren().add(marbleCircle);
        }
    }

    // <<< CHANGE: No longer needs the 'Game' parameter, as the safe zone is stored locally.
    private void updateSafeZone(Map<Cell, StackPane> cellToPaneMap) {
        // Clear old safe cell views, keeping the "Safe: " label
        if (safeZoneDisplayBox.getChildren().size() > 1) {
            safeZoneDisplayBox.getChildren().remove(1, safeZoneDisplayBox.getChildren().size());
        }

        if (playerSafeZone != null) {
            for (Cell safeCell : this.playerSafeZone.getCells()) {
                StackPane cellPane = new StackPane();
                double cellSize = GUI.CELL_SIZE * 0.75;
                cellPane.setPrefSize(cellSize, cellSize);
                cellPane.setUserData(safeCell);

                // <<< CRITICAL STEP >>>
                // This adds the visual pane for this safe cell to the global map, making it
                // findable by the main GUI class for animations.
                cellToPaneMap.put(safeCell, cellPane);

                double marbleRadius = GUI.MARBLE_RADIUS / 1.4;

                if (safeCell.getMarble() != null) {
                    Circle marbleCircle = new Circle(marbleRadius, GUI.getFXColor(safeCell.getMarble().getColour()));
                    marbleCircle.setStroke(GUI.getFXColor(safeCell.getMarble().getColour()).darker());
                    marbleCircle.setStrokeWidth(1.2);
                    String shadowColor = GUI.getFXColor(safeCell.getMarble().getColour()).darker().toString().replace("0x", "#");
                    marbleCircle.setStyle("-fx-effect: dropshadow(gaussian, " + shadowColor + ", 5, 0.2, 0, 1);");
                    cellPane.getChildren().add(marbleCircle);
                } else {
                    Circle emptySlot = new Circle(marbleRadius, Color.TRANSPARENT);
                    emptySlot.setStroke(Color.rgb(100, 100, 100, 0.8));
                    emptySlot.setStrokeWidth(1.2);
                    cellPane.getChildren().add(emptySlot);
                }
                safeZoneDisplayBox.getChildren().add(cellPane);
            }
        }
    }
}
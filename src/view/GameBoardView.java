// PASTE THIS ENTIRE CODE INTO your GameBoardView.java file.

package view;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import engine.board.Cell;
import engine.board.CellType;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.player.Marble;

public class GameBoardView extends GridPane {

    private static final int BOARD_DIMENSION = 10;
    // <<< CHANGE: Final keyword removed as it's not strictly necessary. This map is shared and managed externally.
    private final Map<Cell, StackPane> cellToPaneMap;

    public GameBoardView(ArrayList<Cell> trackCells, Consumer<Cell> onCellClicked, Map<Cell, StackPane> cellToPaneMap) {
        this.cellToPaneMap = cellToPaneMap;

        setAlignment(Pos.CENTER);
        setHgap(1.5);
        setVgap(1.5);

        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                // This logic assumes a 10x10 board where track cells are indexed sequentially.
                // It's a bit rigid but works if the board layout is fixed.
                int linearIndex = i * BOARD_DIMENSION + j;
                if (trackCells.stream().anyMatch(c -> c.getIndex() == linearIndex)) {
                     Cell gameCell = trackCells.stream().filter(c -> c.getIndex() == linearIndex).findFirst().get();
                    
                    StackPane cellPane = createCellPane(gameCell);

                    cellPane.setOnMouseClicked(event -> onCellClicked.accept(gameCell));
                    // Making the cells hand-cursor only when playable is handled by the main GUI class
                    // by making the whole board mouse-transparent. This simplifies logic here.
                    cellPane.setCursor(Cursor.HAND);

                    add(cellPane, j, i);
                } else {
                    Region placeholder = new Region();
                    placeholder.setPrefSize(GUI.CELL_SIZE, GUI.CELL_SIZE);
                    placeholder.getStyleClass().add("grid-placeholder");
                    add(placeholder, j, i);
                }
            }
        }
        repopulateMap(); // Initial population of the map.
    }
    
    /**
     * <<< CHANGE: This method is now a core part of the UI refresh cycle.
     * It iterates over its existing children (the cell panes) and ensures
     * they are in the shared map. This is called by GUI.refreshUI().
     */
    public void repopulateMap() {
        for (Node node : getChildren()) {
            if (node.getUserData() instanceof Cell) {
                Cell cell = (Cell) node.getUserData();
                StackPane cellPane = (StackPane) node;
                this.cellToPaneMap.put(cell, cellPane);
            }
        }
    }
    
    public void updateMarbles() {
        // First, clear all existing marble circles from all cell panes on the board.
        for (Node node : getChildren()) {
            if (node instanceof StackPane) {
                // This lambda removes any child that is a Circle and has the "MARBLE" user data.
                ((StackPane)node).getChildren().removeIf(child -> child instanceof Circle && "MARBLE".equals(child.getUserData()));
            }
        }
        
        // Second, add marble circles back only where they currently exist in the model.
        for (Node node : getChildren()) {
            if (node instanceof StackPane && node.getUserData() instanceof Cell) {
                StackPane cellPane = (StackPane) node;
                Cell gameCell = (Cell) cellPane.getUserData();
                
                if (gameCell.getMarble() != null) {
                    Circle marbleCircle = createMarbleCircle(gameCell.getMarble());
                    cellPane.getChildren().add(marbleCircle);
                }
            }
        }
    }

    public void highlightSelectedMarbles(ArrayList<Marble> selectedMarbles) {
        for (Node node : getChildren()) {
            if (node instanceof StackPane && node.getUserData() instanceof Cell) {
                StackPane cellPane = (StackPane) node;
                Cell cellModel = (Cell) cellPane.getUserData();
                if (cellPane.getChildren().isEmpty() || !(cellPane.getChildren().get(0) instanceof Rectangle)) continue;
                Rectangle cellRect = (Rectangle) cellPane.getChildren().get(0);
                
                // Reset styles first
                styleCellRectangle(cellRect, cellModel, cellPane);
                Circle marbleCircle = findMarbleCircleInCellPane(cellPane);
                if (marbleCircle != null) {
                    marbleCircle.setEffect(null);
                }

                // Apply highlight if this cell contains a selected marble
                if (cellModel.getMarble() != null && selectedMarbles.contains(cellModel.getMarble())) {
                    if (marbleCircle != null) {
                        marbleCircle.setEffect(new javafx.scene.effect.Glow(0.9));
                    }
                    cellRect.setFill(Color.GOLD.deriveColor(0, 1.0, 1.0, 0.5));
                    cellRect.setStroke(Color.GOLD.darker());
                    cellRect.setStrokeWidth(2.0);
                }
            }
        }
    }

    private StackPane createCellPane(Cell gameCell) {
        StackPane cellPane = new StackPane();
        cellPane.setPrefSize(GUI.CELL_SIZE, GUI.CELL_SIZE);
        cellPane.setUserData(gameCell);

        Rectangle cellRect = new Rectangle(GUI.CELL_SIZE, GUI.CELL_SIZE);
        cellRect.setArcWidth(8);
        cellRect.setArcHeight(8);
        cellPane.getChildren().add(cellRect);

        styleCellRectangle(cellRect, gameCell, cellPane);
        return cellPane;
    }
    
    private Circle createMarbleCircle(Marble marble) {
        Circle marbleCircle = new Circle(GUI.MARBLE_RADIUS, GUI.getFXColor(marble.getColour()));
        marbleCircle.setStroke(GUI.getFXColor(marble.getColour()).darker());
        marbleCircle.setStrokeWidth(1.5);
        String shadowColor = GUI.getFXColor(marble.getColour()).darker().toString().replace("0x", "#");
        marbleCircle.setStyle("-fx-effect: dropshadow(gaussian, " + shadowColor + ", 6, 0.3, 0, 1);");
        marbleCircle.setUserData("MARBLE"); // Tag for easy lookup
        marbleCircle.setMouseTransparent(true); // Clicks should go to the cell pane
        return marbleCircle;
    }

    private Circle findMarbleCircleInCellPane(StackPane cellPane) {
        for (Node child : cellPane.getChildren()) {
            if (child instanceof Circle && "MARBLE".equals(child.getUserData())) {
                return (Circle) child;
            }
        }
        return null;
    }
    
    private void styleCellRectangle(Rectangle cellRect, Cell gameCell, StackPane cellPane) {
        // Clear any old markers before re-styling
        cellPane.getChildren().removeIf(node -> "BASE_MARKER".equals(node.getUserData()));
        
        Color baseFill = Color.rgb(100, 110, 120);
        Color strokeCol = Color.rgb(70, 80, 90);
        double intensity = 0.4;
        
        switch (gameCell.getCellType()) {
            case NORMAL:
                cellRect.setFill(gameCell.isTrap() ? Color.INDIANRED.interpolate(baseFill, intensity) : baseFill);
                strokeCol = gameCell.isTrap() ? Color.DARKRED.darker() : strokeCol;
                break;
            case BASE:
                cellRect.setFill(GUI.getFXColor(gameCell.getColour()).interpolate(baseFill, intensity));
                strokeCol = GUI.getFXColor(gameCell.getColour()).darker();
                Circle baseMarker = new Circle(GUI.MARBLE_RADIUS / 3.5, Color.ALICEBLUE.deriveColor(0, 1, 1, 0.6));
                baseMarker.setStroke(Color.NAVY.deriveColor(0, 1, 0.5, 1));
                baseMarker.setStrokeWidth(0.8);
                baseMarker.setUserData("BASE_MARKER");
                baseMarker.setMouseTransparent(true);
                cellPane.getChildren().add(baseMarker);
                break;
            case ENTRY:
                cellRect.setFill(GUI.getFXColor(gameCell.getColour()).interpolate(baseFill, intensity));
                strokeCol = GUI.getFXColor(gameCell.getColour()).darker();
                break;
            case SAFE:
                cellRect.setFill(GUI.getFXColor(gameCell.getColour()).interpolate(baseFill, intensity));
                strokeCol = GUI.getFXColor(gameCell.getColour()).darker();
                break;
        }
        cellRect.setStroke(strokeCol);
        cellRect.setStrokeWidth(1.5);
        String innerShadowColor = strokeCol.darker().toString().replace("0x", "#");
        cellRect.setStyle("-fx-effect: innershadow(gaussian, " + innerShadowColor + ", 3, 0.05, 0, 0);");
    }
}
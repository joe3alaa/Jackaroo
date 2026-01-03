
package view;

import engine.Game;
import engine.action.ActionResult;
import engine.action.AnimationStep;
import engine.board.Cell;
import engine.board.CellType;
import engine.board.SafeZone;
import exception.GameException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.MarbleSelectionNeededException;
import exception.SplitOutOfRangeException;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Colour;
import model.card.Card;
import model.card.standard.Seven;
import model.player.CPU;
import model.player.Marble;
import model.player.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class GUI extends Application {
    private Game game;
    private BorderPane rootLayout;
    private GameBoardView gameBoardView;
    private Label turnIndicatorLabel;
    private BorderPane firePitCardPane;
    private StackPane firePitWrapper;
    private Label nextPlayerLabel;
    private Button playTurnButton;
    private Button deselectButton;
    private Label cpuStatusLabel;
    private VBox centralInfoDisplay;
    private VBox bottomSectionContainer;
    private StackPane animationLayer;
    public static final int CELL_SIZE = 35;
    public static final int MARBLE_RADIUS = CELL_SIZE / 3;
    private final List<PlayerArea> playerAreaControllers = new ArrayList<>();
    // <<< CHANGE: This map is now managed more reliably by the refreshUI method.
    private final Map<Cell, StackPane> cellToPaneMap = new HashMap<>();
    private static final Duration ANIM_DURATION_NORMAL = Duration.millis(700);
    private static final Duration ANIM_DURATION_LONG = Duration.millis(850);
    private static final Duration ANIM_DURATION_SHORT = Duration.millis(600);
    private static final Duration ANIM_MOVE_STEP = Duration.millis(150);
    private static final Duration CPU_THINK_TIME_BASE = Duration.millis(1200);
    private static final Duration CPU_THINK_TIME_RANDOM = Duration.millis(800);

    public static void main(String[] args) {
        launch(args);
    }

    // PASTE and REPLACE the ENTIRE start() method in GUI.java

 // PASTE and REPLACE the ENTIRE start() method in GUI.java

    @Override
    public void start(Stage primaryStage) {
        try {
            Font.loadFont(getClass().getResource("/fonts/Oswald-Regular.ttf").toExternalForm(), 10);
            Font.loadFont(getClass().getResource("/fonts/Oswald-Bold.ttf").toExternalForm(), 10);
            Font.loadFont(getClass().getResource("/fonts/Roboto-Regular.ttf").toExternalForm(), 10);
            Font.loadFont(getClass().getResource("/fonts/Roboto-Bold.ttf").toExternalForm(), 10);
        } catch (Exception e) {
            System.err.println("Could not load custom fonts. Using default system fonts.");
            e.printStackTrace();
        }
        primaryStage.setTitle("Jackaroo: A New Game Spin - M3 Masterpiece (Refactored)");

        // --- Name Dialog ---
        TextInputDialog nameDialog = new TextInputDialog("Player 1");
        nameDialog.setTitle("Player Name");
        nameDialog.setHeaderText("Enter Your Name to Begin the Jackaroo Adventure!");
        nameDialog.setContentText("Name:");
        javafx.scene.control.DialogPane dialogPane = nameDialog.getDialogPane();
        dialogPane.getStyleClass().add("game-dialog-pane");
        Optional<String> result = nameDialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            Platform.exit();
            return;
        }
        String humanPlayerName = result.get().trim();

        try {
            game = new Game(humanPlayerName);
        } catch (IOException e) {
            showError("Game Initialization Failed", "Could not load game resources: " + e.getMessage());
            Platform.exit();
            return;
        }

        // --- Root Layout ---
        rootLayout = new BorderPane();
        rootLayout.setPadding(new Insets(10));
        rootLayout.getStyleClass().add("root-layout");

        // --- Component Initialization ---
        Consumer<Cell> cellClickHandler = (Cell clickedCell) -> {
            if (game.getPlayers().get(game.getCurrentPlayerIndexPublic()) instanceof CPU) return;
            if (clickedCell != null && clickedCell.getMarble() != null) {
                Marble clickedMarble = clickedCell.getMarble();
                Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());
                if (currentPlayer.getSelectedMarblesPublic().contains(clickedMarble)) {
                    game.deselectAll();
                } else {
                    try {
                        game.selectMarble(clickedMarble);
                    } catch (InvalidMarbleException e) {
                        showError("Invalid Marble Selection", e.getMessage());
                    }
                }
                refreshUI();
            }
        };

        this.gameBoardView = new GameBoardView(game.getBoard().getTrack(), cellClickHandler, cellToPaneMap);
        initializePlayerAreas();

        centralInfoDisplay = new VBox(5);
        centralInfoDisplay.setAlignment(Pos.CENTER);
        
        initializeInfoPanel();
        initializeActionButtons();

        // 1. Center Area (Board and Fire Pit)
        StackPane centerStack = new StackPane(gameBoardView);

        VBox centralColumn = new VBox(10, centralInfoDisplay, centerStack);
        centralColumn.setAlignment(Pos.CENTER);

        // 2. Side Player Areas
        Pane leftPlayerPane = getControllerForPlayer(game.getPlayers().get(1)).getPane();
        Pane rightPlayerPane = getControllerForPlayer(game.getPlayers().get(3)).getPane();

        // 3. Central Layout (Board + Sides)
        BorderPane centerLayout = new BorderPane();
        centerLayout.setLeft(leftPlayerPane);
        centerLayout.setRight(rightPlayerPane);
        centerLayout.setCenter(centralColumn);
        BorderPane.setMargin(leftPlayerPane, new Insets(0, 20, 0, 0));
        BorderPane.setMargin(rightPlayerPane, new Insets(0, 0, 0, 20));
        BorderPane.setMargin(centralColumn, new Insets(0, 0, 20, 0));

        // 4. Top Player Area
        Pane topPlayerPane = getControllerForPlayer(game.getPlayers().get(2)).getPane();

        // 5. Bottom Control Panel (Player Area + Buttons) - Using VBox for compact layout
        Pane humanPlayerPane = getControllerForPlayer(game.getPlayers().get(0)).getPane();
        HBox actionButtonArea = new HBox(20, playTurnButton, deselectButton);
        actionButtonArea.setAlignment(Pos.CENTER);
        VBox bottomContainer = new VBox(12, humanPlayerPane, actionButtonArea);
        bottomContainer.setAlignment(Pos.CENTER);

        // --- Populate the Main rootLayout (which will be the bottom layer) ---
        rootLayout.setTop(topPlayerPane);
        rootLayout.setCenter(centerLayout);
        rootLayout.setBottom(bottomContainer);
        BorderPane.setMargin(topPlayerPane, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(bottomContainer, new Insets(10, 0, 0, 0));

        // <<< CRITICAL ANIMATION FIX >>>
        // The animationLayer is now a top-level pane that covers the entire scene.
        // The main game layout (rootLayout) sits underneath it.
        // This guarantees a single, consistent coordinate system for all animations.
        animationLayer = new StackPane();
        animationLayer.setMouseTransparent(true); // Clicks pass through to the UI below.
        StackPane sceneRoot = new StackPane(rootLayout, animationLayer);

        // --- Final Setup ---
        refreshUI();
        Scene scene = new Scene(sceneRoot);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
        });
        primaryStage.show();

        if (!isGameOver() && game.getPlayers().get(game.getCurrentPlayerIndexPublic()) instanceof CPU) {
            initiateCpuTurnSequence();
        }
    }

    private void handlePlayTurnAction() {
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());
        Card selectedCard = currentPlayer.getSelectedCard();
        if (selectedCard == null) {
            showError("Action Error", "Please select a card to play.");
            return;
        }

        if (selectedCard instanceof Seven && currentPlayer.getSelectedMarblesPublic().size() == 2) {
            if (!promptForSevenSplit()) {
                return;
            }
        }

        try {
            setInteractivity(false);
            ActionResult result = game.playPlayerTurn();
            playAnimationsFromResult(result, this::finalizeTurnAndRefresh);
        } catch (MarbleSelectionNeededException msne) {
            showError("Selection Required", msne.getMessage());
            setInteractivity(true);
        } catch (GameException e) {
            if (game.hasAnyValidMoveForCurrentPlayer()) {
                showError("Invalid Selection", e.getMessage() + "\n\nPlease select a valid marble (or marbles) and try again.");
                game.deselectAll();
                refreshUI();
                setInteractivity(true);
            } else {
                showError("Non-Playable Card", "You have no valid moves with this card.\n\nIt will be discarded, and your turn will now end.");
                finalizeTurnAndRefresh();
            }
        }
    }
    
    private void playAnimationsFromResult(ActionResult result, Runnable onAllAnimationsFinished) {
        if (result == null || result.getAnimationSteps().isEmpty()) {
            Platform.runLater(onAllAnimationsFinished);
            return;
        }

        // <<< FIX: Use a ParallelTransition for a more fluid, overlapping effect. >>>
        ParallelTransition allAnimations = new ParallelTransition();
        Card cardPlayerPlayed = result.getCardPlayed();
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());

        // --- Step 1: Create the Discard Animation (if any) ---
        Animation discardAnimation = null;
        for (AnimationStep step : result.getAnimationSteps()) {
            if (step instanceof AnimationStep.Discard && cardPlayerPlayed != null && ((AnimationStep.Discard) step).targetPlayer == currentPlayer) {
                discardAnimation = createDiscardAnimation((AnimationStep.Discard) step, cardPlayerPlayed);
                break;
            }
        }
        if (discardAnimation != null) {
            allAnimations.getChildren().add(discardAnimation);
        }
        
        // --- Step 2: Create a container for all marble animations ---
        ParallelTransition marbleAnimations = new ParallelTransition();
        for (AnimationStep step : result.getAnimationSteps()) {
            Animation animationForStep = null; 

            if (step instanceof AnimationStep.Move) {
                animationForStep = createMoveAnimation(((AnimationStep.Move) step).marble, ((AnimationStep.Move) step).path);
            } else if (step instanceof AnimationStep.Field) {
                animationForStep = createFieldingAnimation(((AnimationStep.Field) step).fieldedMarble, ((AnimationStep.Field) step).toBaseCell);
            } else if (step instanceof AnimationStep.Swap) {
                AnimationStep.Swap s = (AnimationStep.Swap) step;
                animationForStep = createSwapAnimation(s.marble1, s.start1, s.end1, s.marble2, s.start2, s.end2);
            } else if (step instanceof AnimationStep.Save) {
                animationForStep = createSaveAnimation(((AnimationStep.Save) step).savedMarble, ((AnimationStep.Save) step).fromCell, ((AnimationStep.Save) step).toSafeCell);
            } else if (step instanceof AnimationStep.Destroy) {
                animationForStep = createDestroyAnimation(((AnimationStep.Destroy) step).destroyedMarble, ((AnimationStep.Destroy) step).fromCell);
            }
            // Discard is handled separately, so we ignore it here

            if (animationForStep != null) {
                marbleAnimations.getChildren().add(animationForStep);
            }
        }
        
        // --- Step 3: Add the marble animations with a slight delay ---
        if (!marbleAnimations.getChildren().isEmpty()) {
             // By wrapping the marble animations in a SequentialTransition with a Pause,
             // we ensure they start slightly after the card discard, creating a nice overlap.
             SequentialTransition delayedMarbleAnimations = new SequentialTransition(
                 new PauseTransition(Duration.millis(200)),
                 marbleAnimations
             );
             allAnimations.getChildren().add(delayedMarbleAnimations);
        }

        allAnimations.setOnFinished(event -> Platform.runLater(onAllAnimationsFinished));
        allAnimations.play();
    }
    
    private Animation createDiscardAnimation(AnimationStep.Discard discard, Card cardToAnimate) {
        Player fromPlayer = discard.targetPlayer;
        PlayerArea pa = getControllerForPlayer(fromPlayer);
        if (pa == null) {
            System.err.println("Could not find PlayerArea to start discard animation for " + fromPlayer.getName());
            return new PauseTransition(Duration.ZERO);
        }
        Node cardDisplayArea = pa.getPane().lookup("#cardDisplayArea_" + fromPlayer.getColour());
        if (!(cardDisplayArea instanceof HBox)) {
            return new PauseTransition(Duration.ZERO);
        }
        HBox hbox = (HBox) cardDisplayArea;

        Node startNode = null;
        if (fromPlayer instanceof CPU) {
            startNode = hbox; // Animate from the general card area for CPUs.
        } else {
            for (Node node : hbox.getChildren()) {
                if (node instanceof CardView && cardToAnimate.equals(node.getUserData())) {
                    startNode = node;
                    node.setVisible(false); // Hide the original card before animating the copy.
                    break;
                }
            }
        }
        if (startNode == null) {
            startNode = hbox;
        }

        // --- Create a temporary CardView on the animation layer ---
        Point2D startCenter = getCenterInAnimationLayer(startNode);
        Point2D endCenter = getCenterInAnimationLayer(firePitWrapper);

        CardView animatedCard = new CardView(cardToAnimate);
        animatedCard.setManaged(false); // <<< FIX: Tell the StackPane not to manage this node's position.
        animationLayer.getChildren().add(animatedCard);
        animatedCard.setOpacity(0.0); // Start transparent to avoid a flicker.

        // --- Define the animation timeline ---
        Timeline timeline = new Timeline();
        // Set initial state at 0ms. Use layoutX/Y and account for width/height to center it.
        KeyFrame kf0 = new KeyFrame(Duration.ZERO,
            new KeyValue(animatedCard.layoutXProperty(), startCenter.getX() - animatedCard.getPrefWidth() / 2),
            new KeyValue(animatedCard.layoutYProperty(), startCenter.getY() - animatedCard.getPrefHeight() / 2),
            new KeyValue(animatedCard.opacityProperty(), 1.0)
        );
        // Animate to the final state.
        KeyFrame kf1 = new KeyFrame(ANIM_DURATION_LONG,
            new KeyValue(animatedCard.layoutXProperty(), endCenter.getX() - animatedCard.getPrefWidth() / 2, Interpolator.EASE_BOTH),
            new KeyValue(animatedCard.layoutYProperty(), endCenter.getY() - animatedCard.getPrefHeight() / 2, Interpolator.EASE_BOTH),
            new KeyValue(animatedCard.scaleXProperty(), 0.8, Interpolator.EASE_IN),
            new KeyValue(animatedCard.scaleYProperty(), 0.8, Interpolator.EASE_IN),
            new KeyValue(animatedCard.rotateProperty(), 15 + (Math.random() - 0.5) * 20, Interpolator.EASE_OUT)
        );
        timeline.getKeyFrames().addAll(kf0, kf1);

        timeline.setOnFinished(e -> animationLayer.getChildren().remove(animatedCard));
        return timeline;
    }

    private void finalizeTurnAndRefresh() {
        game.endPlayerTurn();
        refreshUI();
        checkForWin();
        if (!isGameOver()) {
            if (game.getPlayers().get(game.getCurrentPlayerIndexPublic()) instanceof CPU) {
                initiateCpuTurnSequence();
            } else {
                setInteractivity(true);
            }
        }
    }
    
    private boolean promptForSevenSplit() {
        TextInputDialog splitDialog = new TextInputDialog("1");
        splitDialog.setTitle("Seven Card Split");
        splitDialog.setHeaderText("Enter split distance for the first marble (1-6).");
        splitDialog.setContentText("Distance (1-6):");
        splitDialog.getDialogPane().getStyleClass().add("game-dialog-pane");
        Optional<String> result = splitDialog.showAndWait();
        if (result.isPresent()) {
            try {
                int distance = Integer.parseInt(result.get());
                game.editSplitDistance(distance);
                return true;
            } catch (NumberFormatException | SplitOutOfRangeException ex) {
                showError("Split Error", "Invalid distance. Must be a number from 1 to 6.");
                return false;
            }
        }
        return false;
    }

    private Transition createMoveAnimation(Marble marble, ArrayList<Cell> path) {
        if (path == null || path.size() < 2) return new PauseTransition(Duration.ZERO);

        Circle animatedMarbleView = createAnimatedMarbleView(marble);
        animatedMarbleView.setManaged(false); // <<< FIX: Tell the StackPane not to manage this node.
        animationLayer.getChildren().add(animatedMarbleView);

        // Hide the static marble in the source cell to prevent it from being visible during animation.
        Cell sourceModelCell = path.get(0);
        StackPane sourceCellPane = findCellPaneOnBoard(sourceModelCell);
        if (sourceCellPane != null) {
            Circle staticMarbleInSource = findMarbleCircleInCellPane(sourceCellPane);
            if (staticMarbleInSource != null) staticMarbleInSource.setVisible(false);
        }

        SequentialTransition pathAnimation = createSequentialMoveForMarble(animatedMarbleView, path);
        
        pathAnimation.setOnFinished(event -> {
            animationLayer.getChildren().remove(animatedMarbleView);
        });
        return pathAnimation;
    }

    private Animation createFieldingAnimation(Marble fieldedMarble, Cell toBaseCell) {
        StackPane targetBaseCellPane = findCellPaneOnBoard(toBaseCell);
        if (targetBaseCellPane == null) return new PauseTransition(Duration.ZERO);

        Point2D endCenter = getCenterInAnimationLayer(targetBaseCellPane);

        Circle animatedMarbleView = createAnimatedMarbleView(fieldedMarble);
        animatedMarbleView.setManaged(false);
        animationLayer.getChildren().add(animatedMarbleView);
        
        Circle staticMarbleInTarget = findMarbleCircleInCellPane(targetBaseCellPane);
        if (staticMarbleInTarget != null) staticMarbleInTarget.setVisible(false);
        
        Timeline timeline = new Timeline(
            // <<< FIX: Define the marble's position at the start of the animation. >>>
            // At the start, it's invisible, tiny, and already at its final location.
            new KeyFrame(Duration.ZERO,
                new KeyValue(animatedMarbleView.layoutXProperty(), endCenter.getX()),
                new KeyValue(animatedMarbleView.layoutYProperty(), endCenter.getY()),
                new KeyValue(animatedMarbleView.scaleXProperty(), 0.0),
                new KeyValue(animatedMarbleView.scaleYProperty(), 0.0),
                new KeyValue(animatedMarbleView.opacityProperty(), 0.0)
            ),
            // <<< FIX: Re-define the marble's position at the end of the animation. >>>
            // This guarantees it stays put while the other properties change.
            new KeyFrame(ANIM_DURATION_NORMAL,
                new KeyValue(animatedMarbleView.layoutXProperty(), endCenter.getX()),
                new KeyValue(animatedMarbleView.layoutYProperty(), endCenter.getY()),
                new KeyValue(animatedMarbleView.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(animatedMarbleView.scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(animatedMarbleView.opacityProperty(), 1.0, Interpolator.EASE_OUT)
            )
        );

        timeline.setOnFinished(event -> {
            animationLayer.getChildren().remove(animatedMarbleView);
        });
        return timeline;
    }

    private Transition createSwapAnimation(Marble m1, Cell m1Start, Cell m1End, Marble m2, Cell m2Start, Cell m2End) {
        Circle m1View = createAnimatedMarbleView(m1);
        Circle m2View = createAnimatedMarbleView(m2);
        
        m1View.setManaged(false); // <<< FIX: Tell the StackPane not to manage this node.
        m2View.setManaged(false); // <<< FIX: Tell the StackPane not to manage this node.

        animationLayer.getChildren().addAll(m1View, m2View);

        StackPane m1StartPane = findCellPaneOnBoard(m1Start);
        StackPane m1EndPane = findCellPaneOnBoard(m1End);
        StackPane m2StartPane = findCellPaneOnBoard(m2Start);
        StackPane m2EndPane = findCellPaneOnBoard(m2End);

        if (m1StartPane == null || m1EndPane == null || m2StartPane == null || m2EndPane == null) {
            animationLayer.getChildren().removeAll(m1View, m2View);
            return new PauseTransition(Duration.ZERO);
        }

        Circle staticM1 = findMarbleCircleInCellPane(m1EndPane);
        Circle staticM2 = findMarbleCircleInCellPane(m2EndPane);
        if (staticM1 != null) staticM1.setVisible(false);
        if (staticM2 != null) staticM2.setVisible(false);

        Timeline m1Timeline = createSingleMarbleMoveTimeline(m1View, m1StartPane, m1EndPane, ANIM_DURATION_NORMAL);
        Timeline m2Timeline = createSingleMarbleMoveTimeline(m2View, m2StartPane, m2EndPane, ANIM_DURATION_NORMAL);
        ParallelTransition parallelSwap = new ParallelTransition(m1Timeline, m2Timeline);

        parallelSwap.setOnFinished(event -> {
            animationLayer.getChildren().removeAll(m1View, m2View);
        });

        return parallelSwap;
    }

    private Animation createSaveAnimation(Marble marble, Cell fromCell, Cell toCell) {
        Circle animatedMarbleView = createAnimatedMarbleView(marble);
        animatedMarbleView.setManaged(false);
        animationLayer.getChildren().add(animatedMarbleView);

        StackPane fromPane = findCellPaneOnBoard(fromCell);
        StackPane toPane = findCellPaneOnBoard(toCell);

        if (fromPane == null || toPane == null) {
            animationLayer.getChildren().remove(animatedMarbleView);
            return new PauseTransition(Duration.ZERO);
        }

        Circle staticMarble = findMarbleCircleInCellPane(toPane);
        if (staticMarble != null) staticMarble.setVisible(false);

        // The initial move from the track to the safe cell.
        Timeline moveTimeline = createSingleMarbleMoveTimeline(animatedMarbleView, fromPane, toPane, ANIM_DURATION_NORMAL);
        
        // <<< FIX: Add a "pulse" animation for a more satisfying feel. >>>
        Timeline pulseTimeline = new Timeline(
            // KeyFrame to scale the marble up slightly.
            new KeyFrame(Duration.millis(150),
                new KeyValue(animatedMarbleView.scaleXProperty(), 1.3, Interpolator.EASE_OUT),
                new KeyValue(animatedMarbleView.scaleYProperty(), 1.3, Interpolator.EASE_OUT)
            ),
            // KeyFrame to return it to its normal size.
            new KeyFrame(Duration.millis(400),
                new KeyValue(animatedMarbleView.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                new KeyValue(animatedMarbleView.scaleYProperty(), 1.0, Interpolator.EASE_IN)
            )
        );

        // Chain the move and the pulse together to play one after the other.
        SequentialTransition fullSaveAnimation = new SequentialTransition(moveTimeline, pulseTimeline);

        fullSaveAnimation.setOnFinished(event -> {
            animationLayer.getChildren().remove(animatedMarbleView);
        });
        
        return fullSaveAnimation;
    }

    private Animation createDestroyAnimation(Marble marble, Cell fromCell) {
        StackPane cellPane = findCellPaneOnBoard(fromCell);
        if (cellPane == null) return new PauseTransition(Duration.ZERO);

        Circle animatedMarbleView = createAnimatedMarbleView(marble);
        animatedMarbleView.setManaged(false);
        animationLayer.getChildren().add(animatedMarbleView);

        Point2D cellCenter = getCenterInAnimationLayer(cellPane);

        Circle staticMarble = findMarbleCircleInCellPane(cellPane);
        if (staticMarble != null) {
            staticMarble.setVisible(false);
        }

        Timeline destroyAnimation = new Timeline(
            // Set initial state at 0ms.
            new KeyFrame(Duration.ZERO,
                new KeyValue(animatedMarbleView.layoutXProperty(), cellCenter.getX()),
                new KeyValue(animatedMarbleView.layoutYProperty(), cellCenter.getY()),
                new KeyValue(animatedMarbleView.opacityProperty(), 1.0)
            ),
            // Animate to the final "destroyed" state.
            new KeyFrame(ANIM_DURATION_SHORT,
                // <<< FIX: Add a slight upward move to make it "pop" off the board. >>>
                new KeyValue(animatedMarbleView.layoutYProperty(), cellCenter.getY() - 20, Interpolator.EASE_OUT),
                new KeyValue(animatedMarbleView.opacityProperty(), 0.0, Interpolator.EASE_IN),
                new KeyValue(animatedMarbleView.scaleXProperty(), 0.1, Interpolator.EASE_IN),
                new KeyValue(animatedMarbleView.scaleYProperty(), 0.1, Interpolator.EASE_IN),
                new KeyValue(animatedMarbleView.rotateProperty(), 180, Interpolator.EASE_IN)
            )
        );

        destroyAnimation.setOnFinished(event -> {
            animationLayer.getChildren().remove(animatedMarbleView);
        });
        return destroyAnimation;
    }

    private SequentialTransition createSequentialMoveForMarble(Circle marbleView, ArrayList<Cell> path) {
        SequentialTransition sequentialTransition = new SequentialTransition();
        if (path == null || path.size() < 2) return sequentialTransition;

        // <<< FIX: A complete rewrite using a robust "from-to" pattern for each step. >>>
        // This eliminates the timing bug that caused the animation to start from (0,0).

        for (int i = 0; i < path.size() - 1; i++) {
            // For each segment of the path, get the start and end cell panes.
            StackPane fromPane = findCellPaneOnBoard(path.get(i));
            StackPane toPane = findCellPaneOnBoard(path.get(i + 1));

            if (fromPane == null || toPane == null) continue;

            // Get the absolute center coordinates for the start and end of this segment.
            Point2D fromPoint = getCenterInAnimationLayer(fromPane);
            Point2D toPoint = getCenterInAnimationLayer(toPane);

            // Create a self-contained timeline for this single step.
            Timeline stepTimeline = new Timeline(
                // At the beginning of this step, explicitly place the marble at the "from" position.
                new KeyFrame(Duration.ZERO,
                    new KeyValue(marbleView.layoutXProperty(), fromPoint.getX()),
                    new KeyValue(marbleView.layoutYProperty(), fromPoint.getY()),
                    new KeyValue(marbleView.opacityProperty(), 1.0) // Ensure it's visible
                ),
                // At the end of this step, animate it to the "to" position.
                new KeyFrame(ANIM_MOVE_STEP,
                    new KeyValue(marbleView.layoutXProperty(), toPoint.getX(), Interpolator.EASE_BOTH),
                    new KeyValue(marbleView.layoutYProperty(), toPoint.getY(), Interpolator.EASE_BOTH),
                 // <<< FIX: Explicitly maintain the opacity to prevent it from reverting. >>>
                    new KeyValue(marbleView.opacityProperty(), 1.0)
                )
            );
            // Add this completed step to the overall sequence.
            sequentialTransition.getChildren().add(stepTimeline);
        }
        return sequentialTransition;
    }

    private Timeline createSingleMarbleMoveTimeline(Circle marbleView, StackPane startPane, StackPane endPane, Duration duration) {
        Point2D startCenter = getCenterInAnimationLayer(startPane);
        Point2D endCenter = getCenterInAnimationLayer(endPane);

        // <<< FIX: This uses the same robust from-to pattern. >>>
        return new Timeline(
            // At the start, explicitly define the position and make it appear.
            new KeyFrame(Duration.ZERO,
                new KeyValue(marbleView.layoutXProperty(), startCenter.getX()),
                new KeyValue(marbleView.layoutYProperty(), startCenter.getY()),
                new KeyValue(marbleView.opacityProperty(), 0.0)
            ),
            // At the end, define the final position and full opacity.
            new KeyFrame(duration,
                new KeyValue(marbleView.layoutXProperty(), endCenter.getX(), Interpolator.EASE_BOTH),
                new KeyValue(marbleView.layoutYProperty(), endCenter.getY(), Interpolator.EASE_BOTH),
                new KeyValue(marbleView.opacityProperty(), 1.0)
            )
        );
    }

    private Circle createAnimatedMarbleView(Marble marble) {
        Circle animatedMarbleView = new Circle(MARBLE_RADIUS, getFXColor(marble.getColour()));
        animatedMarbleView.setStroke(getFXColor(marble.getColour()).darker());
        animatedMarbleView.setStrokeWidth(1.5);
        animatedMarbleView.setStyle("-fx-effect: dropshadow(gaussian, " + getFXColor(marble.getColour()).darker().toString().replace("0x", "#") + ", 8, 0.4, 0, 2);");
        // <<< CHANGE: The animation layer is mouse transparent, so this is not strictly needed but good practice.
        animatedMarbleView.setMouseTransparent(true);
        
        // <<< FIX: Set opacity to 0 by default to prevent flickering. >>>
        // The animation timelines are now solely responsible for making the marble visible
        // at the correct moment, eliminating the single-frame flicker at the top-left.
        animatedMarbleView.setOpacity(0.0);
        
        return animatedMarbleView;
    }

    private void handleDeselectAction() {
        game.deselectAll();
        refreshUI();
    }

    private void refreshUI() {
        // --- Step 1: Clear the map to prepare for a full, clean rebuild.
        cellToPaneMap.clear();

        // --- Step 2: Have the main board populate the map with all track cells.
        gameBoardView.repopulateMap();
        
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());
        
        Consumer<Card> cardClickHandler = (Card card) -> {
            if (game.getPlayers().get(game.getCurrentPlayerIndexPublic()) instanceof CPU) return;
            if (currentPlayer.getSelectedCard() == card) {
                game.deselectAll();
            } else {
                try {
                    game.selectCard(card);
                } catch (InvalidCardException e) {
                    showError("Invalid Card", e.getMessage());
                }
            }
            refreshUI();
        };

        // --- Step 3: Loop through each player area. This updates their visual state (cards, home marbles)
        // AND populates the map with their unique safe zone cells.
        for (PlayerArea controller : playerAreaControllers) {
            boolean isHumanPlayer = controller.getPlayer() == game.getPlayers().get(0);
            controller.update(game, isHumanPlayer ? cardClickHandler : c -> {}, cellToPaneMap);
            controller.setHighlight(controller.getPlayer() == currentPlayer);
        }

        // --- Step 4: Now that the UI is fully updated and the map is complete, update visual elements
        // that depend on the final state.
        gameBoardView.updateMarbles();
        gameBoardView.highlightSelectedMarbles(currentPlayer.getSelectedMarblesPublic());
        
        turnIndicatorLabel.setText("Current: " + currentPlayer.getName());
        turnIndicatorLabel.setTextFill(getFXColor(currentPlayer.getColour()));
        
        Player nextPlayer = game.getPlayers().get((game.getCurrentPlayerIndexPublic() + 1) % game.getPlayers().size());
        nextPlayerLabel.setText("Next: " + nextPlayer.getName());
        nextPlayerLabel.setTextFill(getFXColor(nextPlayer.getColour()).desaturate().interpolate(Color.LIGHTGRAY, 0.5));

        updateFirePitCard();
        if (!(currentPlayer instanceof CPU)) {
            cpuStatusLabel.setText("");
        }

        setInteractivity(!(currentPlayer instanceof CPU));
    }

    private void updateFirePitCard() {
        firePitCardPane.getChildren().clear();
        if (!game.getFirePit().isEmpty()) {
            Card topFirePitCard = game.getFirePit().get(game.getFirePit().size() - 1);
            CardView firePitCardView = new CardView(topFirePitCard);
            firePitCardPane.setCenter(firePitCardView);
        } else {
            Label emptyFirePitText = new Label("Fire Pit");
            emptyFirePitText.setTextFill(Color.rgb(100, 100, 100));
            emptyFirePitText.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 13));
            BorderPane.setAlignment(emptyFirePitText, Pos.CENTER);
            firePitCardPane.setCenter(emptyFirePitText);
        }
    }

    private void setInteractivity(boolean isHumanTurn) {
        playTurnButton.setDisable(!isHumanTurn || game.getPlayers().get(game.getCurrentPlayerIndexPublic()).getSelectedCard() == null);
        deselectButton.setDisable(!isHumanTurn);
        gameBoardView.setMouseTransparent(!isHumanTurn);
        
        // Get the human player from the game model (always at index 0)
        Player humanPlayer = game.getPlayers().get(0);
        PlayerArea humanController = getControllerForPlayer(humanPlayer);
        
        if (humanController != null) {
            // PlayerArea pane contains the cards, so we make it interactive or not.
            humanController.getPane().setMouseTransparent(!isHumanTurn);
        }
    }

    private void initializeActionButtons() {
        playTurnButton = new Button("Play Turn");
        playTurnButton.getStyleClass().addAll("action-button", "action-button-play");
        playTurnButton.setOnAction(e -> handlePlayTurnAction());
        
        deselectButton = new Button("Deselect All");
        deselectButton.getStyleClass().addAll("action-button", "action-button-deselect");
        deselectButton.setOnAction(e -> handleDeselectAction());
    }

    private void initializePlayerAreas() {
        playerAreaControllers.clear();
        ArrayList<Player> turnOrder = game.getPlayers();

        for (int i = 0; i < turnOrder.size(); i++) {
            Player p = turnOrder.get(i);
            Pos alignment;

            switch (i) {
                case 0:  alignment = Pos.BOTTOM_CENTER; break;
                case 1:  alignment = Pos.CENTER_RIGHT;  break;
                case 2:  alignment = Pos.TOP_CENTER;    break;
                case 3:  alignment = Pos.CENTER_LEFT;   break;
                default: continue;
            }
            playerAreaControllers.add(new PlayerArea(p, alignment, game));
        }
    }
    
    private PlayerArea getControllerForPlayer(Player player) {
        for (PlayerArea controller : playerAreaControllers) {
            if (controller.getPlayer() == player) {
                return controller;
            }
        }
        return null;
    }

    private void initializeInfoPanel() {
        turnIndicatorLabel = new Label("Turn: ");
        turnIndicatorLabel.setFont(Font.font("Oswald", FontWeight.BOLD, 16));
        turnIndicatorLabel.setTextFill(Color.LIGHTCYAN);
        
        nextPlayerLabel = new Label("Next: ");
        nextPlayerLabel.setFont(Font.font("Roboto", FontWeight.NORMAL, 14));
        nextPlayerLabel.setTextFill(Color.rgb(180, 180, 180));
        
        firePitCardPane = new BorderPane();
        
        double cardWidth = CELL_SIZE * 1.4;
        double cardHeight = CELL_SIZE * 1.9;
        firePitWrapper = new StackPane(firePitCardPane);
        firePitWrapper.getStyleClass().add("fire-pit-wrapper");
        firePitWrapper.setPrefSize(cardWidth, cardHeight);
        firePitWrapper.setMaxSize(cardWidth, cardHeight);
        firePitWrapper.setMinSize(cardWidth, cardHeight);

        HBox turnInfoLine = new HBox(25, turnIndicatorLabel, nextPlayerLabel);
        turnInfoLine.setAlignment(Pos.CENTER);
        cpuStatusLabel = new Label("");
        cpuStatusLabel.setFont(Font.font("Roboto", FontPosture.ITALIC, 12));
        cpuStatusLabel.setTextFill(Color.LIGHTSALMON);
        cpuStatusLabel.setMinHeight(20);
        
        centralInfoDisplay.getChildren().addAll(firePitWrapper, turnInfoLine, cpuStatusLabel);
    }    
    
    public static Color getFXColor(Colour modelColour) {
        if (modelColour == null) return Color.WHITE;
        switch (modelColour) {
            case RED:    return Color.CRIMSON;
            case GREEN:  return Color.FORESTGREEN;
            case BLUE:   return Color.ROYALBLUE;
            case YELLOW: return Color.GOLD;
            default:     return Color.SLATEGRAY;
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.getDialogPane().getStyleClass().add("game-dialog-pane");
            alert.showAndWait();
        });
    }

    private boolean isGameOver() {
        return game.checkWin() != null;
    }

    private void checkForWin() {
        Colour winner = game.checkWin();
        if (winner != null) {
            setInteractivity(false);
            cpuStatusLabel.setText(winner.toString() + " WINS! Game Over!");
            cpuStatusLabel.setTextFill(getFXColor(winner).brighter());
            cpuStatusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            showError("Game Over!", winner.toString() + " WINS! Congratulations!");
        }
    }

    private void initiateCpuTurnSequence() {
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndexPublic());
        if (!(currentPlayer instanceof CPU) || isGameOver()) {
            refreshUI();
            return;
        }
        setInteractivity(false);
        cpuStatusLabel.setText(currentPlayer.getName() + " is thinking...");
        cpuStatusLabel.setTextFill(getFXColor(currentPlayer.getColour()).interpolate(Color.WHITE, 0.3));
        
        // Use constants for timing
        Duration randomDelay = CPU_THINK_TIME_RANDOM.multiply(Math.random());
        PauseTransition thinkingPause = new PauseTransition(CPU_THINK_TIME_BASE.add(randomDelay));
        
        thinkingPause.setOnFinished(event -> performCpuAction((CPU) currentPlayer));
        thinkingPause.play();
    }
    
    private void performCpuAction(CPU cpuPlayer) {
        if (isGameOver()) return;
        try {
            ActionResult result = game.playPlayerTurn();
            if (result.getCardPlayed() != null) {
                 cpuStatusLabel.setText(cpuPlayer.getName() + " played: " + result.getCardPlayed().getName());
            } else {
                 cpuStatusLabel.setText(cpuPlayer.getName() + " made a move.");
            }
            playAnimationsFromResult(result, this::finalizeTurnAndRefresh);
        } catch (GameException e) {
            // Provide feedback to the user in the GUI instead of just the console.
            System.err.println("CPU (" + cpuPlayer.getName() + ") turn resulted in an error: " + e.getMessage());
            cpuStatusLabel.setText(cpuPlayer.getName() + " couldn't make a valid move and forfeits the turn.");
            // End the turn gracefully so the game doesn't get stuck.
            finalizeTurnAndRefresh();
        }
    }

    private Circle findMarbleCircleInCellPane(StackPane cellPane) {
        if (cellPane == null) return null;
        for (Node child : cellPane.getChildren()) {
            if (child instanceof Circle && "MARBLE".equals(child.getUserData())) {
                return (Circle) child;
            }
        }
        return null;
    }
    
    // <<< CHANGE: The problematic recursive refresh has been removed.
    // This method now relies on the `cellToPaneMap` being correct, which the new `refreshUI` logic ensures.
    private StackPane findCellPaneOnBoard(Cell cellModel) {
        if (cellModel == null) return null;
        StackPane pane = cellToPaneMap.get(cellModel);
        if (pane == null) {
            // This should no longer happen with the new refresh logic, but we keep it as a diagnostic warning.
            System.err.println("CRITICAL WARNING: UI StackPane not found for Cell model: " + cellModel + ". Animations may fail.");
        }
        return pane;
    }
    
    /**
     * A robust method to calculate the center point of any UI Node and return its
     * coordinates within the animationLayer's coordinate system. This avoids
     * layout timing issues and provides a single, reliable way to get animation
     * start/end points.
     * @param node The node to find the center of.
     * @return The center point of the node, in coordinates relative to the animationLayer.
     */
    private Point2D getCenterInAnimationLayer(Node node) {
        if (node == null) {
            System.err.println("Warning: Attempted to find center of a null node for animation.");
            return new Point2D(0, 0);
        }
        // <<< FIX: Use getLayoutBounds() instead of getBoundsInLocal(). >>>
        // This calculates the position based on the pure layout size, ignoring visual effects
        // like shadows that could offset the calculated center point.
        Bounds nodeBoundsInScene = node.localToScene(node.getLayoutBounds());

        Point2D nodeCenterInScene = new Point2D(
            nodeBoundsInScene.getMinX() + nodeBoundsInScene.getWidth() / 2,
            nodeBoundsInScene.getMinY() + nodeBoundsInScene.getHeight() / 2
        );

        return animationLayer.sceneToLocal(nodeCenterInScene);
    }
}
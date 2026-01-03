
package engine.board;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import engine.GameManager;
import engine.action.ActionResult;
import exception.*;
import model.Colour;
import model.player.Marble;
import model.player.Player;

/**
 * Comprehensive test suite for the Board class.
 * 
 * Testing philosophy: AAA Pattern
 * - Arrange: Set up test data
 * - Act: Execute the method being tested
 * - Assert: Verify the outcome
 * 
 * @author Your Name
 */
@DisplayName("Board Class Tests")
class BoardTest {

    private Board board;
    
    @Mock
    private GameManager mockGameManager;
    
    private Player testPlayer;
    private Marble testMarble;
    private ActionResult testResult;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Standard test setup
        ArrayList<Colour> colorOrder = new ArrayList<>(
            Arrays.asList(Colour.GREEN, Colour.RED, Colour.YELLOW, Colour.BLUE)
        );
        
        when(mockGameManager.getActivePlayerColour()).thenReturn(Colour.GREEN);
        
        board = new Board(colorOrder, mockGameManager);
        testPlayer = new Player("TestPlayer", Colour.GREEN);
        testPlayer.setGameReference((engine.Game) mockGameManager); // Cast if needed
        testMarble = testPlayer.getOneMarble();
        testResult = new ActionResult(testPlayer, null);
    }
    
    // ==================== TEST GROUP 1: BASIC MOVEMENT ====================
    
    @Nested
    @DisplayName("Basic Movement Tests")
    class BasicMovementTests {
        
        @Test
        @DisplayName("Test 1: Move marble forward by standard distance")
        void testSimpleForwardMove() throws Exception {
            // Arrange: Place marble at position 0 (Green's base)
            board.sendToBase(testMarble, testResult);
            
            // Act: Move forward 5 spaces
            board.moveBy(testMarble, 5, false, testResult);
            
            // Assert: Marble should be at position 5
            Cell expectedCell = board.getTrack().get(5);
            assertEquals(testMarble, expectedCell.getMarble(),
                "Marble should have moved to position 5");
        }
        
        @Test
        @DisplayName("Test 2: Move marble backward with Four card")
        void testBackwardMove() throws Exception {
            // Arrange: Place marble at position 10
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 10, false, testResult);
            
            // Act: Move backward 4 spaces
            board.moveBy(testMarble, -4, false, testResult);
            
            // Assert: Marble should be at position 6
            Cell expectedCell = board.getTrack().get(6);
            assertEquals(testMarble, expectedCell.getMarble(),
                "Marble should have moved backward to position 6");
        }
        
        @Test
        @DisplayName("Test 3: Move wraps around the board correctly")
        void testWrapAroundMove() throws Exception {
            // Arrange: Place marble at position 95 (near end of track)
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 95, false, testResult);
            
            // Act: Move forward 10 spaces (should wrap to position 5)
            board.moveBy(testMarble, 10, false, testResult);
            
            // Assert: Marble should be at position 5
            Cell expectedCell = board.getTrack().get(5);
            assertEquals(testMarble, expectedCell.getMarble(),
                "Marble should wrap around and land at position 5");
        }
    }
    
    // ==================== TEST GROUP 2: SAFE ZONE ====================
    
    @Nested
    @DisplayName("Safe Zone Tests")
    class SafeZoneTests {
        
        @Test
        @DisplayName("Test 4: Marble enters safe zone when passing entry")
        void testEnterSafeZone() throws Exception {
            // Arrange: Green's entry is at position 98
            // Place marble at position 95
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 95, false, testResult);
            
            // Act: Move 5 spaces (passes entry at 98, lands in safe zone)
            board.moveBy(testMarble, 5, false, testResult);
            
            // Assert: Marble should be in safe zone at position 1
            SafeZone greenSafeZone = board.getSafeZones().stream()
                .filter(sz -> sz.getColour() == Colour.GREEN)
                .findFirst()
                .orElse(null);
            
            assertNotNull(greenSafeZone, "Green safe zone should exist");
            assertEquals(testMarble, greenSafeZone.getCells().get(1).getMarble(),
                "Marble should be at position 1 in safe zone");
        }
        
        @Test
        @DisplayName("Test 5: Cannot move backward in safe zone")
        void testCannotMoveBackwardInSafeZone() throws Exception {
            // Arrange: Place marble in safe zone
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 99, false, testResult); // Enter safe zone
            
            // Act & Assert: Moving backward should throw exception
            assertThrows(IllegalMovementException.class, () -> {
                board.moveBy(testMarble, -1, false, testResult);
            }, "Should not allow backward movement in safe zone");
        }
        
        @Test
        @DisplayName("Test 6: Cannot overshoot safe zone")
        void testCannotOvershootSafeZone() throws Exception {
            // Arrange: Place marble at last safe zone position (index 3)
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 102, false, testResult); // Enters at position 3
            
            // Act & Assert: Moving 2 more steps should fail
            assertThrows(IllegalMovementException.class, () -> {
                board.moveBy(testMarble, 2, false, testResult);
            }, "Should not allow overshooting safe zone");
        }
    }
    
    // ==================== TEST GROUP 3: COLLISION & CAPTURE ====================
    
    @Nested
    @DisplayName("Collision and Capture Tests")
    class CollisionTests {
        
        @Test
        @DisplayName("Test 7: Marble captures opponent when landing on them")
        void testCaptureOpponent() throws Exception {
            // Arrange: Place opponent marble at position 10
            Marble opponentMarble = new Marble(Colour.RED);
            board.getTrack().get(10).setMarble(opponentMarble);
            
            // Place our marble at position 5
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 5, false, testResult);
            
            // Act: Move to position 10 (capture opponent)
            board.moveBy(testMarble, 5, false, testResult);
            
            // Assert: Our marble should be there, opponent should be gone
            assertEquals(testMarble, board.getTrack().get(10).getMarble(),
                "Our marble should occupy position 10");
            
            // Verify opponent was sent home
            verify(mockGameManager, times(1)).sendHome(eq(opponentMarble), any());
        }
        
        @Test
        @DisplayName("Test 8: Cannot land on own marble")
        void testCannotLandOnOwnMarble() throws Exception {
            // Arrange: Place two of our marbles on the board
            Marble marble1 = testPlayer.getOneMarble();
            Marble marble2 = testPlayer.getOneMarble();
            
            board.sendToBase(marble1, testResult);
            board.sendToBase(marble2, testResult);
            
            board.moveBy(marble1, 5, false, testResult);
            
            // Act & Assert: marble2 cannot land on marble1
            assertThrows(IllegalMovementException.class, () -> {
                board.moveBy(marble2, 5, false, testResult);
            }, "Should not allow landing on own marble");
        }
        
        @Test
        @DisplayName("Test 9: Cannot bypass marble on its protected base")
        void testCannotBypassProtectedBase() throws Exception {
            // Arrange: Place Red marble on Red's base (position 25)
            Marble redMarble = new Marble(Colour.RED);
            board.getTrack().get(25).setMarble(redMarble);
            
            // Place our marble before Red's base
            board.sendToBase(testMarble, testResult);
            board.moveBy(testMarble, 20, false, testResult);
            
            // Act & Assert: Cannot move through protected base
            assertThrows(IllegalMovementException.class, () -> {
                board.moveBy(testMarble, 10, false, testResult);
            }, "Should not bypass opponent on protected base");
        }
    }
    
    // ==================== TEST GROUP 4: SPECIAL ACTIONS ====================
    
    @Nested
    @DisplayName("Special Action Tests")
    class SpecialActionTests {
        
        @Test
        @DisplayName("Test 10: Swap marbles correctly exchanges positions")
        void testSwapMarbles() throws Exception {
            // Arrange: Place two marbles on different positions
            Marble marble1 = testPlayer.getOneMarble();
            Marble opponentMarble = new Marble(Colour.RED);
            
            board.sendToBase(marble1, testResult);
            board.moveBy(marble1, 10, false, testResult); // Position 10
            
            board.getTrack().get(20).setMarble(opponentMarble); // Position 20
            
            // Act: Swap them
            board.swap(marble1, opponentMarble, testResult);
            
            // Assert: Positions should be swapped
            assertEquals(marble1, board.getTrack().get(20).getMarble(),
                "marble1 should now be at position 20");
            assertEquals(opponentMarble, board.getTrack().get(10).getMarble(),
                "opponentMarble should now be at position 10");
        }
    }
    
    // ==================== BONUS: EDGE CASE TESTS ====================
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Bonus: Test split distance validation")
        void testSplitDistanceValidation() {
            // Valid splits: 1-6
            assertTrue(BoardConstants.isValidSevenSplit(1));
            assertTrue(BoardConstants.isValidSevenSplit(6));
            
            // Invalid splits
            assertFalse(BoardConstants.isValidSevenSplit(0));
            assertFalse(BoardConstants.isValidSevenSplit(7));
        }
        
        @Test
        @DisplayName("Bonus: Test getSplitDistance returns correct value")
        void testGetSplitDistance() {
            // Default split is 3
            assertEquals(3, board.getSplitDistance(),
                "Default split distance should be 3");
            
            // Set to 5
            board.setSplitDistance(5);
            assertEquals(5, board.getSplitDistance(),
                "Split distance should update to 5");
        }
    }
}

/**
 * HOW TO RUN THESE TESTS:
 * 
 * 1. Add JUnit 5 and Mockito to your pom.xml (Maven) or build.gradle:
 *    
 *    <dependency>
 *        <groupId>org.junit.jupiter</groupId>
 *        <artifactId>junit-jupiter</artifactId>
 *        <version>5.9.3</version>
 *        <scope>test</scope>
 *    </dependency>
 *    <dependency>
 *        <groupId>org.mockito</groupId>
 *        <artifactId>mockito-core</artifactId>
 *        <version>5.3.1</version>
 *        <scope>test</scope>
 *    </dependency>
 * 
 * 2. Run in IDE: Right-click to Run 'BoardTest'
 * 3. Run in terminal: mvn test
 * 
 * LEARNING POINTS:
 * - @Nested groups related tests
 * - @DisplayName makes test output readable
 * - Mocking isolates the class under test
 * - Each test is independent (setUp runs before each)
 */
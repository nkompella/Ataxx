package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author Neha Kompella
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" };
    private static final String[]
        GAME2 = { "a7-a5", "g7-e7",
                  "g1-g2", "e5-g5",
                  "g2-e2", "a1-c3"};

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    /** Tests I wrote.
     */
    @Test public void testPiecePositions() {
        Board b1 = new Board();
        makeMoves(b1, GAME2);
        assertEquals(b1.getRpp().size(), 3);
        assertEquals(b1.getBluePiecePositions().size(), 2);
    }

    @Test public void testColors() {
        Board b1 = new Board();
        makeMoves(b1, GAME2);
        assertEquals(b1.whoseMove(), PieceColor.RED);
        assertEquals(b1.whoseMove().opposite(), PieceColor.BLUE);
    }
}

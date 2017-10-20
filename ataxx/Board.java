package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */

import java.util.Observable;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.LinkedList;
import java.util.Formatter;
import java.util.Arrays;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/**
 * An Ataxx board.   The squares are labeled by column (a char value between
 * 'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 * + 2) or by linearized index, an integer described below.  Values of
 * the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 * two layers of border squares, which are always blocked.
 * This artificial border (which is never actually printed) is a common
 * trick that allows one to avoid testing for edge conditions.
 * For example, to look at all the possible moves from a square, sq,
 * on the normal board (i.e., not in the border region), one can simply
 * look at all squares within two rows and columns of sq without worrying
 * about going off the board. Since squares in the border region are
 * blocked, the normal logic that prevents moving to a blocked square
 * will apply.
 * <p>
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index".  This is simply the
 * number of the square in row-major order (counting from 0).
 * <p>
 * Moves on this board are denoted by Moves.
 *
 * @author Neha Kompella
 */
class Board extends Observable {

    /**
     * Number of moves made thus far in the game.
     */
    private static int numMoves;
    /**
     * Number of continuous jumps made thus far in the game.
     */
    private static int numJumps;
    /**
     * Number of passes made thus far in the game.
     */
    private static int numPasses;
    /**
     * Number of red pieces on the board.
     */
    private int redPieces;
    /**
     * Number of blue pieces on the board.
     */
    private int bluePieces;
    /**
     * The last color that was undone.
     */
    private PieceColor lastSet;

    /**
     * Number of squares on a side of the board.
     */
    static final int SIDE = 7;
    /**
     * Length of a side + an artificial 2-deep border region.
     */
    static final int EXTENDED_SIDE = SIDE + 4;

    /**
     * Number of non-extending moves before game ends.
     */
    static final int JUMP_LIMIT = 25;

    /**
     * Index of the left corner of the board.
     */
    static final int STARTING_INDEX = 90;

    /**
     * Number by which the starting index is decremented
     * in order to dump the board to the screen.
     */
    static final int DECREASING = 18;

    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        numMoves = 0;
        _whoseMove = RED;
        redPieces = numPieces(RED);
        bluePieces = numPieces(BLUE);
        clear();
    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        _board = b._board.clone();
        numMoves = b.numMoves();
        _whoseMove = b.whoseMove();
        redPieces = b.redPieces();
        bluePieces = b.bluePieces();
        for (int north = 0; north < b.rpp.size(); north++) {
            rpp.add(b.rpp.get(north));
        }
        for (int north = 0; north < b.bpp.size(); north++) {
            bpp.add(b.bpp.get(north));
        }
        for (int k = 0; k < b.redPieceUndoStack.size(); k++) {
            List<Integer> lst = new ArrayList<>();
            for (int a = 0; k < b.redPieceUndoStack.get(a).size(); k++) {
                lst.add(b.redPieceUndoStack.get(a).get(a));
            }
            redPieceUndoStack.add(lst);
        }
        for (int k = 0; k < b.bluePieceUndoStack.size(); k++) {
            List<Integer> lst = new ArrayList<>();
            for (int a = 0; k < b.bluePieceUndoStack.get(a).size(); k++) {
                lst.add(b.bluePieceUndoStack.get(a).get(a));
            }
            bluePieceUndoStack.add(lst);
        }
        numJumps = b.numJumps();
        for (int north = 0; north < b.numJumpsStack.size(); north++) {
            numJumpsStack.add(b.numJumpsStack.get(north));
        }
        lastSet = b.lastSet;
    }

    /**
     * Given a linearized index of a square, returns its column.
     * @param index is the index of a square on the board.
     */
    public static char col(int index) {
        char row = row(index);
        return (char) (index - EXTENDED_SIDE * (row - '1' + 2) + 'a' - 2);
    }

    /**
     * Given a linearized index of a square, returns its row.
     *
     * @param index is the index of a square on the board.
     */
    public static char row(int index) {
        return (char) (index / EXTENDED_SIDE - 2 + '1');
    }

    /**
     * Return the linearized index of square COL ROW.
     */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /**
     * Return the index of the horizontal reflection of square COL ROW.
     */
    static int reflectHorizontal(char col, char row) {
        char reflectedCol = (char) ('h' - col + 'a' - 1);
        return index(reflectedCol, row);

    }

    /**
     * Return the index of the vertical reflection of square COL ROW.
     */
    static int reflectVertical(char col, char row) {
        char reflectedRow = (char) ('8' - row + '0');
        return index(col, reflectedRow);
    }

    /**
     * Return the index of the diagonal reflection of square COL ROW.
     */
    static int reflectDiagonal(char col, char row) {
        char reflectedCol = (char) ('h' - col + 'a' - 1);
        char reflectedRow = (char) ('8' - row + '0');
        return index(reflectedCol, reflectedRow);
    }

    /**
     * Return the linearized index of the square that is DC columns and DR
     * rows away from the square with index SQ.
     */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions and no blocks.
     */
    void clear() {
        rpp.clear();
        bpp.clear();
        redPieceUndoStack.clear();
        bluePieceUndoStack.clear();

        _whoseMove = RED;

        for (char column = 'a'; column < 'h'; column++) {
            for (char row = '1'; row < '8'; row++) {
                int index = index(column, row);
                _board[index] = EMPTY;
            }
        }

        for (int i = 0; i < _board.length; i++) {
            if (_board[i] != EMPTY) {
                _board[i] = BLOCKED;
            }
        }

        _board[index('a', '7')] = _whoseMove;
        _board[index('a', '1')] = _whoseMove.opposite();
        _board[index('g', '1')] = _whoseMove;
        _board[index('g', '7')] = _whoseMove.opposite();

        rpp.add(index('a', '7'));
        rpp.add(index('g', '1'));
        bpp.add(index('a', '1'));
        bpp.add(index('g', '7'));


        List<Integer> currentRedPiecePositions = new ArrayList<>();
        for (int north = 0; north < rpp.size(); north++) {
            currentRedPiecePositions.add(rpp.get(north));
        }

        List<Integer> currentBluePiecePositions = new ArrayList<>();
        for (int north = 0; north < bpp.size(); north++) {
            currentBluePiecePositions.add(bpp.get(north));
        }

        startUndo();
        redPieceUndoStack.add(currentRedPiecePositions);
        bluePieceUndoStack.add(currentBluePiecePositions);
        numJumpsStack.add(numJumps);
        setChanged();
        notifyObservers();
    }

    /**
     * Returns true if the board has no blue pieces.
     */
    boolean allRed() {
        for (int i = 0; i < _board.length; i++) {
            if (_board[i] == BLUE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the board has no red pieces.
     */
    boolean allBlue() {
        for (int i = 0; i < _board.length; i++) {
            if (_board[i] == RED) {
                return false;
            }
        }
        return true;
    }


    /**
     * Return true iff the game is over: i.e., if neither side has
     * any moves, if one side has no pieces, or if there have been
     * MAX_JUMPS consecutive jumps without intervening extends.
     */
    boolean gameOver() {
        if (numJumps() >= JUMP_LIMIT) {
            return true;
        } else if (allBlue() || allRed()) {
            return true;
        } else if (!canMove(BLUE) && !canMove(RED)) {
            return true;
        }
        return false;
    }

    /**
     * Return number of red pieces on the board.
     */
    int redPieces() {
        return numPieces(RED);
    }

    /**
     * Return number of blue pieces on the board.
     */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /**
     * Return number of COLOR pieces on the board.
     */
    int numPieces(PieceColor color) {
        int count = 0;
        for (int i = 0; i < _board.length; i++) {
            if (_board[i] == color) {
                count++;
            }
        }
        return count;
    }

    /**
     * Increment numPieces(COLOR) by K.
     */
    private void incrPieces(PieceColor color, int k) {
        if (color == RED) {
            redPieces += k;
        } else if (color == BLUE) {
            bluePieces += k;
        }


    }

    /**
     * The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     * '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     * BLOCKED.  Returns the same value as get(index(C, R)).
     */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /**
     * Return the current contents of square with linearized index SQ.
     */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'g', and
     * '1' <= R <= '7'.
     */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /**
     * Set square with linearized index SQ to V.  This operation is
     * undoable.
     */
    private void set(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /**
     * Set square at C R to V (not undoable).
     */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /**
     * Set square at linearized index SQ to V (not undoable).
     */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /**
     * Return true iff MOVE is legal on the current board.
     */
    boolean legalMove(Move move) {
        if (move == null) {
            return false;
        }
        int startIndex = index(move.col0(), move.row0());
        if (move.isPass()) {
            return true;
        }
        if (_board[startIndex] != _whoseMove) {
            return false;
        }
        int index = index(move.col1(), move.row1());
        if (_board[index] != EMPTY) {
            return false;
        }
        return true;
    }

    /**
     * Return a list of all the squares next to that with int index
     * that are empty.
     *
     * @param index takes in the index of a piece on the board.
     */
    public List<Integer> neighborsIndexes(int index) {
        List<Integer> emptyNeigbors = new LinkedList<>();
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                if (i == 0 & j == 0) {
                    int count = 0;
                } else {
                    int direction = neighbor(index, i, j);
                    if (_board[direction] == EMPTY) {
                        emptyNeigbors.add(direction);
                    }
                }
            }
        }
        return emptyNeigbors;
    }


    /**
     * Return true iff player WHO can move, ignoring whether it is
     * that player's move and whether the game is over.
     */
    boolean canMove(PieceColor who) {
        if (who == RED) {
            for (int item : rpp) {
                if (neighborsIndexes(item).size() > 0) {
                    return true;
                }
            }
        } else if (who == BLUE) {
            for (int item : bpp) {
                if (neighborsIndexes(item).size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Return total number of moves and passes since the last
     * clear or the creation of the board.
     */
    int numMoves() {
        return numMoves + numPasses;
    }

    /**
     * Return number of non-pass moves made in the current game since the
     * last extend move added a piece to the board (or since the
     * start of the game). Used to detect end-of-game.
     */
    int numJumps() {
        return numJumps;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));

        }
    }

    /**
     * Return true if there is a block on the square at the given index.
     *
     * @param index is the index of a square on the board.
     */
    boolean blockHere(int index) {
        if (_board[index] == BLOCKED) {
            return true;
        }
        return false;
    }

    /**
     * Return true if there is the square at the given index is empty.
     *
     * @param index is the index of a square on the board.
     */
    boolean emptyHere(int index) {
        if (_board[index] == EMPTY) {
            return true;
        }
        return false;
    }

    /**
     * Return true if there is a piece on the square at the given index.
     *
     * @param index is the index of a square on the board.
     */
    boolean pieceHere(int index) {
        if (_board[index] == RED || _board[index] == BLUE) {
            return true;
        }
        return false;
    }


    /**
     * Make the MOVE on this Board, assuming it is legal.
     */
    void makeMove(Move move) {
        Boolean m = legalMove(move);
        if (move != null) {
            if (m) {
                allMoves.add(move);
                passer(move);
                int indexCur = index(move.col1(), move.row1());
                PieceColor saved = _board[move.fromIndex()];
                lastSet = saved;
                _board[move.toIndex()] = saved;
                if (saved == RED && !rpp.contains(move.toIndex())
                        && !bpp.contains(move.toIndex())) {
                    rpp.add(move.toIndex());
                } else if (saved == BLUE
                        && !bpp.contains(move.toIndex())
                        && !rpp.contains(move.toIndex())) {
                    bpp.add(move.toIndex());
                }
                if (move.isJump()) {
                    numJumps++;
                    _board[move.fromIndex()] = EMPTY;
                    if (saved == RED) {
                        listWork(rpp, move.fromIndex());
                    } else if (saved == BLUE) {
                        listWork(bpp, move.fromIndex());
                    }
                } else {
                    numMoves++;
                    numJumps = 0;
                }
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        if (i == 0 & j == 0) {
                            int a = 0;
                        } else {
                            int check = neighbor(indexCur, i, j);
                            if (pieceHere(check)) {
                                set(check, saved);
                                if (saved == RED
                                        && !rpp.contains(check)) {
                                    rpp.add(check);
                                    listWork(bpp, check);
                                } else if (saved == BLUE
                                        && !bpp.contains(check)) {
                                    bpp.add(check);
                                    listWork(rpp, check);
                                }
                            }
                        }
                    }
                }
                if (saved == RED) {
                    stackAdd(redPieceUndoStack, rpp);
                } else {
                    stackAdd(bluePieceUndoStack, bpp);
                }
                doSomething();
            }
        }
    }

    /**
     * Makes a pass.
     * @param move is the move that will make a pass.
     */
    void passer(Move move) {
        if (move.isPass()) {
            pass();
            return;
        }
    }

    /**
     * Does all the things to do after a move is made.
     */
    void doSomething() {
        numJumpsStack.add(numJumps);
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /**
     * Add elements of a list to the stack.
     * @param stck is the stack to be added to.
     * @param lst is the list with elements to add to a stack.
     */
    void stackAdd(Stack<List<Integer>> stck, List<Integer> lst) {
        List<Integer> currentPiecePositions = new ArrayList<>();
        for (int north = 0; north < lst.size(); north++) {
            currentPiecePositions.add(lst.get(north));
        }
        stck.add(currentPiecePositions);
    }

    /**
     * Removes items from a list if the elements match the check.
     * @param lst refers to the list who's elements will be checked.
     * @param check refers to the check to remove items.
     */
    void listWork(List<Integer> lst, int check) {
        for (int i = 0; i < lst.size(); i++) {
            if (lst.get(i) == check) {
                lst.remove(i);
            }
        }
    }

    /**
     * Return red piece positions.
     */
    public List<Integer> getRpp() {
        return rpp;
    }

    /**
     * Return blue piece positions.
     */
    public List<Integer> getBluePiecePositions() {
        return bpp;
    }


    /**
     * Update to indicate that the current player passes, assuming it
     * is legal to do so.  The only effect is to change whoseMove().
     */
    void pass() {
        assert !canMove(_whoseMove);
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /**
     * Undo the last move.
     */
    void undo() {
        numJumpsStack.pop();
        for (char column = 'a'; column < 'h'; column++) {
            for (char row = '1'; row < '8'; row++) {
                int index = index(column, row);
                _board[index] = EMPTY;
            }
        }
        if (redPieceUndoStack.size() > 1 && RED == lastSet) {
            redPieceUndoStack.pop();
            List<Integer> hiya = redPieceUndoStack.peek();
            rpp = new ArrayList<>();
            for (int i = 0; i < hiya.size(); i++) {
                rpp.add(hiya.get(i));
            }
            for (int i = 0; i < rpp.size(); i++) {
                int toPut = rpp.get(i);
                _board[toPut] = RED;
            }
            if (bluePieceUndoStack.size() == 1) {
                List<Integer> byuh = bluePieceUndoStack.peek();
                for (int i = 0; i < byuh.size(); i++) {
                    bpp.add(byuh.get(i));
                }

                for (int i = 0; i < bpp.size(); i++) {
                    int toPut = bpp.get(i);
                    _board[toPut] = BLUE;
                }
            }
            lastSet = BLUE;
        } else if (bluePieceUndoStack.size() > 1 && BLUE == lastSet) {
            bluePieceUndoStack.pop();
            List<Integer> byuh = bluePieceUndoStack.peek();
            bpp = new ArrayList<>();
            for (int i = 0; i < byuh.size(); i++) {
                bpp.add(byuh.get(i));
            }
            for (int i = 0; i < bpp.size(); i++) {
                int toPut = bpp.get(i);
                _board[toPut] = BLUE;
            }
            if (redPieceUndoStack.size() == 1) {
                List<Integer> hiya = redPieceUndoStack.peek();
                for (int i = 0; i < hiya.size(); i++) {
                    rpp.add(hiya.get(i));
                }
                for (int i = 0; i < rpp.size(); i++) {
                    int toPut = rpp.get(i);
                    _board[toPut] = RED;
                }
            }
            lastSet = RED;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Indicate beginning of a move in the d stack.
     */
    private void startUndo() {

    }

    /**
     * Add an undo action for changing SQ to NEWCOLOR on current
     * board.
     */
    private void addUndo(int sq, PieceColor newColor) {
    }

    /**
     * Return true iff it is legal to place a block at C R.
     */
    boolean legalBlock(char c, char r) {
        int indexToGo = index(c, r);
        if (_board[indexToGo] == EMPTY) {
            return true;
        }
        return false;
    }

    /**
     * Return true iff it is legal to place a block at CR.
     */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Set a block on the square C R and its reflections across the middle
     * row and/or column, if that square is unoccupied and not
     * in one of the corners. Has no effect if any of the squares is
     * already occupied by a block.  It is an error to place a block on a
     * piece.
     */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }

        final int o = 24;
        final int d = 30;
        final int t = 90;
        final int a = 96;

        char upDown = (char) ('8' - r + '0');
        char rightLeft = (char) ('h' - c + 'a' - 1);
        int index = index(c, r);
        int verticalIndex = reflectVertical(c, r);
        int horizontalIndex = reflectHorizontal(c, r);
        int diagonalIndex = reflectDiagonal(c, r);
        if (legalBlock(c, r) && index != o
                && index != d && index != t && index != a) {
            _board[index] = BLOCKED;
            if (legalBlock(rightLeft, r)) {
                _board[horizontalIndex] = BLOCKED;
            }
            if (legalBlock(c, upDown)) {
                _board[verticalIndex] = BLOCKED;
            }
            if (legalBlock(rightLeft, upDown)) {
                _board[diagonalIndex] = BLOCKED;
            }

        }
        setChanged();
        notifyObservers();
    }

    /**
     * Place a block at CR.
     */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Return a list of all moves made since the last clear (or start of
     * game).
     */
    List<Move> allMoves() {
        return allMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /**
     * Return a text depiction of the board (not a dump).  If LEGEND,
     * supply row and column numbers around the edges.
     */
    String toString(boolean legend) {
        String[] copy = new String[_board.length];
        for (int i = 0; i < _board.length; i++) {
            if (_board[i] == null) {
                copy[i] = null;
            } else if (_board[i].equals(RED)) {
                copy[i] = "r";
            } else if (_board[i].equals(BLUE)) {
                copy[i] = "b";
            } else if (_board[i].equals(BLOCKED)) {
                copy[i] = "X";
            } else if (_board[i].equals(EMPTY)) {
                copy[i] = "-";
            }
        }

        int start = STARTING_INDEX;
        System.out.println("===");
        for (int i = 0; i < SIDE; i++) {
            Formatter ft = new Formatter();
            ArrayList<String> ll = new ArrayList<>();
            for (int j = 0; j < SIDE; j++) {
                ll.add(copy[start]);
                start += 1;
            }
            ft.format("  %s %s %s %s %s %s %s", ll.get(0), ll.get(1), ll.get(2),
                    ll.get(3), ll.get(4), ll.get(5), ll.get(6));
            System.out.println(ft.toString());
            start -= DECREASING;
        }
        System.out.println("===");

        return "a";
    }

    /**
     * For reasons of efficiency in copying the board,
     * we use a 1D array to represent it, using the usual access
     * algorithm: row r, column c => index(r, c).
     * <p>
     * Next, instead of using a 7x7 board, we use an 11x11 board in
     * which the outer two rows and columns are blocks, and
     * row 2, column 2 actually represents row 0, column 0
     * of the real board.  As a result of this trick, there is no
     * need to special-case being near the edge: we don't move
     * off the edge because it looks blocked.
     * <p>
     * Using characters as indices, it follows that if 'a' <= c <= 'g'
     * and '1' <= r <= '7', then row c, column r of the board corresponds
     * to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     * re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION].
     */
    private final PieceColor[] _board;

    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;

    /**
     * Stack of all the positions at which there were red pieces
     * after each move.
     */
    private Stack<List<Integer>> redPieceUndoStack = new Stack<>();

    /**
     * Stack of all the positions at which there were blue pieces
     * after each move.
     */
    private Stack<List<Integer>> bluePieceUndoStack = new Stack<>();

    /**
     * List of all the indexes at which there are red pieces on the board.
     */
    private List<Integer> rpp = new ArrayList<>();

    /**
     * List of all the indexes at which there are blue pieces on the board.
     */
    private List<Integer> bpp = new ArrayList<>();

    /**
     * Stack of the number of continuous jumps there are after each move.
     */
    private Stack<Integer> numJumpsStack = new Stack<>();

    /**
     * List of all moves made.
     */
    private List<Move> allMoves = new ArrayList<>();
}

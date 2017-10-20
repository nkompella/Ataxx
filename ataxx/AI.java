package ataxx;

import java.util.List;

import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

/**
 * A Player that computes its own moves.
 *
 * @author Neha Kompella
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 4;
    /**
     * A position magnitude indicating a win (for red if positive, blue
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            if (whichPiecePositions(1).size() > 0) {
                return Move.pass();
            } else {
                return null;
            }
        } else {
            _lastFoundMove = null;
            Move move = findMove();
            if (move == null) {
                move = null;
            }
            String str = myColor().toString();
            System.out.println(str + " moves " + move.toString() + ".");
            return move;
        }
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        return _lastFoundMove;
    }

    /**
     * Used to communicate best moves found by findMove, when asked for.
     */
    private Move _lastFoundMove;

    /**
     * Determines the indexes at which there are pieces of color sense (where
     * if sense = -1, color is blue and if sense = 1, color is red.
     *
     * @param sense refers to the color of whose piecePositions are needed
     * @return either redPiecePositions or bluePiecePositions.
     */
    private List<Integer> whichPiecePositions(int sense) {
        if (sense == 1 && myColor() == RED
                || sense == -1 && myColor() == BLUE) {
            return board().getRpp();
        } else if (sense == 1 && myColor() == BLUE
                || sense == -1 && myColor() == RED) {
            return board().getBluePiecePositions();
        } else {
            return null;
        }
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value >= BETA if SENSE==1,
     * and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     * DEPTH levels before using a static estimate.
     */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            return staticScore(board);
        }
        List<Integer> piecePositions = whichPiecePositions(sense);
        List<Integer> emptyNeighbors;
        int bestSoFar = INFTY * -sense;
        for (int i = 0; i < piecePositions.size(); i++) {
            int indexCurrent = piecePositions.get(i);
            emptyNeighbors = board.neighborsIndexes(indexCurrent);
            char col0 = Board.col(indexCurrent);
            char row0 = Board.row(indexCurrent);
            for (int j = 0; j < emptyNeighbors.size(); j++) {
                int moveTo = emptyNeighbors.get(j);
                char col1 = Board.col(moveTo);
                char row1 = Board.row(moveTo);
                Move move = Move.move(col0, row0, col1, row1);
                boolean b = board.legalMove(move);
                if (col1 >= 'a' && col1 <= 'g'
                        && row1 <= '7' && row1 >= '1'
                        && board.legalMove(move)) {
                    board.makeMove(col0, row0, col1, row1);
                    int d = depth - 1;
                    int o = -sense;
                    int a = findMove(board, d, saveMove, o, alpha, beta);
                    if (sense == 1) {
                        if (a >= bestSoFar) {
                            bestSoFar = a;
                            if (saveMove) {
                                _lastFoundMove = move;
                            }
                            alpha = max(alpha, a);
                            if (beta <= alpha) {
                                break;
                            }
                            saveMove = false;
                        }
                    } else {
                        saveMove = false;
                        if (a <= bestSoFar) {
                            bestSoFar = a;
                            beta = min(beta, a);
                            if (beta >= alpha) {
                                break;
                            }
                        }
                    }

                    board.undo();
                }
            }
        }

        return bestSoFar;

    }

    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        if (board.gameOver()) {
            PieceColor opp = myColor().opposite();
            if (game().winner(board).equals(myColor().toString())) {
                return INFTY;
            } else if (game().winner(board).equals(opp.toString())) {
                return -INFTY;
            } else if (game().winner(board).equals("Draw")) {
                return 0;
            }
        }
        PieceColor opp = myColor().opposite();
        return board.numPieces(myColor()) - board.numPieces(opp);
    }
}


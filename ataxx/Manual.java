package ataxx;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Neha Kompella
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command command = game().getMoveCmnd(myColor().toString() + ": ");
        String[] operands = command.operands();
        char first = operands[0].charAt(0);
        char second = operands[1].charAt(0);
        char third = operands[2].charAt(0);
        char fourth = operands[3].charAt(0);
        Move move = Move.move(first, second, third, fourth);
        return move;
    }
}




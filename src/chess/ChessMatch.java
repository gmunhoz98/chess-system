package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChessMatch {
    private Board board;
    private Color currentPlayer;
    private int turn;
    private boolean check;
    private boolean checkMate;
    private ChessPiece enPassantVulnerable;
    private ChessPiece promoted;

    public ChessPiece getPromoted() {
        return promoted;
    }

    public ChessPiece getEnPassantVulnerable() {
        return enPassantVulnerable;
    }

    public boolean getCheckMate() {
        return checkMate;
    }

    private List<Piece> piecesOnTheBoard = new ArrayList<>();

    private List<Piece> capturedPieces = new ArrayList<>();

    public Color getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean getCheck() {
        return check;
    }

    public int getTurn() {
        return turn;
    }

    public ChessMatch() {
        board = new Board(8, 8);
        turn = 1;
        currentPlayer = Color.WHITE;
        initialSetup();
    }

    public ChessPiece[][] getPieces() {
        ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];

        for (int i = 0; i < board.getRows(); i++) {
            for (int j = 0; j < board.getColumns(); j++) {
                mat[i][j] = (ChessPiece) board.piece(i, j);
            }
        }
        return mat;
    }

    public boolean[][] possibleMoves(ChessPosition sourcePosition) {
        Position position = sourcePosition.toPosition();
        validateSourcePosition(position);

        return board.piece(position).possibleMoves();
    }

    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();

        validateSourcePosition(source); //Valida a posição de inicio
        validateTargetPosition(source, target); //Valida a posição de destino
        Piece capturedPiece = makeMove(source, target);
        if (testCheck(currentPlayer)) {
            undoMove(source, target, capturedPiece);
            throw new ChessException("Não pode se por em cheque");
        }

        ChessPiece movedPiece = (ChessPiece) board.piece(target);

        // Promotion Peão
        promoted = null;
        if (movedPiece instanceof Peao) {
            if ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0) || (movedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
                promoted = (ChessPiece) board.piece(target);
                promoted = replacePrometedPiece("Q");
            }
        }

        check = (testCheck(opponent(currentPlayer))) ? true : false;

        if (testCheckMate(opponent(currentPlayer))) {
            checkMate = true;
        } else {
            nextTurn();
        }

        // EnPassant
        if (movedPiece instanceof Peao && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
            enPassantVulnerable = movedPiece;
        } else {
            enPassantVulnerable = null;
        }

        return (ChessPiece) capturedPiece;
    }

    private Piece makeMove(Position source, Position target) {
        ChessPiece p = (ChessPiece) board.removePiece(source);
        p.increaseMoveCount();
        Piece capturedPiece = board.removePiece(target);
        board.placePiece(p, target);

        if (capturedPiece != null) {
            piecesOnTheBoard.remove(capturedPiece);
            capturedPieces.add(capturedPiece);
        }

        // Castling/Roque pequeno
        if (p instanceof Rei && target.getColumn() == source.getColumn() + 2) {
            Position sourceTorre = new Position(source.getRow(), source.getColumn() + 3);
            Position targetTorre = new Position(source.getRow(), source.getColumn() + 1);
            ChessPiece torre = (ChessPiece) board.removePiece(sourceTorre);
            board.placePiece(torre, targetTorre);
            torre.increaseMoveCount();
        }

        // Castling/Roque grande
        if (p instanceof Rei && target.getColumn() == source.getColumn() - 2) {
            Position sourceTorre = new Position(source.getRow(), source.getColumn() - 4);
            Position targetTorre = new Position(source.getRow(), source.getColumn() - 1);
            ChessPiece torre = (ChessPiece) board.removePiece(sourceTorre);
            board.placePiece(torre, targetTorre);
            torre.increaseMoveCount();
        }

        // EnPassant
        if (p instanceof Peao) {
            if (source.getColumn() != target.getColumn() && capturedPiece == null) {
                Position pawnPosition;
                if (p.getColor() == Color.WHITE) {
                    pawnPosition = new Position(target.getRow() + 1, target.getColumn());
                } else {
                    pawnPosition = new Position(target.getRow() - 1, target.getColumn());
                }
                capturedPiece = board.removePiece(pawnPosition);
                capturedPieces.add(capturedPiece);
                piecesOnTheBoard.remove(capturedPiece);
            }
        }

        return capturedPiece;
    }

    //Promotion
    public ChessPiece replacePrometedPiece(String type) {
        if (promoted == null) {
            throw new IllegalStateException("Tem que haver alguma peça para promover");
        }
        if (!type.equals("B") && !type.equals("C") && !type.equals("Q") && !type.equals("T")) {
            return promoted;
        }
        Position pos = promoted.getChessPosition().toPosition();
        Piece p = board.removePiece(pos);
        piecesOnTheBoard.remove(p);

        ChessPiece newPiece = newPiece(type, promoted.getColor());
        board.placePiece(newPiece, pos);
        piecesOnTheBoard.add(newPiece);

        return newPiece;
    }

    private ChessPiece newPiece(String type, Color color) {
        if (type.equals("B")) return new Bispo(board, color);
        if (type.equals("C")) return new Cavalo(board, color);
        if (type.equals("T")) return new Torre(board, color);
        return new Rainha(board, color);
    }

    private void undoMove(Position source, Position target, Piece capturedPiece) {
        ChessPiece p = (ChessPiece) board.removePiece(target);
        p.decreaseMoveCount();
        board.placePiece(p, source);

        if (capturedPiece != null) {
            board.placePiece(capturedPiece, target);
            capturedPieces.remove(capturedPiece);
            piecesOnTheBoard.add(capturedPiece);
        }

        // Castling/Roque pequeno
        if (p instanceof Rei && target.getColumn() == source.getColumn() + 2) {
            Position sourceTorre = new Position(source.getRow(), source.getColumn() + 3);
            Position targetTorre = new Position(source.getRow(), source.getColumn() + 1);
            ChessPiece torre = (ChessPiece) board.removePiece(targetTorre);
            board.placePiece(torre, sourceTorre);
            torre.decreaseMoveCount();
        }

        // Castling/Roque grande
        if (p instanceof Rei && target.getColumn() == source.getColumn() - 2) {
            Position sourceTorre = new Position(source.getRow(), source.getColumn() - 4);
            Position targetTorre = new Position(source.getRow(), source.getColumn() - 1);
            ChessPiece torre = (ChessPiece) board.removePiece(targetTorre);
            board.placePiece(torre, sourceTorre);
            torre.decreaseMoveCount();
        }

        // EnPassant
        if (p instanceof Peao) {
            if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
                ChessPiece pawn = (ChessPiece) board.removePiece(target);
                Position pawnPosition;
                if (p.getColor() == Color.WHITE) {
                    pawnPosition = new Position(3, target.getColumn());
                } else {
                    pawnPosition = new Position(4, target.getColumn());
                }
                board.placePiece(pawn, pawnPosition);
            }
        }
    }

    private void validateSourcePosition(Position position) {
        if (!board.thereIsAPiece(position)) {
            throw new ChessException("Não existe peça nessa posição");
        }
        if (currentPlayer != ((ChessPiece) board.piece(position)).getColor()) {
            throw new ChessException("Essa peça não é sua!!");
        }
        if (!board.piece(position).isThereAnyPossibleMove()) {
            throw new ChessException("Não existe movimentos possiveis para a peça escolhida!");
        }
    }

    private void validateTargetPosition(Position source, Position target) {
        if (!board.piece(source).possibleMove(target)) {
            throw new ChessException("A peça escolhida não pode se mover para a posição escolhida");
        }
    }

    private Color opponent(Color color) {
        return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    private ChessPiece King(Color color) {
        List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).collect(Collectors.toList());
        for (Piece p : list) {
            if (p instanceof Rei) {
                return (ChessPiece) p;
            }
        }
        throw new IllegalStateException("Não existe Rei da cor " + color + " no tabuleiro");
    }

    private boolean testCheck(Color color) {
        Position KingPosition = King(color).getChessPosition().toPosition();
        List<Piece> opponentPieces = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
        for (Piece p : opponentPieces) {
            boolean[][] mat = p.possibleMoves();
            if (mat[KingPosition.getRow()][KingPosition.getColumn()]) {
                return true;
            }
        }
        return false;
    }

    private boolean testCheckMate(Color color) {
        if (!testCheck(color)) {
            return false;
        }
        List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).collect(Collectors.toList());
        for (Piece p : list) {
            boolean[][] mat = p.possibleMoves();
            for (int i = 0; i < board.getRows(); i++) {
                for (int j = 0; j < board.getColumns(); j++) {
                    if (mat[i][j]) {
                        Position source = ((ChessPiece) p).getChessPosition().toPosition();
                        Position target = new Position(i, j);

                        Piece capturedPiece = makeMove(source, target);
                        boolean testCheck = testCheck(color);
                        undoMove(source, target, capturedPiece);

                        if (!testCheck) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void placeNewPiece(char column, int row, ChessPiece piece) {
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
        piecesOnTheBoard.add(piece);
    }

    private void nextTurn() {
        turn++;
        currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    private void initialSetup() {
        placeNewPiece('e', 1, new Rei(board, Color.WHITE, this));
        placeNewPiece('d', 1, new Rainha(board, Color.WHITE));
        placeNewPiece('a', 1, new Torre(board, Color.WHITE));
        placeNewPiece('h', 1, new Torre(board, Color.WHITE));
        placeNewPiece('c', 1, new Bispo(board, Color.WHITE));
        placeNewPiece('f', 1, new Bispo(board, Color.WHITE));
        placeNewPiece('b', 1, new Cavalo(board, Color.WHITE));
        placeNewPiece('g', 1, new Cavalo(board, Color.WHITE));
        placeNewPiece('a', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('b', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('c', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('d', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('e', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('f', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('g', 2, new Peao(board, Color.WHITE, this));
        placeNewPiece('h', 2, new Peao(board, Color.WHITE, this));

        placeNewPiece('e', 8, new Rei(board, Color.BLACK, this));
        placeNewPiece('d', 8, new Rainha(board, Color.BLACK));
        placeNewPiece('a', 8, new Torre(board, Color.BLACK));
        placeNewPiece('h', 8, new Torre(board, Color.BLACK));
        placeNewPiece('c', 8, new Bispo(board, Color.BLACK));
        placeNewPiece('f', 8, new Bispo(board, Color.BLACK));
        placeNewPiece('b', 8, new Cavalo(board, Color.BLACK));
        placeNewPiece('g', 8, new Cavalo(board, Color.BLACK));
        placeNewPiece('a', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('b', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('c', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('d', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('e', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('f', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('g', 7, new Peao(board, Color.BLACK, this));
        placeNewPiece('h', 7, new Peao(board, Color.BLACK, this));
    }
}

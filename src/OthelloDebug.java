import java.util.Scanner;

public class OthelloDebug {

        public static void main(String[] args) {
            OthelloDebug debug = new OthelloDebug();
            debug.start();
        }

    private Othello game;
    private Scanner scanner;

    public OthelloDebug() {
        this.game = new Othello();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Othelloを開始します。");

        while (true) {
            drawBoard();
            boolean turn = game.getCurrentTurn();
            System.out.println((turn ? "●" : "◯") + "の手番です。");

            int[][] placeable = game.searchPlaceable(game.getBoard());
            if (placeable.length == 0) {
                int winner = game.checkWinner();
                System.out.println("ゲーム終了！勝者は" + (winner == 1 ? "●" : "◯") + "です。");
                break;
            }

            System.out.print("座標を入力してください（例：11）アルファベットのある列が後です：");
            String input = scanner.next();
            if (input.equals("0")) {
                System.out.println("ゲームを終了します。");
                break;
            }

            int x = input.charAt(0) - '1';
            int y = input.charAt(1) - '1';
            int[][] place = {{x, y}};


            boolean isValid = false;
            for (int i = 0; i < placeable.length; i++) {
                if (placeable[x][y] == 1) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                System.out.println("そこには置けません。");
                continue;
            }

            game.applyMove(place[0], turn);
        }
    }

    private void drawBoard() {
        int[][] board = game.getBoard();
        System.out.println("  1 2 3 4 5 6 7 8");
        for (int i = 0; i < 8; i++) {
            System.out.print((i + 1) + " ");
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == 0) {
                    if (isPlaceable(i, j)) {
                        System.out.print("□ ");
                    } else {
                        System.out.print("■ ");
                    }
                } else if (board[i][j] == 1) {
                    System.out.print("● ");
                } else {
                    System.out.print("◯ ");
                }
            }
            System.out.println((i + 1));
        }
        System.out.println("  a b c d e f g h");
    }

    private boolean isPlaceable(int x, int y) {
        int[][] placeable = game.searchPlaceable(game.getBoard());

            if (placeable[x][y] == 1) {
                return true;
            }
        return false;
    }
}
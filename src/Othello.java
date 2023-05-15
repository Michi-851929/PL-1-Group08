public class Othello {
    //マスの状態
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = -1;
    int[][] directions = {
        {-1,  0},
        { 1,  0},
        { 0, -1},
        { 0,  1},
        {-1, -1},
        { 1,  1},
        {-1,  1},
        { 1, -1}
    };//八方向探索用の配列
    
    private Player[] players = new Player[2];
    private boolean currentTurn; // 現在の手番（true: 黒番、false: 白番）なんかひっくり返すの混同しそうだし
    private int[][] board = new int[8][8]; // 盤面情報（0: 石が置かれていない、1: 黒の石が置かれている、-1: 白の石が置かれている）

    

    public Othello(Player me, Player you) {
        //盤面の初期化
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                board[i][j] = EMPTY;
            }
        }
        board[3][3] = BLACK;
        board[4][4] = BLACK;
        board[3][4] = WHITE;
        board[4][3] = WHITE;
        
        //プレイヤ情報の初期化
        players[0] = me;
        players[1] = you;
        
        currentTurn = true; // 黒からスタートする
    }
    
    public Player[] getPlayers()
    {
    	return players;
    }

    public boolean getCurrentTurn() {
        // 現在の手番
        return currentTurn;
    }

    public int[][] getBoard() {
        // 盤面情報を出力する
        return board;
    }


    /*
    1.もしマスが空白でなければ、スキップ。
    2.8方向について以下の手順を繰り返す。
    その方向にある石を順に数え、自分の石があるか空白マスに到達するまで数え続ける。
    自分の石がある場合は、空白ではに部分あったそれまでの石を相手の石とみなし、自分の石までの間に空白マスがある場合はそのマスに石を置ける場所としてplaceable配列に1を設定する。

    */

    public boolean[][] searchPlaceable() {
        boolean[][] placeable = new boolean[8][8];

        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
            	placeable[i][j] = false;
                if(board[i][j] != EMPTY) {
                	continue; // 空白マスでない場合はスキップ
                }
                for (int[] dir : directions) {
                    int x = i + dir[0];
                    int y = j + dir[1];
                    int count = 0;
                    while (x >= 0 && x < 8 && y >= 0 && y < 8) {
                        if (board[x][y] == 0) {
                            break; // 空白マスに到達した場合は終了
                        }
                        if (board[x][y] == (currentTurn ? 1 : -1)) {
                            if (count > 0) {
                                placeable[i][j] = true; // おける場所
                            }
                            break; // 自分の石がある場合は終了
                        } else {
                            count++;
                        }
                        x += dir[0];
                        y += dir[1];
                    }
                }
            }
        }
        return placeable;
    }


    public int checkWinner() {
        int blackCount = 0;
        int whiteCount = 0;
        int emptyCount = 0;
        
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                if(board[i][j] == BLACK) {
                    blackCount++;
                }
                else if(board[i][j] == WHITE) {
                    whiteCount++;
                }
                else{
                    emptyCount++;
                }
            }
        }
        if(emptyCount == 0 || blackCount == 0 || whiteCount == 0) {
            // 全てのマスが埋まっている、または黒の石か白の石しかない場合、どちらかが勝利
            if(blackCount > whiteCount) {
                return 1;
            }
            else if(blackCount < whiteCount) {
                return -1;
            }
            else{
                return 0; // 引き分け
            }
        } else {
            return 2; // ゲーム続行
        }
    }

    public boolean[][] applyMove(int[] move) {
        // 指し手の座標を取得
        int i = move[0];
        int j = move[1];
        boolean[][] change = new boolean[8][8];

        // 石の色を設定
        int color = (currentTurn ? BLACK : WHITE);

        // 盤面に石を置く
        try {
            if(i >= 0 && i < 8 && j >= 0 && j < 8 && board[i][j] == EMPTY) {
                board[i][j] = color;
                for(int inc1 = 0; inc1 < 8; inc1++) {
                	for(int inc2 = 0; inc2 < 8; inc2++) {
                		change[inc1][inc2] = false;
                	}
                }
                
                // 8方向について裏返せる石を探索し、裏返す
                for(int[] dir : directions) {
                    int x = i + dir[0];
                    int y = j + dir[1];
                    int count = 0;
                    while(x >= 0 && x < 8 && y >= 0 && y < 8) {
                        if(board[x][y] == 0) {
                            break; // 空白マスに到達した場合は終了
                        }
                        if(board[x][y] == color) {
                            if(count > 0) {
                                // 裏返す
                                x = i + dir[0];
                                y = j + dir[1];
                                for (int k = 0; k < count; k++) {
                                    board[x][y] = color;
                                    change[x][y] = true;
                                    x += dir[0];
                                    y += dir[1];
                                }
                            }
                            break; // 自分の石がある場合は終了
                        }
                        else {
                            count++;
                        }
                        x += dir[0];
                        y += dir[1];
                    }
                }
            }
            else {
                throw new Exception("The place has already had a stone.");
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        // 現在の手番を更新する
        currentTurn = !currentTurn;
        
        return change;
    }
}

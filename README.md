# PL-1-Group08

## Othelloクラス
以下のものを実装しました。(本多)

・Othello()
実行時に初期盤面がセットされます。

・boolean getCurrentTurn()
現在のターンを確認できます。（true: 黒番、false: 白番）

・int[][] getBoard()
盤面情報を確認できます。（0: 石が置かれていない、1: 黒の石が置かれている、-1: 白の石が置かれている）

・public int[][] searchPlaceable(int[][] board)
盤面を入力するとおける場所は1,置けない場所は0を配置した8×8 配列をかえします。booleanにしてもいいかもしれません。

・int checkWinner()
ボードの情報のうち空きマス(emptyCount)白の数(whiteCount)黒の数(emptyCount)をカウントし勝敗を判定します。（1:黒の勝利、0:引き分け、-1;白の勝利、2:試合続行）を返します。

・void applyMove(int[] move, boolean isBlack)
move[0]=x,move[1]=yとして指し手を判断し、オセロの手法どおりに石を裏返します。妥当な手が指されることのみ想定しています。盤面を更新したのちcurrentTurnも書き換えます。

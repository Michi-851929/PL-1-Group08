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

## Clientクラス

・コンストラクタ:Client　玖津見

・画面遷移メソッド:Void　玖津見

・待ちプレイヤ問い合わせメソッド:String[]　本多

・サーバに接続:String[]　本多

・接続確認応答:boolean　本多

・盤面更新:Void　玖津見

・自分の番:Void　高田

・相手の番:Void　高田

・指し手を受け付ける:int[]　玖津見

・対局終了:Void　高田

## サーバーとクライアントの間の通信

int[2]

{0-7, 0-7} → 打った盤面

{0-7+8, 0-7} → 打った盤面(試合終了時)

{16, 0} → 接続確認応答(投了)

{16, 1} → 接続確認応答(継続) , サーバからクライアントへの確認

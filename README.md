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

・public void connectToServer()(本多)

サーバの10000番ポートに接続し2つのスレッドを起動します。1つはsendHeartbeatメソッドでサーバに1秒ごとにハートビートを送信します。もう1つは最後に押されたボタンを0.5秒ごとに監視し続け変更があった際にサーバに最新の値を送信します。最後は接続を終了するかどうかの判断をするwhile文が存在します。レスポンスのタイムアウトやサーバに接続失敗するとソケットを閉じ、これに反応してほかのスレッドも終了します。

ほかのメソッドにかかわるので詳しく書きますがボタンを監視して変更があった際に送信するようにしているのは、エラーの原因になりうる短時間の間の連打に対応できることが一番大きいです。試験的に0.5秒間隔にしていますがもっと短くてもよいかもしれません。

・private static void sendHeartbeat(Socket socket)(本多)

DataOutputStreamを用いて1を送り続けます。何かあった時に際して反応して0を送れるようにする機能は要実装です。

・private static void sendCommand(Socket socket, int[] command)

ObjectOutputStreamで入力された値をサーバに送信します。

・private static int receiveResponse(Socket socket)

DataInputStreamでサーバから受け取った値をそのまま返り値とします。

・boolean isEqual(int a[], int b[])(本多)

2つの配列を比較して同じ時にtrue違うときにfalseを出力します。

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

### ポート

10001:空き部屋確認

10000:それ以外

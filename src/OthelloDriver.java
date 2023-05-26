import java.io.*;

public class OthelloDriver {
	public static void main (String [] args) throws Exception{
		Player p1 = new Player("test1", true, 600000);
		Player p2 = new Player("test2", false, 600000);
		Othello game = new Othello(p1, p2); //初期化
		System.out.println("テスト１：Othelloクラスのオブジェクトを初期化した結果：");
		/*printStatus(game);
		printGrids(game);
		while(true){
			System.out.println("石を置く場所(数字またはpass)をキーボードで入力してください");
			String s = r.readLine();//文字列の入力
			System.out.println(s + " が入力されました。手番は " + game.getTurn() + " です。");
			game.putStone(Integer.parseInt(s), game.getTurn(), true);
			printStatus(game);
			printGrids(game);
			System.out.println("手番を変更します。¥n");
			game.changeTurn();
			}
		}*/
		//状態を表示する
	public static void printStatus(Othello game) {
		System.out.println("getCurrentTurn出力:" + game.getCurrentTurn());
		System.out.println("checkWinner出力:" + game.checkWinner());
	}
	/*
	 * //テスト用に盤面を表示する
	 * public static void printGrids(Othello game){
	 * String [] grids = game.getGrids();
	 * int row = game.getRow();
	 * System.out.println("getRow出力：" + row);
	 * System.out.println("Gridsテスト出力：(8要素ごとに改行)");
	 * for(int i = 0 ; i < row * row ; i++){
	 * System.out.print(grids[i] + " ");
	 * if(i % row == row - 1){
	 * System.out.print("¥n");
	 * }
	 * }
	 * }
	 */
}
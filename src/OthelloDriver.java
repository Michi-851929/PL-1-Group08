import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OthelloDriver {
	public static void main (String [] args) throws Exception{
		Player p1 = new Player("test1", true, 600000);
		Player p2 = new Player("test2", false, 600000);
		Othello game = new Othello(p1, p2); //初期化
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in), 1);
		System.out.println("テスト１：Othelloクラスのオブジェクトを初期化した結果：");
		printStatus(game);
		printGrids(game);
		int[] in = new int[2];
		while(true){
			System.out.println("石を置く場所(数字またはpass)をキーボードで入力してください");
			String s = r.readLine();//文字列の入力
			System.out.println(s + " が入力されました。手番は " + game.getCurrentTurn() + " です。(trueが黒番falseが白番)");
			in[0] = Integer.parseInt(s)/10;
			in[1] = Integer.parseInt(s)%10;
			game.applyMove(in);
			printStatus(game);
			printGrids(game);
			System.out.println("手番を変更します。");
			}
		}
	
		//状態を表示する
	public static void printStatus(Othello game) {
		System.out.println("getCurrentTurn出力:" + game.getCurrentTurn());
		System.out.println("checkWinner出力:" + game.checkWinner());
	}
	
	//テスト用に盤面を表示する
	public static void printGrids(Othello game){
		System.out.println("盤面テスト出力：(8要素ごとに改行)");
		int i, j;
		int[][] board = new int[8][8];
		boolean[][] board2 = new boolean[8][8];
		board = game.getBoard();
		board2 = game.searchPlaceable();
		int x = 0;
		boolean y = false;
		for(i=0; i<8; i++) {
			for(j=0; j<8; j++) {
				x = board[i][j];
				y = board2[i][j];
				if(x == 0) {
					if(y == false) {
						System.out.print("□");
					}
					else {
						System.out.print("■");
					}
				}
				else if(x == -1) {
					System.out.print("〇");
				}
				else if(x == 1) {
					System.out.print("●");
				}
			}
			System.out.print("\n");
		}
	}
	
}
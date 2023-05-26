
public class PlayerDriver {

	public static void main(String[] args) throws Exception{
		/*パターン1*/
		System.out.println("テスト1");
		Player player1 = new Player("test1", true, 600000);
		System.out.println("コンストラクタの引数は\"test1\", true, 600000");
		System.out.println("getPlayerName出力: " + player1.getPlayerName());
		System.out.println("isFirstMover出力: " + player1.isFirstMover());
		System.out.println("getLeftTime出力: " + player1.getLeftTime());
		System.out.println("setNameで「black」を入力します");
		player1.setLeftTime(300000);
		System.out.println("getLeftTime出力: " + player1.getLeftTime());
		
		/*パターン2*/
		System.out.println("テスト2");
		Player player2 = new Player("test2", false, 300000);
		System.out.println("コンストラクタの引数は\"test2\", false, 300000");
		System.out.println("getPlayerName出力: " + player2.getPlayerName());
		System.out.println("isFirstMover出力: " + player2.isFirstMover());
		System.out.println("getLeftTime出力: " + player2.getLeftTime());
		System.out.println("setNameで「black」を入力します");
		player2.setLeftTime(100000);
		System.out.println("getLeftTime出力: " + player2.getLeftTime());
	}
}
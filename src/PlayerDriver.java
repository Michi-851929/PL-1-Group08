public class PlayerDriver {

	public static void main(String[] args) throws Exception{
		
		Player player1 = new Player("test1", true, 600000);
		System.out.println("コンストラクタの引数は\"test1\", true, 600000");
		System.out.println("getPlayerName出力: " + player1.getPlayerName());
		System.out.println("isFirstMover出力: " + player1.isFirstMover());
		System.out.println("getLeftTime出力: " + player1.getLeftTime());
		System.out.println("setLeftTimeで「300000」を入力します");
		player1.setLeftTime(300000);
		System.out.println("getLeftTime出力: " + player1.getLeftTime());
		
	}
	
}
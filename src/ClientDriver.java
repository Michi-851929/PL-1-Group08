import java.util.Scanner;

public class ClientDriver {

	public static void main(String[] args) {
		Player player1 = new Player("先攻プレイヤ", true, 10 * 60 * 1000);
		Player player2 = new Player("後攻プレイヤ", false, 10 * 60 * 1000);
		Client client = new Client("Client Test");
		Scanner stdin = new Scanner(System.in);
		client.othello = new Othello(player1, player2);
		System.out.println("Client Test Start");
		System.out.println("Title Phase Displayed");
		stdin.nextInt();
		System.out.println("Battle Phase Displayed");
		client.changePhase(1);
		client.reloadDisplay(client.getCommand());
		stdin.nextInt();
		System.out.println("Result Phase Displayed");
		client.endmsg1 = "test";
		client.endmsg2 = "message";
		client.changePhase(2);

	}

}

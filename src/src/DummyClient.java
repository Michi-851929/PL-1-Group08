package src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class DummyClient {

	public static void main(String[] args) {
		Socket socket1 = null;// 空き部屋確認用
		Socket socket2 = null;// それ意外用
		// 空き部屋確認
		System.out.println("名前を入力してください");

		Scanner scanner = new Scanner(System.in);
		String myname = scanner.next();

		System.out.println("希望時間を入力してください");
		int time = scanner.nextInt();

		try {
			socket1 = new Socket("localhost", 10001);
			PrintWriter out = new PrintWriter(socket1.getOutputStream(), true);
			DataInputStream in = new DataInputStream(socket1.getInputStream());
			int heartbeat = 1; // ハートビートメッセージ

			out.println(heartbeat); // ハートビートメッセージを送信

			int vacantRoom[] = { -1, -1, -1 };
			// サーバからのレスポンスをパースしてint配列に格納
			vacantRoom[0] = in.readInt();
			vacantRoom[1] = in.readInt();
			vacantRoom[2] = in.readInt();
			System.out.println("空き状況:" + vacantRoom[0] + vacantRoom[1] + vacantRoom[2]);

		} catch (IOException e) {
			System.out.println("Error connecting to server: " + e.getMessage());
		} finally {
			try {
				socket1.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}

		// マッチング
		boolean matching = true; // 投了でfalseに
		try {
			socket2 = new Socket("localhost", 10000);
			DataOutputStream dos = new DataOutputStream(socket2.getOutputStream());
			dos.writeUTF(myname);
			dos.writeInt(time);
			dos.flush();
			System.out.println("サーバーに test/1 を送信");

			// プレイヤ名とルーム番号を受信する
			InputStream in = socket2.getInputStream();
			DataInputStream dis = new DataInputStream(in);

			String opponentName = null;
			int heartbeat_0;
			int turnNum = 0;
			heartbeat_0 = dis.readInt();

			while (true) {
				try {
					heartbeat_0 = dis.readInt();
					System.out.println("heartbeat_0 is " + heartbeat_0);
					if (heartbeat_0 == 17) {
						opponentName = dis.readUTF();
						turnNum = dis.readInt();
						if (turnNum == 1) {
							System.out.println("私が先攻, 相手の名前は" + opponentName);
						} else {
							System.out.println("私が後攻, 相手の名前は" + opponentName);
						}
						break;
					} else {
						dis.readInt();
						dis.readInt();
						if (matching) {
							dos.writeInt(16);
							dos.writeInt(0);
							dos.writeInt(1);
						} else { // キャンセル時はこちら
							dos.writeInt(16);
							dos.writeInt(0);
							dos.writeInt(0);
							return;
						}
					}
				} catch (SocketException se) {
					return;
				} catch (Exception ex) {
					ex.printStackTrace();
					return;
				}
			}

			// ここからマッチング後
			boolean turn = turnNum != 0;
			while (true) {
				// 先行なら
				if (turn) {
					new Thread(() -> {
						try {
							System.out.println("あなたの番です");
							int cmd = scanner.nextInt();
							dos.writeInt(cmd);
							dos.writeInt(2);
							dos.writeInt(2);
							dos.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}).start();
				}

				int[] response = new int[3];
				for (int i = 0; i < 3; i++) {
					response[i] = dis.readInt();
				}
				System.out.println("response 0,1,2 = " + response[0] + "," + response[1] + "," + response[2]);
				if (response[0] == 16) {
					dos.writeInt(16);
					dos.writeInt(0);
					dos.writeInt(1);
					dos.flush();
				} else {
					new Thread(() -> {
						try {
							int cmd = scanner.nextInt();
							dos.writeInt(cmd);
							dos.writeInt(2);
							dos.writeInt(2);
							dos.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}).start();
				}
			}

		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}
}

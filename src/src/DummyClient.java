package src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class DummyClient {

	public static void main(String[] args) {
		
		//空き部屋確認
		
		Socket socket1 = null;
		
		try {
			socket1 = new Socket("localhost", 10001);
			PrintWriter out = new PrintWriter(socket1.getOutputStream(), true);
			DataInputStream in = new DataInputStream(socket1.getInputStream());
			int heartbeat = 1; // ハートビートメッセージ

			out.println(heartbeat); // ハートビートメッセージを送信

			int vacantRoom[] = {-1,-1,-1};
			// サーバからのレスポンスをパースしてint配列に格納
			vacantRoom[0] = in.readInt();
			vacantRoom[1] = in.readInt();
			vacantRoom[2] = in.readInt();
			System.out.println("空き状況:"+vacantRoom[0]+vacantRoom[1]+vacantRoom[2]);

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
		
		//マッチング
		
		try {
			Socket socket2 = new Socket("localhost", 10000);
			DataOutputStream out = new DataOutputStream(socket2.getOutputStream());
			out.writeUTF("test");
			out.writeInt(1);
			out.flush();
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
							sendHeartbeat(1);
						} else {
							sendHeartbeat(0);
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
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}

}

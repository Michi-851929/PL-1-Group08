import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.swing.JFrame;
import java.util.*;

public class Client extends JFrame implements ActionListener{

	private static final int SERVER_PORT_1 = 10000;//接続確認に使うほうはこちら
	private static final int SERVER_PORT_2 = 10001;//部屋確認に使うほうはこちら
	private static final int COMMAND_CHECK_INTERVAL = 101; // 0.1秒ごとにgetCommandメソッドを監視する
	private static final int HEARTBEAT_INTERVAL = 1000; // 1秒ごとにサーバーにハートビートを送信する
	private static final int TIMEOUT_INTERVAL = 5000; // 5秒サーバーからのレスポンスがなかったらタイムアウトする

	String hostName = "localhost"; // 接続するサーバのホスト名

	private int[] vacantRoom;
	Client()
	{
		
	}

	public void changePhase()
	{
		
	}

	public void reloadDisplay()
	{
		
	}
	
	public String[] getWaitingPlayers()
	{
		
	}

	public void doMyTurn()
	{
		
	}
	
	public void doYourTurn()
	{
		
	}
	public void connectToServer() {
		try {

			// サーバーに接続する
			Socket socket = new Socket("hostName", SERVER_PORT_1);

			// 仮のtextFieldと仮のアクションイベント
			// エラーが表示されているのが気になるので追加しただけ
			//TODO：後で消す
			JTextField textField = new JTextField("Player1");
			ActionEvent f = new ActionEvent(new JButton("Button 1"), ActionEvent.ACTION_PERFORMED, null);


			// 名前とルーム番号をサーバーに送信する
			String name = getPlayerName(textField);
			int roomNumber = getRoomNumber(f);
			sendPlayerInfo(socket, name, roomNumber);


			//プレイヤ名を受け取る
			new Thread(() -> {
				try {
					// プレイヤ名とルーム番号を受信する
					BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String opponentName = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).start();

			// getCommandメソッドを監視するスレッド起動する
			// マッチング中にもこれを走らせ、接続中断のボタンが押された時も対応可能にする。
			new Thread(() -> {
				int[] prevCommand = new int[2];
				while (true) {
					try {
						// 0.1秒待つ
						Thread.sleep(COMMAND_CHECK_INTERVAL);

						// getCommandメソッドを呼び出す
						int[] newCommand = getCommand();

						// 前回のコマンドと変化があったらサーバーに送信する
						if (!isEqual(prevCommand, newCommand)) {
							prevCommand = newCommand;
							if (isEqual(newCommand, new int[]{16, 0})) {
								// 特別な入力があった場合、ハートビートに0を送って接続を終了する
								sendHeartbeat(socket, 0);
								socket.close();
								return;
							}
							sendCommand(socket, newCommand);
						}
					} catch (InterruptedException e) {
						// スレッドが中断されたら終了する
						return;
					} catch (IOException e) {
						// サーバーに接続できなかったら終了する
						e.printStackTrace();
						return;
					}
				}
			}).start();


			// ハートビートを送信するスレッドを起動する
			new Thread(() -> {
				try {
					// 1秒ごとにハートビートを送信する
					while (true) {
						sendHeartbeat(socket,1);
						Thread.sleep(HEARTBEAT_INTERVAL);
					}
				} catch (InterruptedException e) {
					// スレッドが中断されたら終了する
					return;
				} catch (IOException e) {
					// サーバーに接続できなかったら終了する
					e.printStackTrace();
					return;
				}
			}).start();

			// サーバーからのレスポンスを受け取る
			while (true) {
				try {
					// サーバーからのデータを受け取る
					int response = receiveResponse(socket);

					// サーバーからのレスポンスが1でなければエラー
					if (response != 1) {
						throw new RuntimeException("サーバーから不正な値が送信されました: " + response);
					}
				} catch (SocketTimeoutException e) {
					// タイムアウトしたら例外処理を返して終了する
					System.err.println("接続がタイムアウトしました");
					socket.close();
					return;
				} catch (IOException e) {
					// サーバーからのデータを受け取れなかったらエラー
					e.printStackTrace();
					socket.close();
					return;
				}
			}
		} catch (IOException e) {
			// サーバーに接続できなかったらエラー
			e.printStackTrace();
		}
	}


	/**
	 2つのint配列が等しいかどうかを判定する。
	 **/
	private static boolean isEqual(int[] a, int[] b) {
		if (a.length != b.length) {
			return false;
		}
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}
	public int getRoomNumber(ActionEvent e) {
		int roomNumber = 0;
		Object source = e.getSource();

		if (source instanceof JButton) {
			JButton button = (JButton) source;
			String buttonText = button.getText();

			switch (buttonText) {
				case "Button 1":
					roomNumber = 1;
					break;
				case "Button 2":
					roomNumber = 2;
					break;
				case "Button 3":
					roomNumber = 3;
					break;
				default:
					break;
			}
		}

		return roomNumber;
	}

	private static String getPlayerName(JTextField textField) {
		return textField.getText();
	}
	private static void sendPlayerInfo(Socket socket, String name, int roomNumber) throws IOException {
		// 名前とルーム番号をサーバーに送信する
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.writeUTF(name);
		out.writeInt(roomNumber);
		out.flush();
	}
	/**
	 int[2]のコマンドをサーバーに送信する。
	 配列の送信方法は
	 **/
	private static void sendCommand(Socket socket, int[] command) throws IOException {
		command[2] = getTimeLimit();
		OutputStream out = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(command);
	}
	/**
	 TODO:現在の制限時間を参照できるようにする
	 **/
	public static int getTimeLimit() {
		int timeLimit = 0;
		return timeLimit;
	}

	/**
	 サーバーにハートビートを送信する。接続確認って言ってましたが俗にハートビートというらしいです。
	 **/
	private static void sendHeartbeat(Socket socket,int heartbeat) throws IOException {
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(heartbeat); // ハートビートを表す値として通常は1を送信する
	}

	/**
	 サーバーからのレスポンスを受け取る。
	 タイムアウトした場合はSocketTimeoutExceptionを投げる。
	 **/
	private static int receiveResponse(Socket socket) throws IOException {
		InputStream in = socket.getInputStream();
		DataInputStream dis = new DataInputStream(in);
		socket.setSoTimeout(TIMEOUT_INTERVAL); // タイムアウト時間を設定する
		return dis.readInt();
	}

	/**
	 int[2]を取得する仮実装。最後に押されたボタンを所得する関数をお願いします。
	 **/
	private static int[] getCommand() {
		int[] play = new int[2];
		return play;
	}


	public void checkVacantRoom() {

		try (
				Socket socket = new Socket(hostName, SERVER_PORT_2);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		) {
			int heartbeat = 1; // ハートビートメッセージ
			String responseMsg; // サーバからのレスポンスメッセージ

			while (true) {
				out.println(heartbeat); // ハートビートメッセージを送信
				responseMsg = in.readLine(); // サーバからのレスポンスを受信

				if (responseMsg != null) {
					// サーバからのレスポンスをパースしてint配列に格納
					String[] responseArray = responseMsg.split(",");
					vacantRoom[0] = Integer.parseInt(responseArray[0]);
					vacantRoom[1] = Integer.parseInt(responseArray[1]);
					vacantRoom[2] = Integer.parseInt(responseArray[2]);
				}

				Thread.sleep(500); // 0.5秒待機
			}
		} catch (IOException e) {
			System.err.println("Error connecting to server: " + e.getMessage());
		} catch (InterruptedException e) {
			System.err.println("Heartbeat interrupted: " + e.getMessage());
		}
	}




	public void endBattle()
	{
		
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		
	}
	
    public static void main(String[] args)
    {
        

    }


}

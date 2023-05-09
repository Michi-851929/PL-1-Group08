import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.swing.JFrame;

public class Client extends JFrame implements ActionListener{

	private static final int SERVER_PORT_1 = 10000;//接続確認に使うほうはこちら
	private static final int COMMAND_CHECK_INTERVAL = 500; // 0.5秒ごとにgetCommandメソッドを監視する
	private static final int HEARTBEAT_INTERVAL = 1000; // 1秒ごとにサーバーにハートビートを送信する
	private static final int TIMEOUT_INTERVAL = 5000; // 5秒サーバーからのレスポンスがなかったらタイムアウトする
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

	public String[] accessServer()
	{

	}

	public boolean reactConnection()
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
			Socket socket = new Socket("localhost", SERVER_PORT_1);

			// getCommandメソッドを監視するスレッドを起動する
			new Thread(() -> {
				int[] prevCommand = new int[2];
				while (true) {
					try {
						// 0.5秒待つ
						Thread.sleep(COMMAND_CHECK_INTERVAL);

						// getCommandメソッドを呼び出す
						int[] newCommand = getCommand();

						// 前回のコマンドと変化があったらサーバーに送信する
						if (!isEqual(prevCommand, newCommand)) {
							prevCommand = newCommand;
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
						sendHeartbeat(socket);
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


	/**
	 int[2]のコマンドをサーバーに送信する。
	 配列の送信方法は
	 **/
	private static void sendCommand(Socket socket, int[] command) throws IOException {
		OutputStream out = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(command);

	}

	/**
	 サーバーにハートビートを送信する。接続確認って言ってましたが俗にハートビートというらしいです。
	 **/
	private static void sendHeartbeat(Socket socket) throws IOException {
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(1); // ハートビートを表す値として1を送信する
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server{
	private int port; // サーバの待ち受け用ポート
	private PrintWriter out; //データ送信用オブジェクト
	//private Receiver receiver; //データ受信用オブジェクト
	static boolean[] RoomInfo = {false, false, false};
	private GameThread[] GameThread; //対局用スレッド

	//Serverコンストラクタ
	public Server(int port) { //待ち受けポートを引数とする
		this.port = port; //待ち受けポートを渡す
		RoomInfoThread rit = new RoomInfoThread();//部屋状況確認スレッドを宣言
		rit.start();
		GameThread = new GameThread[128];//ゲームスレッドを宣言
		for(int i = 0; i<128; i++) {
			GameThread[i] = new GameThread(i);
			GameThread[i].start();
		}
		MatchThread mt = new MatchThread(port);
		mt.start();

	}

	//待ちプレイヤ確認応答スレッド
	class RoomInfoThread extends Thread{
		int Info_port;
		private PrintWriter Info_out; //データ送信用オブジェクト
		//コンストラクタ
		RoomInfoThread(){
			Info_port = port+1; //port+1番のポートを待ちプレイヤ確認応答スレッドに使用する
		}
		//run
		@Override
		public void run() {
			while(true) {
				try {
					ServerSocket ri_ss = new ServerSocket(Info_port);
					Socket ri_socket = ri_ss.accept();
					Info_out = new PrintWriter(ri_socket.getOutputStream(), true);//データ送信オブジェクトを用意
					updateRoomInfo();
					Info_out.println(Server.GetRoomInfo()); //待ち状況を書き込む
					Info_out.flush(); //待ち状況を送信する
					ri_ss.close();
					ri_socket.close();
				} catch (IOException e) {
					System.err.println("クライアントとの接続が切れました．");
				}
			}
		}
	}

	//待ちプレイヤ情報出力
	public static boolean[] GetRoomInfo() {
		return RoomInfo;
	}

	//待ちプレイヤ更新メソッド
	public boolean[] updateRoomInfo() {
		boolean[] retval = new boolean[3];//返り値

		//ここに更新処理を入れる//

		return retval;
	}

	//マッチングスレッド
	class MatchThread extends Thread{
		int port;
		MatchThread(int port){
			this.port = port;
		}

		//runメソッド
		@Override
		public void run() {
			while(true) { //無限ループ
				try {
					ServerSocket ss_match = new ServerSocket(port);
					Socket socket_match = ss_match.accept();
					System.out.println("プレイヤーがマッチングスレッドに接続しました");
					
					//プレイヤーデータ宣言
					String player_name = null;
					int player_time = 0;//希望待ち時間　1:3min 2:5min 3:7min
					//データ受信
					DataInputStream is = new DataInputStream(socket_match.getInputStream());
					player_name = is.readUTF();
					player_time = is.readInt();
					is.close(); //DataInputStreamをclose

					//待ち時間の一致するプレイヤーを探す
					int room_tojoin = findWaitingRoom(player_time);

					//待機中のプレイヤがいない場合、最も番号の若い空き部屋を探す(findVacantRoom)
					if(room_tojoin == -1) {
						room_tojoin = findVacantRoom();

						//空き部屋が見つかったら
						if(room_tojoin != -1) {
							GameThread[room_tojoin].setPlayer(socket_match, player_name, true);
						}

						//空き部屋が見つからないとき、クライアントを切断
						else {
							ss_match.close();
							socket_match.close();
						}
					}

					//待機中のプレイヤがいる部屋を見つけられた場合
					else {
						GameThread[room_tojoin].setPlayer(socket_match, player_name, false);//後攻なので3個目の引数はfalse
					}
					

				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} 
			}
		}

		//待機中のプレイヤがいる部屋を探す
		private int findWaitingRoom(int time) {
			int retval = -1;
			for(int i = 0; i<128;i++) {
				if(GameThread[i].isWaiting() && time == GameThread[i].getTime()) {
					retval = i;
					break;
				}
			}
			return retval;
		}

		//最も番号が若い空き部屋を探す
		private int findVacantRoom() {
			int retval = -1;
			for(int i = 0; i<128; i++) {
				if(GameThread[i].isVacant()) {
					retval = i;
					break;
				}
			}
			return retval;
		}
	}
	//マッチングここまで

	//対局スレッド
	class GameThread extends Thread{
		int RoomID;
		String P1_name;//先攻
		String P2_name;//後攻
		int time;//開始時の残り時間
		Socket P1_socket;//先攻のソケット
		Socket P2_socket;//後攻のソケット


		//コンストラクタ
		GameThread(int id){
			P1_name = null;
			P2_name = null;
			time = 0;
			RoomID = id;
			System.out.println("Room"+id+"の試合を終了しました");
		}

		//待機プレイヤの有無を返す
		public boolean isWaiting() {
			if(P1_name != null && P2_name == null) {
				return true;
			}
			else {
				return false;
			}
		}

		//空のルームであるか否かを返す trueなら空
		public boolean isVacant() {
			if(P1_name == null && P2_name == null) {
				return true;
			}
			else {
				return false;
			}
		}

		//プレイヤ名を返す
		public String getPlayerName(boolean isFirst) {
			if(isFirst) {
				return P1_name;
			}
			else {
				return P2_name;
			}
		}

		//部屋の初期持ち時間を返す
		public int getTime() {
			return time;
		}

		//プレイヤを部屋に入れる
		public void setPlayer(Socket sc, String name, boolean isFirst) {
			if(isFirst) {
				P1_name = name;
				P1_socket = sc;
			}
			else {
				P2_name = name;
				P2_socket = sc;
			}
		}

		//ゲーム開始時の残り時間を設定する
		public void setTime(int t) {
			time = t;
		}
		
		//試合終了メソッド
		public void closeGame() {
			P1_name = null;
			P2_name = null;
			P1_socket.close();
			P2_socket.close();
			int time = 0;
		}

		//runメソッド
		@Override
		public void run() {
			while(true) {
				try {
					while(P1_name == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
					System.out.println(P1_name+"が Room"+RoomID+"に先攻として入りました");
					ConnectThread P1_ct = new ConnectThread(RoomID, true, P1_socket);
					while(P2_name == null) {//後攻が来るまで無限ループ
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
					System.out.println(P2_name+"が Room"+RoomID+"に後攻として入りました");
					ConnectThread P2_ct = new ConnectThread(RoomID, false, P2_socket);

					//後攻が来たら
					BufferedReader br_p1 = new BufferedReader(new InputStreamReader(P1_socket.getInputStream()));
					BufferedReader br_p2 = new BufferedReader(new InputStreamReader(P2_socket.getInputStream()));
					BufferedWriter bw_p1 = new BufferedWriter(new OutputStreamWriter(P1_socket.getOutputStream()));
					BufferedWriter bw_p2 = new BufferedWriter(new OutputStreamWriter(P2_socket.getOutputStream()));
					ObjectOutputStream oos_p1 = new ObjectOutputStream(P1_socket.getOutputStream());
					ObjectInputStream ois_p1 = new ObjectInputStream(P1_socket.getInputStream());
					ObjectOutputStream oos_p2 = new ObjectOutputStream(P2_socket.getOutputStream());
					ObjectInputStream ois_p2 = new ObjectInputStream(P2_socket.getInputStream());
					
					//先攻/後攻を送信
					bw_p1.write(1);
					bw_p1.flush();
					bw_p2.write(0);
					bw_p2.flush();
					
					//相手の名前を送信
					bw_p1.write(P2_name);
					bw_p1.flush();
					bw_p2.write(P1_name);
					bw_p2.flush();
					
					//試合終了まで無限ループ
					while(true) {
						//先攻の番
						
					}
					//★続き(対局部分)はここに今度書きます			
				}
				catch (SocketTimeoutException es){
					//★残っている側のプレイヤーに対戦相手がタイムアウトしたことを伝え試合を終了する
				}
				catch (LeaveGameException el) {
					//★切断希望が出たことをプレイヤーに伝える
				}
				closeGame();
				//ここでGameThread[i]は初期状態に戻り、無限ループへ
			}
		}
	}
	//対局スレッドここまで

	//接続状態確認スレッド
	class ConnectThread extends Thread{
		int id;
		int command_send[];
		Boolean isFirst;
		Socket ct_socket;
		ConnectThread(int id, boolean isFirst, Socket s){
			this.id = id;
			this.isFirst = isFirst;
			ct_socket = s;
			command_send = new int[3];
			command_send[0]=16;
			command_send[1]=1;
			command_send[2]=-1;
		}
		@Override
		public void run() {
			InputStream is_ct = ct_socket.getInputStream();
			OutputStream os_ct = ct_socket.getOutputStream();
			DataOutputStream dos_ct = new DataOutputStream(os_ct);
			ObjectOutputStream oos_ct = new ObjectOutputStream(os_ct);
			ct_socket.setSoTimeout(1000);
			while(true) {
				try {
					oos_ct.writeObject(command_send);
					if(is_ct.read() == 1) {
						//ok
						Thread.sleep(1000);
					}
					else {
						if(isFirst) {
							throw new LeaveGameException("先攻がゲーム退出希望");
						}
						else {
							throw new LeaveGameException("後攻がゲーム退出希望");
						}
					}
					break;
			}

			catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch(SocketTimeoutException es) {
				if(isFirst) {
					GameThread[id].P1_socket.close();
				}
				else {
					GameThread[id].P2_socket.close();
				}
				ct_socket.close();
				throw es;
			}
		}
	}

	//mainメソッド
	public static void main(String[] args){
		Server server = new Server(10000); //待ち受けポート10000番でサーバオブジェクトを準備
	}
}
	//切断希望受信エラー
public class LeaveGameException extends Exception{
	LeaveGameException(String msg){
		super(msg);
	}
}
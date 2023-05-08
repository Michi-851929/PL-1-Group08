import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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
		GameThread = new GameThread[128];//ゲームスレッドを宣言
		for(int i = 0; i<128; i++) {
			GameThread[i] = new GameThread(i);
		}
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
	
	//Sample// データ受信用スレッド(内部クラス)
	class Receiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ

		// 内部クラスReceiverのコンストラクタ
		Receiver (Socket socket){
			try{
				sisr = new InputStreamReader(socket.getInputStream());
				br = new BufferedReader(sisr);
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}
		
		// 内部クラス Receiverのメソッド
		public void run(){
			try{
				while(true) {// データを受信し続ける
					String inputLine = br.readLine();//データを一行分読み込む
					if (inputLine != null){ //データを受信したら
						System.out.println("サーバからメッセージ " + inputLine + " が届きました．そのまま返信します．");
						out.println(inputLine);//受信データをバッファに書き出す
						out.flush();//受信データをそのまま返信する
					}
				}
			} catch (IOException e){ // 接続が切れたとき
				System.err.println("クライアントとの接続が切れました．");
			}
		}
	}
	//Sampleここまで

	//マッチングスレッド
	class MatchThread extends Thread{
		int port;
		MatchThread(int port){
			this.port = port;
		}
		
		//runメソッド
		public void run() {
			while(true) { //無限ループ
				try {
					ServerSocket ss_match = new ServerSocket(port);
					Socket socket_match = ss_match.accept();
					System.out.println("プレイヤーがマッチングスレッドに接続しました");
					
					//★データ受信をここに書く予定
					
					String player_name = null;
					int player_time = 1;//希望待ち時間
					
					//待ち時間の一致するプレイヤーを探す
					int room_tojoin = findWaitingRoom(player_time);
					
					//待機中のプレイヤがいない場合、最も番号の若い空き部屋を探す(findVacantRoom)
					if(room_tojoin == -1) {
						room_tojoin = findVacantRoom();
						
						//空き部屋が見つかったら
						if(room_tojoin != -1) {
							GameThread[room_tojoin].setPlayer(socket_match, player_name, true);
						}
						
						//空き部屋が見つからなかったら
						else {
							//★部屋が見つからなかったことをクライアントに伝える処理を書く
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
		
		//runメソッド
		public void run() {
			while(P1_name == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
			System.out.println(P1_name+"が Room"+RoomID+"に先攻として入りました");
			while(P2_name == null) {//後攻が来るまで無限ループ
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
			System.out.println(P2_name+"が Room"+RoomID+"に後攻として入りました");
			//後攻が来たら
			//続きは今度書きます
		}		
	}
	//対局スレッドここまで

	// メソッド
	/*public void acceptClient(){ //クライアントの接続(サーバの起動)
		try {
			System.out.println("サーバが起動しました．");
			ServerSocket ss = new ServerSocket(port); //サーバソケットを用意
			while (true) {
				Socket socket = ss.accept(); //新規接続を受け付ける
				System.out.println("クライアントと接続しました．"); //テスト用出力
					out = new PrintWriter(socket.getOutputStream(), true);//データ送信オブジェクトを用意
					receiver = new Receiver(socket);//データ受信オブジェクト(スレッド)を用意
					receiver.start();//データ送信オブジェクト(スレッド)を起動
			}
		} catch (Exception e) {
			System.err.println("ソケット作成時にエラーが発生しました: " + e);
		}
	}
	*/
	
	//mainメソッド
	public static void main(String[] args){
		Server server = new Server(10000); //待ち受けポート10000番でサーバオブジェクトを準備
		//server.acceptClient(); //クライアント受け入れを開始

	}
}


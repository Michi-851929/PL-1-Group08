import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Server {
	private int port; // サーバの待ち受け用ポート
	// private PrintWriter out; //データ送信用オブジェクト
	// private Receiver receiver; //データ受信用オブジェクト
	static boolean[] RoomInfo = { false, false, false };
	private GameThread[] GameThread; // 対局用スレッド

	private Socket sockets[]; // ソケット
	private DataInputStream diss[];

	// Serverコンストラクタ
	public Server(int port) { // 待ち受けポートを引数とする
		this.port = port; // 待ち受けポートを渡す
		RoomInfoThread rit = new RoomInfoThread();// 部屋状況確認スレッドを宣言
		rit.start();
		GameThread = new GameThread[128];// ゲームスレッドを宣言
		for (int i = 0; i < 128; i++) {
			GameThread[i] = new GameThread(i);
			GameThread[i].start();
		}
		MatchThread mt = new MatchThread(port);
		mt.start();
		
		sockets = new Socket[256];
		for (int i = 0; i < 256; i++) {
			sockets[i] = new Socket();
		}
		
		diss = new DataInputStream[256];
	}

	// 待ちプレイヤ確認応答スレッド
	class RoomInfoThread extends Thread {
		int Info_port;
		private BufferedReader Info_in; // データ受信用オブジェクト
		private DataOutputStream dos;

		// コンストラクタ
		RoomInfoThread() {
			Info_port = port + 1; // port+1番のポートを待ちプレイヤ確認応答スレッドに使用する
		}

		// run
		@Override
		public void run() {
			while (true) {
				try {
					ServerSocket ri_ss = new ServerSocket(Info_port);
					Socket ri_socket = ri_ss.accept();
					Info_in = new BufferedReader(new InputStreamReader(ri_socket.getInputStream()));
					dos = new DataOutputStream(ri_socket.getOutputStream());

					if (Info_in.read() == '1') {
						updateRoomInfo();
						for (int i = 0; i < 3; i++) {
							if (RoomInfo[i]) {// 待機している人がいるなら
								dos.writeInt(1);
							} else {// 待機している人がいないなら
								dos.writeInt(0);
							}
						}
						dos.flush();// クライアントに待ち情報を送信
					} else {
						System.out.println("RoomInfoThread:クライアントから送られた値が1ではありません");
					}
					ri_ss.close();
					ri_socket.close();
					//System.out.println("RoomInfoThread: ソケットを閉じました");
				} catch (IOException e) {
					System.err.println("RoomInfoThread:クライアントとの接続が切れました．");
				}
			}
		}
	}

	// 待ちプレイヤ情報出力
	public static boolean[] GetRoomInfo() {
		return RoomInfo;
	}

	// 待ちプレイヤ更新メソッド
	public boolean[] updateRoomInfo() {
		boolean[] retval = new boolean[3];// 返り値
		retval[0] = false;
		retval[1] = false;
		retval[2] = false;
		for (int i = 0; i < 1; i++) {
			if (GameThread[i].isWaiting()) {
				switch (GameThread[i].getTime()) {
					case 1:
						retval[0] = true;
						break;
					case 2:
						retval[1] = true;
						break;
					case 3:
						retval[2] = true;
						break;
				}
			}
		}

		return retval;
	}

	// マッチングスレッド
	class MatchThread extends Thread {
		int port;
		int player_num; //プレイヤー番号

		MatchThread(int port) {
			this.port = port;
			player_num = 0;
		}

		// runメソッド
		@Override
		public void run() {
			ServerSocket ss_match = null;
			try {
				ss_match = new ServerSocket(port);
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			while (true) { // 無限ループ
				try {
					//Socket socket_match = ss_match.accept();
					/*for(int i = 0;i<256;i++) {
						System.out.println("MatchThread:" + sockets[i].isClosed());
						if(sockets[i].isClosed()) {
							player_num = i;
							break;
						}
					}*/
					player_num++;
					System.out.println("MatchThread:player_num = " + player_num);
					System.out.println("MatchThread:" + sockets[player_num].isClosed());
					System.out.println("MatchThread:プレイヤーを待っています 次のプレイヤーの番号: "+player_num);
					sockets[player_num] = ss_match.accept();
					
					System.out.println("MatchThread:プレイヤーがマッチングスレッドに接続しました player_num:"+ player_num);
					
					/*
					if (socket_match.isClosed()) {
						System.out.println("MatchThread.run(): Player's socket is closed!");
					} else {
						System.out.println("MatchThread.run(): Player's socket is NOT closed!");
					}
					*/

					// プレイヤーデータ宣言
					String player_name = null;
					int player_time = 0;// 希望待ち時間 1:3min 2:5min 3:7min
					// データ受信
					//DataInputStream is = new DataInputStream(socket_match.getInputStream());
					diss[player_num] = new DataInputStream(sockets[player_num].getInputStream());
					
					/*
					player_name = is.readUTF();
					player_time = is.readInt();
					*/
					player_name = diss[player_num].readUTF();
					player_time = diss[player_num].readInt();
					
					//System.out.println("MatchThread: socket_match ->" + socket_match.toString());
					System.out.println("MatchThread: socket_match ->" + sockets[player_num].toString());


					// 待ち時間の一致するプレイヤーを探す
					int room_tojoin = findWaitingRoom(player_time);

					// 待機中のプレイヤがいない場合、最も番号の若い空き部屋を探す(findVacantRoom)
					if (room_tojoin == -1) {
						room_tojoin = findVacantRoom();

						// 空き部屋が見つかったら
						/*
						if (room_tojoin != -1) {
							GameThread[room_tojoin].setPlayer(socket_match, player_name, true);
							// 部屋の時間を設定
							GameThread[room_tojoin].setTime(player_time);
						}
						*/
						if (room_tojoin != -1) {
							GameThread[room_tojoin].setPlayer(player_num, player_name, true);
							// 部屋の時間を設定
							GameThread[room_tojoin].setTime(player_time);
						}
						

						// 空き部屋が見つからないとき、クライアントを切断
						else {
							ss_match.close();
							sockets[player_num].close();
							System.out.println("MatchThread: 空き部屋が見つからないためソケットを閉じました");
						}
					}

					// 待機中のプレイヤがいる部屋を見つけられた場合
					else {
						GameThread[room_tojoin].setPlayer(player_num, player_name, false);// 後攻なので3個目の引数はfalse
					}

					if (sockets[player_num].isClosed()) {
						System.out.println("MatchThread end of run(): Player's socket is closed!");
					} else {
						System.out.println("MatchThread end of run(): Player's socket is NOT closed!");
					}

					// InputStreamをclose
					//is.close();

				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}

		// 待機中のプレイヤがいる部屋を探す
		private int findWaitingRoom(int time) {
			int retval = -1;
			for (int i = 0; i < 1; i++) {
				if (GameThread[i].isWaiting() && time == GameThread[i].getTime()) {
					retval = i;
					break;
				}
			}
			return retval;
		}

		// 最も番号が若い空き部屋を探す
		private int findVacantRoom() {
			int retval = -1;
			for (int i = 0; i < 128; i++) {
				if (GameThread[i].isVacant()) {
					retval = i;
					break;
				}
			}
			return retval;
		}
	}
	// マッチングここまで

	// 対局スレッド
	class GameThread extends Thread {
		int RoomID;
		int P1_num; //プレイヤー番号
		int P2_num;
		String P1_name;// 先攻
		String P2_name;// 後攻
		int time;// 開始時の残り時間
		Socket P1_socket;// 先攻のソケット
		Socket P2_socket;// 後攻のソケット
		int command[];
		ReceiveMessageThread P1_rmt;
		ReceiveMessageThread P2_rmt;
		boolean running;
		ConnectThread P1_ct;
		ConnectThread P2_ct;

		// コンストラクタ
		GameThread(int id) {
			P1_name = null;
			P2_name = null;
			P1_num = -1;
			P2_num = -1;
			P1_socket = new Socket();
			P2_socket = new Socket();
			time = 0;
			RoomID = id;
			command = new int[3];
			command[0] = 0;
			command[1] = 0;
			command[2] = 0;
			System.out.println("GameThread[" + RoomID + "]: GameThreadを開始しました");
		}

		// 試合ループを終了
		public void stopRunning() {
			running = false;
		}

		// 待機プレイヤの有無を返す
		public boolean isWaiting() {
			if (P1_name != null && P2_name == null) {
				return true;
			} else {
				return false;
			}
		}

		// 空のルームであるか否かを返す trueなら空
		public boolean isVacant() {
			if (P1_name == null && P2_name == null) {
				return true;
			} else {
				return false;
			}
		}

		// プレイヤ名を返す
		public String getPlayerName(boolean isFirst) {
			if (isFirst) {
				return P1_name;
			} else {
				return P2_name;
			}
		}

		// 部屋の初期持ち時間を返す
		public int getTime() {
			return time;
		}

		// 旧 プレイヤを部屋に入れる
		/*
		public void setPlayer(Socket sc, String name, boolean isFirst) {
			if (isFirst) {
				P1_name = name;
				P1_socket = sc;
				
				System.out.println("GameThread.setPlayer(): P1's socket ->" + P1_socket.toString());
				if (P1_socket.isClosed()) {
					System.out.println("GameThread.setPlayer(): P1's socket is closed!");
				}

			} else {
				P2_name = name;
				P2_socket = sc;
			}
		}
		*/
		
		// 新プレイヤを部屋に入れる
		public void setPlayer(int pnum, String name, boolean isFirst) {
			if (isFirst) {
				P1_name = name;
				P1_num = pnum;
				System.out.println("GameThread.setPlayer(): P1's socket ->" + sockets[P1_num].toString());
				if (sockets[P1_num].isClosed()) {
					System.out.println("GameThread.setPlayer(): P1's socket is closed!");
				}
			} else {
				P2_name = name;
				P2_num = pnum;
			}
		}

		// 部屋情報を出力
		public void outputRoomInfo() {
			System.out.println("Room ID: " + RoomID);
			switch (getTime()) {
				case 1:
					System.out.println("Time: 5min");
					break;
				case 2:
					System.out.println("Time: 10min");
					break;
				case 3:
					System.out.println("Time: 20min");
					break;
				default:
					System.out.println("Time: Unknown");

			}
			System.out.println("First Player: " + getPlayerName(true));
			System.out.println("Second Player: " + getPlayerName(false));
		}

		// ゲーム開始時の残り時間を設定する
		public void setTime(int t) {
			time = t;
		}
///////////////////////////////////////////////////////////
		// 試合終了メソッド 
		/*
		public void closeGame() {
			P1_name = null;
			P2_name = null;
			try {
				P1_socket.close();
				P2_socket.close();
				System.out.println("GameThread" + RoomID + ": 試合が終了したためソケットを閉じました");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("GameThread" + RoomID + ": 試合が終了したためソケットを閉じようとしましたが閉じることができませんでした");
			}
			P1_rmt.stopRunning();
			P2_rmt.stopRunning();
			time = 0;
			System.out.println("GameThread[" + RoomID + "]: 試合を終了しました");
		}
		*/
		//新 試合終了メソッド
		public void closeGame() {
			P1_name = null;
			P2_name = null;
			try {
				sockets[P1_num].close();
				sockets[P2_num].close();
				sockets[P1_num] = new Socket();
				sockets[P2_num] = new Socket();
				System.out.println("GameThread" + RoomID + ": 試合が終了したためソケットを閉じました");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("GameThread" + RoomID + ": 試合が終了したためソケットを閉じようとしましたが閉じることができませんでした");
			}
			P1_rmt.stopRunning();
			P2_rmt.stopRunning();
			time = 0;
			System.out.println("GameThread[" + RoomID + "]: 試合を終了しました");
		}

		// runメソッド
		/*
		@Override
		public void run() {
			running = true;
			while (true) {
				try {
					while (P1_name == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					System.out.println("GameThread[" + RoomID + "]:" + P1_name + "が Room" + RoomID + "に、time:" + time
							+ "で先攻として入りました");
					ReceiveMessageThread P1_rmt = new ReceiveMessageThread(P1_socket);
					while (P2_name == null) {// 後攻が来るまで無限ループ
						try {
							Thread.sleep(2000);
							System.out
									.println("GameThread[" + RoomID + "]:" + P1_name + "が対戦相手を待機中 (time:" + time + ")");
							// 以下デバッグ分
							if (P1_socket.isClosed()) {
								System.out.println("GameThread.run(): P1's socket ->" + P1_socket.toString());
								throw new SocketException("Socket is closed");
							} else {
								System.out.println("GameThread.run(): P1's socket ->" + P1_socket.toString());
								System.out.println("GameThread[" + RoomID + "]:P1's Socket is not closed");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					System.out.println("GameThread[" + RoomID + "]:" + P2_name + "が Room" + RoomID + "に、time:" + time
							+ "後攻として入りました");
					ReceiveMessageThread P2_rmt = new ReceiveMessageThread(P2_socket);

					// 後攻が来たら
					DataOutputStream dos_p1 = new DataOutputStream(P1_socket.getOutputStream());
					DataOutputStream dos_p2 = new DataOutputStream(P2_socket.getOutputStream());

					// 相手の名前をもう一方に送信
					System.out.println("GameThread[" + RoomID + "]:対戦相手の情報を送信します");
					dos_p1.writeUTF(P2_name);// 先攻に後攻の名前を伝える
					dos_p2.writeUTF(P1_name);// 後攻に先攻の名前を伝える

					// 先攻/後攻を送信
					dos_p1.writeInt(1);// 先攻に自身が先攻であることを伝える
					dos_p2.writeInt(0);// 後攻に自身が先攻であることを伝える

					// ハートビート起動
					P1_ct = new ConnectThread(RoomID, true, P1_socket, P1_rmt);
					P2_ct = new ConnectThread(RoomID, false, P2_socket, P2_rmt);

					// 前の入力を定義
					int P1_commandBefore[] = new int[3];
					P1_commandBefore = new int[] { -1, -1, -1 };
					int P2_commandBefore[] = new int[3];
					P2_commandBefore = new int[] { -1, -1, -1 };
					// 試合終了まで無限ループ
					while (running) {
						// 先攻の番 盤面が変わるまで無限ループ
						while (P1_commandBefore[0] == P1_rmt.last_command[0]
								&& P1_commandBefore[1] == P1_rmt.last_command[1]) {
							Thread.sleep(50);
							if (running == false) { // 外部からstopRuningメソッドが呼び出されていた時はrunningがfalseとなる
													// このときは試合のwhileループを強制的に抜ける
								break;
							}
						}

						// 外部からstopRuningメソッドが呼び出されていた時はrunningがfalseとなる このときは試合のwhileループを強制的に抜ける
						if (running == false) {
							break;
						}
						// commandBeforeを更新
						P1_commandBefore[0] = P1_rmt.last_command[0];
						P1_commandBefore[1] = P1_rmt.last_command[1];

						// 後攻に情報を送信
						for (int i = 0; i < 3; i++) {
							dos_p2.writeInt(P1_rmt.last_command[i]);
						}

						// 試合終了判定 if分内がtrueなら試合終了なのでwhileループを抜ける
						if (P1_rmt.last_command[0] > 7) {
							break;
						}

						// 後攻の番 盤面が変わるまで無限ループ
						while (P2_commandBefore[0] == P2_rmt.last_command[0]
								&& P2_commandBefore[1] == P2_rmt.last_command[1]) {
							Thread.sleep(50);
							if (running == false) {
								break;
							}
						}
						if (running == false) {
							break;
						}
						// commandBeforeを更新
						P2_commandBefore[0] = P2_rmt.last_command[0];
						P2_commandBefore[1] = P2_rmt.last_command[1];

						// 先攻に情報を送信
						for (int i = 0; i < 3; i++) {
							dos_p1.writeInt(P2_rmt.last_command[i]);
						}
						// 試合終了判定
						if (P2_rmt.last_command[0] > 7) {
							break;
						}
					}
					// 試合終了後

					// 接続確認メソッドを停止
					P1_ct.stopRunning();
					P2_ct.stopRunning();
				}
				/*
				 * catch (SocketTimeoutException es){
				 * //★残っている側のプレイヤーに対戦相手がタイムアウトしたことを伝え試合を終了する
				 * }
				 * catch (LeaveGameException el) {
				 * //★切断希望が出たことをプレイヤーに伝える
				 * }
				 */
		/*
				catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException eio) {
					eio.printStackTrace();
				}
				System.out.println("a");
				closeGame();
				// ここでGameThread[i]は初期状態に戻り、無限ループへ
			}
			*/
			
			// runメソッド
			@Override
			public void run() {
				running = true;
				while (true) {
					try {
						while (P1_name == null) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						System.out.println("GameThread[" + RoomID + "]:" + P1_name + "が Room" + RoomID + "に、time:" + time
								+ "で先攻として入りました");
						ReceiveMessageThread P1_rmt = new ReceiveMessageThread(P1_num);
						while (P2_name == null) {// 後攻が来るまで無限ループ
							try {
								Thread.sleep(2000);
								System.out
										.println("GameThread[" + RoomID + "]:" + P1_name + "が対戦相手を待機中 (time:" + time + ")");
								// 以下デバッグ分
								if (P1_socket.isClosed()) {
									System.out.println("GameThread.run(): P1's socket ->" + sockets[P1_num].toString());
									throw new SocketException("Socket is closed");
								} else {
									System.out.println("GameThread.run(): P1's socket ->" + sockets[P1_num].toString());
									System.out.println("GameThread[" + RoomID + "]:P1's Socket is not closed");
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						System.out.println("GameThread[" + RoomID + "]:" + P2_name + "が Room" + RoomID + "に、time:" + time
								+ "後攻として入りました");
						ReceiveMessageThread P2_rmt = new ReceiveMessageThread(P2_num);

						// 後攻が来たら
						DataOutputStream dos_p1 = new DataOutputStream(sockets[P1_num].getOutputStream());
						DataOutputStream dos_p2 = new DataOutputStream(sockets[P2_num].getOutputStream());

						System.out.println(P1_num);
						System.out.println(P2_num);

						// 相手の名前をもう一方に送信
						System.out.println("GameThread[" + RoomID + "]:対戦相手の情報を送信します");
						dos_p1.writeUTF(P2_name);// 先攻に後攻の名前を伝える
						dos_p2.writeUTF(P1_name);// 後攻に先攻の名前を伝える
						System.out.println(P1_name);
						System.out.println(P2_name);

						// 先攻/後攻を送信
						dos_p1.writeInt(1);// 先攻に自身が先攻であることを伝える
						dos_p2.writeInt(0);// 後攻に自身が先攻であることを伝える

						// ハートビート起動
						P1_ct = new ConnectThread(RoomID, true, P1_num, P1_rmt);
						P2_ct = new ConnectThread(RoomID, false, P2_num, P2_rmt);

						// 前の入力を定義
						int P1_commandBefore[] = new int[3];
						P1_commandBefore = new int[] { -1, -1, -1 };
						int P2_commandBefore[] = new int[3];
						P2_commandBefore = new int[] { -1, -1, -1 };
						// 試合終了まで無限ループ
						while (running) {
							// 先攻の番 盤面が変わるまで無限ループ
							while (P1_commandBefore[0] == P1_rmt.last_command[0]
									&& P1_commandBefore[1] == P1_rmt.last_command[1]) {
								Thread.sleep(50);
								if (running == false) { // 外部からstopRuningメソッドが呼び出されていた時はrunningがfalseとなる
														// このときは試合のwhileループを強制的に抜ける
									break;
								}
							}

							// 外部からstopRuningメソッドが呼び出されていた時はrunningがfalseとなる このときは試合のwhileループを強制的に抜ける
							if (running == false) {
								break;
							}
							// commandBeforeを更新
							P1_commandBefore[0] = P1_rmt.last_command[0];
							P1_commandBefore[1] = P1_rmt.last_command[1];

							// 後攻に情報を送信
							for (int i = 0; i < 3; i++) {
								dos_p2.writeInt(P1_rmt.last_command[i]);
							}

							// 試合終了判定 if分内がtrueなら試合終了なのでwhileループを抜ける
							if (P1_rmt.last_command[0] > 7) {
								break;
							}

							// 後攻の番 盤面が変わるまで無限ループ
							while (P2_commandBefore[0] == P2_rmt.last_command[0]
									&& P2_commandBefore[1] == P2_rmt.last_command[1]) {
								Thread.sleep(50);
								if (running == false) {
									break;
								}
							}
							if (running == false) {
								break;
							}
							// commandBeforeを更新
							P2_commandBefore[0] = P2_rmt.last_command[0];
							P2_commandBefore[1] = P2_rmt.last_command[1];

							// 先攻に情報を送信
							for (int i = 0; i < 3; i++) {
								dos_p1.writeInt(P2_rmt.last_command[i]);
							}
							// 試合終了判定
							if (P2_rmt.last_command[0] > 7) {
								break;
							}
						}
						// 試合終了後

						// 接続確認メソッドを停止
						P1_ct.stopRunning();
						P2_ct.stopRunning();
					}
					/*
					 * catch (SocketTimeoutException es){
					 * //★残っている側のプレイヤーに対戦相手がタイムアウトしたことを伝え試合を終了する
					 * }
					 * catch (LeaveGameException el) {
					 * //★切断希望が出たことをプレイヤーに伝える
					 * }
					 */
					catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException eio) {
						eio.printStackTrace();
					}
					System.out.println("closeGame()メソッドを呼び出します a");
					closeGame();
					// ここでGameThread[i]は初期状態に戻り、無限ループへ
				}
		}
	}

	// 対局スレッドここまで

	// クライアントから送られる差し手やハートビートを受け取るスレッド
	/*
	class ReceiveMessageThread extends Thread {
		int receive_message[];// プレイヤーから受け取ったメッセージ
		int last_command[]; // 最新のプレイヤーの差し手
		int last_heartbeat[];// 最新のプレイヤーのハートビート
		Socket sc_rmt;
		boolean running;

		ReceiveMessageThread(Socket s) {
			receive_message = new int[3];
			receive_message = new int[] { -1, -1, -1 };
			last_command = new int[3];
			last_command = new int[] { -1, -1, -1 };
			last_heartbeat = new int[3];
			last_heartbeat = new int[] { -1, -1, -1 };
			sc_rmt = s;
			running = true;
		}

		public void stopRunning() {
			running = false;
		}

		public void run() {
			while (running) {
				DataInputStream dis_rmt = null;
				try {
					dis_rmt = new DataInputStream(sc_rmt.getInputStream());
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
				while (true) {
					for (int i = 0; i < 3; i++) {
						try {
							receive_message[i] = dis_rmt.readInt();
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
					if (receive_message[0] != 16) {
						for (int i = 0; i < 3; i++) {
							last_command[i] = receive_message[i];
						}
					} else {
						for (int i = 0; i < 3; i++) {
							last_heartbeat[i] = receive_message[i];
						}
					}
				}
			}
		}
	}
	*/
	
	// 新 クライアントから送られる差し手やハートビートを受け取るメソッド
	class ReceiveMessageThread extends Thread {
		int receive_message[];// プレイヤーから受け取ったメッセージ
		int last_command[]; // 最新のプレイヤーの差し手
		int last_heartbeat[];// 最新のプレイヤーのハートビート
		//Socket sc_rmt;
		boolean running;
		int num_player;

		ReceiveMessageThread(int pnum) {
			receive_message = new int[3];
			receive_message = new int[] { -1, -1, -1 };
			last_command = new int[3];
			last_command = new int[] { -1, -1, -1 };
			last_heartbeat = new int[3];
			last_heartbeat = new int[] { -1, -1, -1 };
			num_player = pnum;
			running = true;
		}

		public void stopRunning() {
			running = false;
		}

		public void run() {
			while (running) {
				DataInputStream dis_rmt = null;
				try {
					dis_rmt = new DataInputStream(sockets[num_player].getInputStream());
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
				while (true) {
					for (int i = 0; i < 3; i++) {
						try {
							receive_message[i] = dis_rmt.readInt();
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
					if (receive_message[0] != 16) {
						for (int i = 0; i < 3; i++) {
							last_command[i] = receive_message[i];
						}
					} else {
						for (int i = 0; i < 3; i++) {
							last_heartbeat[i] = receive_message[i];
						}
					}
				}
			}
		}
	}
	// ReceiveMessageThreadここまで

	// 接続状態確認スレッド
	/*
	class ConnectThread extends Thread {
		int id; // 部屋番号
		int command_send[];
		int command_receive[];
		Boolean isFirst; // 先攻か
		Boolean running;
		Socket ct_socket;
		ReceiveMessageThread rmt;

		ConnectThread(int id, boolean isFirst, Socket s, ReceiveMessageThread r) {
			this.id = id;
			this.isFirst = isFirst;
			ct_socket = s;
			command_send = new int[3];
			command_send[0] = 16;
			command_send[1] = 1;
			command_send[2] = -1;
			command_receive = new int[3];
			rmt = r;
			running = true;
		}

		public void stopRunning() {
			running = false;
		}

		@Override
		public void run() {
			try {
				DataOutputStream dos_ct = new DataOutputStream(ct_socket.getOutputStream());
				ct_socket.setSoTimeout(1000);
				while (running) {

					// ハートビートをDataOutputStreamで送る
					dos_ct.writeInt(command_send[0]);
					dos_ct.writeInt(command_send[1]);
					dos_ct.writeInt(command_send[2]);
					System.out.println("ConnectThread: ハートビートを送信" + command_send[0] + "," + command_send[1] + ","
							+ command_send[2]);

					if (rmt.last_heartbeat[1] == 1) {
						// ok
						rmt.last_heartbeat[1] = -1;// -1に書き換える 次も[1]が-1だったら1秒間の間にハートビートが無いことになるのでタイムアウトと判定
						Thread.sleep(1000);
					} else if (rmt.last_heartbeat[1] == -1) {// 前のハートビート確認から1秒後にrmt.last_heartbeat[1]が-1のままのとき
						throw new SocketTimeoutException("ConnectThread:タイムアウトしました");
					} else {
						if (isFirst) {
							throw new LeaveGameException("ConnectThread:先攻がゲーム退出希望");
						} else {
							throw new LeaveGameException("ConnectThread:後攻がゲーム退出希望");
						}
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (SocketTimeoutException es) {
				es.printStackTrace();
			} catch (LeaveGameException le) {
				le.printStackTrace();
			} catch (IOException ie) {
				ie.printStackTrace();
			} finally {
				GameThread[id].stopRunning(); // 試合のループを終了させる
			}
		}
	}*/
	// 新 接続状態確認スレッド
	class ConnectThread extends Thread {
		int id; // 部屋番号
		int command_send[];
		int command_receive[];
		Boolean isFirst; // 先攻か
		Boolean running;
		int num_player;
		ReceiveMessageThread rmt;

		ConnectThread(int id, boolean isFirst, int pnum, ReceiveMessageThread r) {
			this.id = id;
			this.isFirst = isFirst;
			num_player = pnum;
			command_send = new int[3];
			command_send[0] = 16;
			command_send[1] = 1;
			command_send[2] = -1;
			command_receive = new int[3];
			rmt = r;
			running = true;
		}

		public void stopRunning() {
			running = false;
		}

		@Override
		public void run() {
			try {
				DataOutputStream dos_ct = new DataOutputStream(sockets[num_player].getOutputStream());
				sockets[num_player].setSoTimeout(1000);
				while (running) {

					// ハートビートをDataOutputStreamで送る
					dos_ct.writeInt(command_send[0]);
					dos_ct.writeInt(command_send[1]);
					dos_ct.writeInt(command_send[2]);
					System.out.println("ConnectThread: ハートビートを送信" + command_send[0] + "," + command_send[1] + ","
							+ command_send[2]);

					if (rmt.last_heartbeat[1] == 1) {
						// ok
						rmt.last_heartbeat[1] = -1;// -1に書き換える 次も[1]が-1だったら1秒間の間にハートビートが無いことになるのでタイムアウトと判定
						Thread.sleep(1000);
					} else if (rmt.last_heartbeat[1] == -1) {// 前のハートビート確認から1秒後にrmt.last_heartbeat[1]が-1のままのとき
						throw new SocketTimeoutException("ConnectThread:タイムアウトしました");
					} else {
						if (isFirst) {
							throw new LeaveGameException("ConnectThread:先攻がゲーム退出希望");
						} else {
							throw new LeaveGameException("ConnectThread:後攻がゲーム退出希望");
						}
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (SocketTimeoutException es) {
				es.printStackTrace();
			} catch (LeaveGameException le) {
				le.printStackTrace();
			} catch (IOException ie) {
				ie.printStackTrace();
			} finally {
				GameThread[id].stopRunning(); // 試合のループを終了させる
			}
		}
	}

	// mainメソッド
	public static void main(String[] args) {
		Server server = new Server(10000); // 待ち受けポート10000番でサーバオブジェクトを準備
		Scanner scanner = new Scanner(System.in);
		while (true) {
			String admin_command = scanner.next();
			if (admin_command.equals("status")) {
				for (int i = 0; i < 128; i++) {
					if (!server.GameThread[i].isVacant()) {
						server.GameThread[i].outputRoomInfo();
					}
				}
			} else if (admin_command.equals("stop")) {
				break;
			} else {
				System.out.printf("未定義のコマンドです");
			}
		}
		scanner.close();
		return;
	}
}

// 切断希望受信エラー
class LeaveGameException extends Exception {
	LeaveGameException(String msg) {
		super(msg);
	}
}
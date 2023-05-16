import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Client extends JFrame implements ActionListener, FocusListener{

	private static final int SERVER_PORT_1 = 10000;//接続確認に使うほうはこちら
	private static final int COMMAND_CHECK_INTERVAL = 500; // 0.5秒ごとにgetCommandメソッドを監視する
	private static final int HEARTBEAT_INTERVAL = 1000; // 1秒ごとにサーバーにハートビートを送信する
	private static final int TIMEOUT_INTERVAL = 5000; // 5秒サーバーからのレスポンスがなかったらタイムアウトする
	private static final int PHASE_TITLE = 0; //changePhase()の引数でタイトル画面への遷移を表す
	private static final int PHASE_BATTLE = 1; //changePhase()の引数で対局画面への遷移を表す
	private static final int PHASE_RESULT = 2; //changePhase()の引数で結果画面への遷移を表す
	private static final Color BACKGROUND_COLOR = new Color(207, 207, 207);
	
	Othello othello;
	
	JPanel display = new JPanel();
	
	//タイトル画面のオブジェクト
	JTextField ui_tf_namefield;
	JButton ui_jb_5min;
	JButton ui_jb_10min;
	JButton ui_jb_20min;
	JLabel ui_jl_5min;
	JLabel ui_jl_10min;
	JLabel ui_jl_20min;
	JButton ui_jb_start;
	
	//対局画面のオブジェクト
	JLabel ui_jl_name1;
	JLabel ui_jl_name2;
	JLabel ui_jl_time1;
	JLabel ui_jl_time2;
	JLabel ui_jl_nstones1;
	JLabel ui_jl_nstones2;
	JButton ui_jb_giveup;
	JButton[][] ui_jb_field = new JButton[8][8];
	boolean command_pressed = false;
	int[] command_value = {-1, -1};
	
	//結果画面のオブジェクト
	JButton ui_jb_totitle;
	JButton ui_jb_exit;
	
	Client(String title)
	{
		super(title);
		//RepaintManager currentManager = RepaintManager.currentManager(this);
		//currentManager.setDoubleBufferingEnabled(false);
		setLayout(new FlowLayout());
		
		//スタート画面
		JPanel start_display = new JPanel();
		start_display.setLayout(new BorderLayout());
		JLabel ui_jl_logo1 = new JLabel(new ImageIcon("img/othellogo.png"));
		start_display.add(ui_jl_logo1, "Center");
		start_display.add(new JLabel("通信中・・・", SwingConstants.CENTER), "South");
		display.add(start_display);
		add(display);
		
		//接続を待つ
		
		//タイトル画面
		//changePhase(PHASE_TITLE);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(BACKGROUND_COLOR);
		setSize(800, 600);
		setVisible(true);
	}

	public void changePhase(int phase)
	{
		switch(phase) {
		//タイトル画面
		case PHASE_TITLE:
			display.removeAll();
			JPanel title = new JPanel();
			title.setLayout(new BorderLayout(0, 30));
			
			//ロゴとプレイヤ名入力エリア
			JPanel p00 = new JPanel();
			p00.setLayout(new BorderLayout());
			JLabel ui_jl_logo2 = new JLabel(new ImageIcon("img/othellogo2.png"));
			ui_tf_namefield = new JTextField("プレイヤ名");
			ui_tf_namefield.setForeground(Color.LIGHT_GRAY);
			ui_tf_namefield.addFocusListener(this);
			ui_tf_namefield.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			p00.add(ui_jl_logo2, "North");
			p00.add(ui_tf_namefield, "Center");
			title.add(p00, "North");
			
			//持ち時間選択ボタン
			JPanel p01 = new JPanel();
			p01.setLayout(new BorderLayout(0, 10));
			JPanel p02 = new JPanel();
			p02.setLayout(new BorderLayout());
			JPanel p03 = new JPanel();
			p03.setLayout(new GridLayout(1, 3, 10, 0));
			JPanel p04 = new JPanel();
			p04.setLayout(new GridLayout(1, 3));
			JLabel ui_jl_waittime = new JLabel("持ち時間", SwingConstants.CENTER);
			ui_jl_waittime.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 16));
			ui_jb_5min = new JButton("5分");
			ui_jb_5min.addActionListener(this);
			ui_jb_5min.setForeground(Color.BLACK);
			//ui_jb_5min.setBackground(Color.WHITE);
			ui_jb_5min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_10min = new JButton("10分");
			ui_jb_10min.addActionListener(this);
			ui_jb_10min.setForeground(Color.GREEN);
			//ui_jb_10min.setBackground(Color.GREEN);
			ui_jb_10min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_20min = new JButton("20分");
			ui_jb_20min.addActionListener(this);
			ui_jb_20min.setForeground(Color.BLACK);
			//ui_jb_20min.setBackground(Color.WHITE);
			ui_jb_20min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_5min = new JLabel("×", SwingConstants.CENTER);
			ui_jl_5min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_10min = new JLabel("×", SwingConstants.CENTER);
			ui_jl_10min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_20min = new JLabel("×", SwingConstants.CENTER);
			ui_jl_20min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			JLabel ui_jl_waiting = new JLabel("待機プレイヤ", SwingConstants.CENTER);
			ui_jl_waiting.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 16));
			p03.add(ui_jb_5min, "A");
			p03.add(ui_jb_10min, "B");
			p03.add(ui_jb_20min, "C");
			p04.add(ui_jl_5min, "A");
			p04.add(ui_jl_10min, "B");
			p04.add(ui_jl_20min, "C");
			p02.add(p03, "Center");
			p02.add(p04, "South");
			p01.add(ui_jl_waittime, "North");
			p01.add(p02, "Center");
			p01.add(ui_jl_waiting, "South");
			title.add(p01, "Center");
			
			//開始/中止ボタン
			JPanel p05 = new JPanel();
			p05.setLayout(new FlowLayout());
			ui_jb_start = new JButton("開始");
			ui_jb_start.addActionListener(this);
			ui_jb_start.setPreferredSize(new Dimension(200, 50));
			ui_jb_start.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			p05.add(ui_jb_start);
			title.add(p05, "South");
			
			display.add(title);
			setVisible(true);
			repaint();
			break;
			
		//対局画面
		case PHASE_BATTLE:
			display.removeAll();
			JPanel battle = new JPanel();
			battle.setLayout(new BorderLayout(10, 10));
			battle.setBackground(BACKGROUND_COLOR);
			
			JPanel p06 = new JPanel();
			p06.setLayout(new FlowLayout(FlowLayout.RIGHT, 40, 0));
			p06.setBackground(BACKGROUND_COLOR);
			ui_jl_name1 = new JLabel(othello.getPlayers()[0].getPlayerName());
			ui_jl_name1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_time1 = new JLabel("00:00");
			ui_jl_time1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			JLabel ui_jl_turn1 = new JLabel(getStoneIcon((othello.getPlayers()[0].isFirstMover() ? Othello.BLACK : Othello.WHITE), -1));
			p06.add(ui_jl_turn1);
			p06.add(ui_jl_name1);
			p06.add(ui_jl_time1);
			battle.add(p06, "South");
			
			JPanel p07 = new JPanel();
			p07.setLayout(new FlowLayout(FlowLayout.LEFT, 40, 0));
			p07.setBackground(BACKGROUND_COLOR);
			ui_jl_name2 = new JLabel(othello.getPlayers()[1].getPlayerName());
			ui_jl_name2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_time2 = new JLabel("00:00");
			ui_jl_time2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			JLabel ui_jl_turn2 = new JLabel(getStoneIcon((othello.getPlayers()[1].isFirstMover() ? Othello.BLACK : Othello.WHITE), -1));
			p07.add(ui_jl_time2);
			p07.add(ui_jl_name2);
			p07.add(ui_jl_turn2);
			battle.add(p07, "North");
			
			JPanel p08 = new JPanel();
			p08.setLayout(new GridLayout(2, 1));
			p08.setBackground(BACKGROUND_COLOR);
			ui_jl_nstones1 = new JLabel("×  2 ");
			ui_jl_nstones1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_nstones1.setIcon(getStoneIcon(Othello.BLACK, -1));
			ui_jl_nstones2 = new JLabel("×  2 ");
			ui_jl_nstones2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_nstones2.setIcon(getStoneIcon(Othello.WHITE, -1));
			p08.add(ui_jl_nstones1, "A");
			p08.add(ui_jl_nstones2, "B");
			battle.add(p08, "West");
			
			JPanel p09 = new JPanel();
			p09.setBackground(BACKGROUND_COLOR);
			p09.setLayout(new BorderLayout());
			ui_jb_giveup = new JButton("投了");
			ui_jb_giveup.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_giveup.addActionListener(this);
			ui_jb_giveup.setPreferredSize(new Dimension(110, 50));
			p09.add(ui_jb_giveup, "South");
			battle.add(p09, "East");
			
			JPanel p10 = new JPanel();
			p10.setLayout(new GridLayout(8, 8, -6, -6));
			p10.setBackground(BACKGROUND_COLOR);
			int[][] board_state = othello.getBoard();
			for(int i = 0; i < 8; i++) {
				for(int j = 0; j < 8; j++) {
					ui_jb_field[i][j] = new JButton(Integer.toString(i) + Integer.toString(j));
					ui_jb_field[i][j].setIcon(getStoneIcon(board_state[i][j], 0));
					ui_jb_field[i][j].setDisabledIcon(getStoneIcon(board_state[i][j], 0));
					ui_jb_field[i][j].setMargin(new Insets(-4, -4, -4, -24));
					ui_jb_field[i][j].addActionListener(this);
					ui_jb_field[i][j].setEnabled(board_state[i][j] == Othello.EMPTY);
					p10.add(ui_jb_field[i][j]);
				}
			}
			battle.add(p10);
			
			display.add(battle);
			setVisible(true);
			repaint();
			break;
			
		case PHASE_RESULT:
			display.removeAll();
			JPanel result = new JPanel();
			result.setLayout(new BorderLayout(30, 150));
			
			JPanel p11 = new JPanel();
			p11.setLayout(new GridLayout(3, 1));
			JLabel ui_jl_result0 = new JLabel("", SwingConstants.CENTER);
			ui_jl_result0.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 64));
			JLabel ui_jl_result1 = new JLabel("何対何で", SwingConstants.CENTER);
			ui_jl_result1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 48));
			JLabel ui_jl_result2 = new JLabel("あなたの勝ち！", SwingConstants.CENTER);
			ui_jl_result2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 48));
			p11.add(ui_jl_result0);
			p11.add(ui_jl_result1);
			p11.add(ui_jl_result2);
			result.add(p11, "Center");
			
			JPanel p12 = new JPanel();
			p12.setLayout(new GridLayout(2, 1, 30, 30));
			ui_jb_totitle = new JButton("タイトルへ");
			ui_jb_totitle.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_totitle.setPreferredSize(new Dimension(200, 50));
			ui_jb_exit = new JButton("終了");
			ui_jb_exit.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_exit.setPreferredSize(new Dimension(200, 50));
			p12.add(ui_jb_totitle);
			p12.add(ui_jb_exit);
			result.add(p12, "South");
			
			display.add(result);
			setVisible(true);
			repaint();
			break;
			
		default:
			break;
		}
	}
	
	private ImageIcon getStoneIcon(int c, int angle)
	{
		Image img = createImage(50, 50);
		Graphics g = img.getGraphics();
		
		if(angle != -1) {
			g.setColor(new Color(0, 127, 0));
			if(angle == -2) {
				g.setColor(new Color(191, 239, 239));
			}
			for(int i = 0; i < 50; i++) {
				g.drawLine(i, 0, i, 50 - 1);
			}
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, 50 - 1, 50 - 1);
			g.drawRect(1, 1, 50 - 3, 50 - 3);
		}
		
		if(c == Othello.BLACK) {
			g.setColor(angle < 180 ? Color.BLACK : Color.WHITE);
			g.fillOval(10 - (int)(8 * Math.sin(Math.PI / 360 * angle)), 9 + (int)(8 * Math.sin(Math.PI / 360 * angle)), 30 + (int)(16 * Math.sin(Math.PI / 360 * angle)), 1 + (int)((15 + (8 * Math.sin(Math.PI / 360 * angle))) * ( (Math.cos(Math.PI / 180 * angle) + 1))));
		}
		else if(c == Othello.WHITE) {
			g.setColor(angle < 180 ? Color.WHITE : Color.BLACK);
			g.fillOval(10 - (int)(8 * Math.sin(Math.PI / 360 * angle)), 9 + (int)(8 * Math.sin(Math.PI / 360 * angle)), 30 + (int)(16 * Math.sin(Math.PI / 360 * angle)), 1 + (int)((15 + (8 * Math.sin(Math.PI / 360 * angle))) * ( (Math.cos(Math.PI / 180 * angle) + 1))));
		}
		
		ImageIcon icon = new ImageIcon(img);
		return icon;
	}

	public void reloadDisplay(int[] play)
	{
		boolean[][] change_board = othello.applyMove(play);
		int[][] board = othello.getBoard();
		int count_black = 0;
		int count_white = 0;
		(ui_jb_field[play[0]][play[1]]).setDisabledIcon(getStoneIcon((othello.getCurrentTurn() ? Othello.WHITE : Othello.BLACK), 0));
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				int di = i;
				int dj = j;
				if(change_board[i][j]) {
					new Thread(() -> {
						try {
							for(int k = 0; k <= 360; k += 2) {
								Thread.sleep(2);
								ui_jb_field[di][dj].setDisabledIcon(getStoneIcon(othello.getCurrentTurn() ? Othello.BLACK : Othello.WHITE, k));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				}
				if(board[i][j] == Othello.BLACK) {
					count_black++;
				}
				else if(board[i][j] == Othello.WHITE) {
					count_white++;
				}
			}
		}
		ui_jl_nstones1.setText("× " + (count_black >= 10 ? "" : " ") + count_black + " ");
		ui_jl_nstones2.setText("× " + (count_white >= 10 ? "" : " ") + count_white + " ");
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
	private int[] getCommand() {
		int[] play = new int[2];
		boolean[][] field = othello.searchPlaceable();
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				ui_jb_field[i][j].setEnabled(field[i][j]);
				if(field[i][j]) {
					ui_jb_field[i][j].setIcon(getStoneIcon(Othello.EMPTY, -2));
				}
			}
		}
		ui_jb_giveup.setEnabled(true);
		
		try {
			int millis;
			command_pressed = true;
			while(command_pressed) {
				Thread.sleep(100);
				millis = othello.getPlayers()[0].getLeftTime() - 100;
				othello.getPlayers()[0].setLeftTime(millis);
				ui_jl_time1.setText((millis >= 600000 ? "" : " ") + millis / 60000 + ":" + (((millis / 1000) % 60) < 10 ? "0" : "") + ((millis / 1000) % 60));
				if(millis <= 0) { //時間切れ
					play[0] = 8;
					play[1] = 9;
					break;
				}
			}
			for(int i = 0; i < 8; i++) {
				for(int j = 0; j < 8; j++) {
					ui_jb_field[i][j].setEnabled(false);
				}
			}
			ui_jb_giveup.setEnabled(false);
			Thread.sleep(10);
			play[0] = command_value[0];
			play[1] = command_value[1];
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return play;
	}

	
	public void endBattle()
	{
		
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		String s = ae.getActionCommand();
		
		if(s.equals("5分")) {
			ui_jb_5min.setForeground(Color.GREEN);
			ui_jb_10min.setForeground(Color.BLACK);
			ui_jb_20min.setForeground(Color.BLACK);
			
			//ui_jb_5min.setBackground(Color.GREEN);
			//ui_jb_10min.setBackground(Color.WHITE);
			//ui_jb_20min.setBackground(Color.WHITE);
		}
		else if(s.equals("10分")) {
			ui_jb_5min.setForeground(Color.BLACK);
			ui_jb_10min.setForeground(Color.GREEN);
			ui_jb_20min.setForeground(Color.BLACK);
			
			//ui_jb_5min.setBackground(Color.WHITE);
			//ui_jb_10min.setBackground(Color.GREEN);
			//ui_jb_20min.setBackground(Color.WHITE);
		}
		else if(s.equals("20分")) {
			ui_jb_5min.setForeground(Color.BLACK);
			ui_jb_10min.setForeground(Color.BLACK);
			ui_jb_20min.setForeground(Color.GREEN);
			
			//ui_jb_5min.setBackground(Color.WHITE);
			//ui_jb_10min.setBackground(Color.WHITE);
			//ui_jb_20min.setBackground(Color.GREEN);
		}
		else if(s.equals("開始")) {
			ui_jb_start.setText("マッチング中止");
			connectToServer();
		}
		else if(s.equals("マッチング中止")) {
			ui_jb_start.setText("開始");
			
		}
		else if(s.equals("投了")) {
			command_pressed = false;
			command_value[0] = 8;
			command_value[1] = 8;
		}
		
		else {
			command_pressed = false;
			try {
				int i = (int)s.charAt(0) - (int)'0';
				int j = (int)s.charAt(1) - (int)'0';
				
				if(i < 0 || i >= 8 || j < 0 || j >= 8) {
					throw new Exception("Incorrect button processing.");
				}
				
				command_value[0] = i;
				command_value[1] = j;
			}
			catch(Exception ex) {
				ex.printStackTrace();
				System.exit(0);
			}
		}
		/*new Thread(() -> {
			try {
				for(int i = 0; i <= 360; i += 2) {
					Thread.sleep(2);
					ui_jb_field[2][2].setIcon(getStoneIcon(1, i));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();*/
	}
	
	//JTextAreaの「プレイヤ名」の表示切り替え
	public void focusGained(FocusEvent fe)
	{
		String s = ui_tf_namefield.getText();
		if(s.equals("プレイヤ名")) {
			ui_tf_namefield.setText("");
			ui_tf_namefield.setForeground(Color.BLACK);
		}
	}
	
	public void focusLost(FocusEvent fe)
	{
		String s = ui_tf_namefield.getText();
		if(s.equals("")) {
			ui_tf_namefield.setText("プレイヤ名");
			ui_tf_namefield.setForeground(Color.LIGHT_GRAY);
		}
	}
	
    public static void main(String[] args)
    {
        Client client = new Client("Othello Game");
        client.changePhase(PHASE_TITLE);
        client.othello = new Othello(new Player("aiueo", true, 60000 * 10 + 5000), new Player("oeuia", false, 60000 * 11));
        client.changePhase(PHASE_BATTLE);
        int[] play = client.getCommand();
        client.reloadDisplay(play);
        while(true) {
        	play = client.getCommand();
        	client.reloadDisplay(play);
        }
        //client.changePhase(PHASE_RESULT);
    }

}

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
	
	Client(String title)
	{
		super(title);
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
			ui_jb_5min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_10min = new JButton("10分");
			ui_jb_10min.addActionListener(this);
			ui_jb_10min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jb_20min = new JButton("20分");
			ui_jb_20min.addActionListener(this);
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
			
			JPanel p06 = new JPanel();
			p06.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 0));
			ui_jl_name1 = new JLabel(othello.getPlayers()[0].getPlayerName());
			ui_jl_name1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_time1 = new JLabel("5:00");
			ui_jl_time1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			p06.add(ui_jl_name1);
			p06.add(ui_jl_time1);
			battle.add(p06, "South");
			
			JPanel p07 = new JPanel();
			p07.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
			ui_jl_name2 = new JLabel(othello.getPlayers()[1].getPlayerName());
			ui_jl_name2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			ui_jl_time2 = new JLabel("5:00");
			ui_jl_time2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
			p07.add(ui_jl_time2);
			p07.add(ui_jl_name2);
			battle.add(p07, "North");
			
			JPanel p08 = new JPanel();
			p08.setLayout(new GridLayout(2, 1));
			ui_jl_nstones1 = new JLabel("3");
			ui_jl_nstones2 = new JLabel("3");
			p08.add(ui_jl_nstones1, "A");
			p08.add(ui_jl_nstones2, "B");
			battle.add(p08, "West");
			
			JPanel p09 = new JPanel();
			p09.setLayout(new BorderLayout());
			ui_jb_giveup = new JButton("投了");
			p09.add(ui_jb_giveup, "South");
			battle.add(p09, "East");
			
			JPanel p10 = new JPanel();
			p10.setLayout(new GridLayout(8, 8, -6, -6));
			for(int i = 0; i < 8; i++) {
				for(int j = 0; j < 8; j++) {
					ui_jb_field[i][j] = new JButton();//Integer.toString(i) + Integer.toString(j));
					ui_jb_field[i][j].setIcon(getStoneIcon());
					ui_jb_field[i][j].setMargin(new Insets(-4, -4, -4, -4));
					p10.add(ui_jb_field[i][j]);
				}
			}
			battle.add(p10);
			
			display.add(battle);
			setVisible(true);
			repaint();
			break;
		case PHASE_RESULT:
			
			break;
		default:
			break;
		}
	}
	
	private ImageIcon getStoneIcon()
	{
		Image img = createImage(50, 50);
		Graphics g = img.getGraphics();
		g.setColor(Color.GREEN);
		for(int i = 0; i < 50; i++) {
			g.drawLine(i, 0, i, 50 - 1);
		}
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, 50 - 1, 50 - 1);
		g.drawRect(1, 1, 50 - 3, 50 - 3);
		
		ImageIcon icon = new ImageIcon(img);
		return icon;
	}

	public void reloadDisplay()
	{
		
	}
	/*
	public String[] getWaitingPlayers()
	{
		
	}

	public String[] accessServer()
	{

	}

	public boolean reactConnection()
	{
		
	}
*/



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
        client.othello = new Othello(new Player("aiueo", true, 800), new Player("oeuia", false, 800));
        client.changePhase(PHASE_BATTLE);

    }

}

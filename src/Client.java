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
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Client extends JFrame implements ActionListener, FocusListener {

	private static final int SERVER_PORT_1 = 10000;// 接続確認に使うほうはこちら
	private static final int SERVER_PORT_2 = 10001;// 部屋確認に使うほうはこちら
	private static final int TIMEOUT_INTERVAL = 5000; // 5秒サーバーからのレスポンスがなかったらタイムアウトする
	private static final int PHASE_TITLE = 0; // changePhase()の引数でタイトル画面への遷移を表す
	private static final int PHASE_BATTLE = 1; // changePhase()の引数で対局画面への遷移を表す
	private static final int PHASE_RESULT = 2; // changePhase()の引数で結果画面への遷移を表す
	private static final Color BACKGROUND_COLOR = new Color(207, 207, 207);
	private static Othello othello;
	private Player me;
	private Player your;
	private final int[] vacantRoom = { -1, -1, -1 };
	private boolean eob_flag = false;
	private static boolean connectFlag = true;

	private int[] newPlay = { -1, -1, -1 };// 最新の相手が指した手
	private boolean newPlayFlag = false;
	private boolean matching = false;

	private final static InetAddress hostname;

	static {
		try {
			hostname = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	private final JPanel display = new JPanel();

	// タイトル画面のオブジェクト
	private JTextField ui_tf_namefield;
	private JButton ui_jb_5min;
	private JButton ui_jb_10min;
	private JButton ui_jb_20min;
	private int button_selected = 1;
	private JLabel ui_jl_5min;
	private JLabel ui_jl_10min;
	private JLabel ui_jl_20min;
	private JButton ui_jb_start;

	// 対局画面のオブジェクト
	private JLabel ui_jl_name1;
	private JLabel ui_jl_name2;
	private JLabel ui_jl_time1;
	private JLabel ui_jl_time2;
	private JLabel ui_jl_nstones1;
	private JLabel ui_jl_nstones2;
	private JButton ui_jb_giveup;
	private final JButton[][] ui_jb_field = new JButton[8][8];
	private boolean command_pressed = false;
	private final int[] command_value = { -1, -1 };

	// 結果画面のオブジェクト
	private JButton ui_jb_totitle;
	private JButton ui_jb_exit;
	private String endmsg1;
	private String endmsg2;

	private Socket socket;

	public Client(String title) {
		super(title);
		// RepaintManager currentManager = RepaintManager.currentManager(this);
		// currentManager.setDoubleBufferingEnabled(false);
		setLayout(new FlowLayout());

		// スタート画面
		JPanel start_display = new JPanel();
		start_display.setLayout(new BorderLayout());
		JLabel ui_jl_logo1 = new JLabel(new ImageIcon("img/othellogo.png"));
		start_display.add(ui_jl_logo1, "Center");
		start_display.add(new JLabel("通信中・・・", SwingConstants.CENTER), "South");
		display.add(start_display);
		add(display);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(BACKGROUND_COLOR);
		setSize(800, 600);
		setVisible(true);

		// 接続を待つ(socket予定地)
		try {
			while (vacantRoom[0] == -1) {
				Thread.sleep(1000);
				checkVacantRoom();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);
		}

		// タイトル画面
		changePhase(PHASE_TITLE);
		ui_jl_5min.setText((vacantRoom[0] == 1 ? "○" : "×"));
		ui_jl_10min.setText((vacantRoom[1] == 1 ? "○" : "×"));
		ui_jl_20min.setText((vacantRoom[2] == 1 ? "○" : "×"));

	}

	public void changePhase(int phase) {
		switch (phase) {
			// タイトル画面
			case PHASE_TITLE:
				display.removeAll();
				JPanel title = new JPanel();
				title.setLayout(new BorderLayout(0, 30));

				// ロゴとプレイヤ名入力エリア
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

				// 持ち時間選択ボタン
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
				// ui_jb_5min.setBackground(Color.WHITE);
				ui_jb_5min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				ui_jb_10min = new JButton("10分");
				ui_jb_10min.addActionListener(this);
				ui_jb_10min.setForeground(Color.GREEN);
				// ui_jb_10min.setBackground(Color.GREEN);
				ui_jb_10min.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				ui_jb_20min = new JButton("20分");
				ui_jb_20min.addActionListener(this);
				ui_jb_20min.setForeground(Color.BLACK);
				// ui_jb_20min.setBackground(Color.WHITE);
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

				// 開始/中止ボタン
				JPanel p05 = new JPanel();
				p05.setLayout(new FlowLayout());
				ui_jb_start = new JButton("開始");
				ui_jb_start.addActionListener(this);
				ui_jb_start.setPreferredSize(new Dimension(250, 50));
				ui_jb_start.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				p05.add(ui_jb_start);
				title.add(p05, "South");

				display.add(title);
				setVisible(true);
				repaint();
				break;

			// 対局画面
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
				ui_jl_time1.setText((othello.getPlayers()[0].getLeftTime() >= 600000 ? "" : " ")
						+ othello.getPlayers()[0].getLeftTime() / 60000 + ":"
						+ (((othello.getPlayers()[0].getLeftTime() / 1000) % 60) < 10 ? "0" : "")
						+ ((othello.getPlayers()[0].getLeftTime() / 1000) % 60));
				ui_jl_time1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				JLabel ui_jl_turn1 = new JLabel(
						getStoneIcon((othello.getPlayers()[0].isFirstMover() ? Othello.BLACK : Othello.WHITE), -1));
				System.out.println(othello.getPlayers()[0].isFirstMover() ? Othello.BLACK : Othello.WHITE);
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
				ui_jl_time2.setText((othello.getPlayers()[1].getLeftTime() >= 600000 ? "" : " ")
						+ othello.getPlayers()[1].getLeftTime() / 60000 + ":"
						+ (((othello.getPlayers()[1].getLeftTime() / 1000) % 60) < 10 ? "0" : "")
						+ ((othello.getPlayers()[1].getLeftTime() / 1000) % 60));
				ui_jl_time2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				JLabel ui_jl_turn2 = new JLabel(
						getStoneIcon((othello.getPlayers()[1].isFirstMover() ? Othello.BLACK : Othello.WHITE), -1));
				System.out.println(othello.getPlayers()[1].isFirstMover() ? Othello.BLACK : Othello.WHITE);
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
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						ui_jb_field[i][j] = new JButton(Integer.toString(i) + j);
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
				JLabel ui_jl_result1 = new JLabel(endmsg1, SwingConstants.CENTER);
				ui_jl_result1.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 48));
				JLabel ui_jl_result2 = new JLabel(endmsg2, SwingConstants.CENTER);
				ui_jl_result2.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 48));
				p11.add(ui_jl_result0);
				p11.add(ui_jl_result1);
				p11.add(ui_jl_result2);
				result.add(p11, "Center");

				JPanel p12 = new JPanel();
				p12.setLayout(new GridLayout(2, 1, 30, 30));
				ui_jb_totitle = new JButton("タイトルへ");
				ui_jb_totitle.addActionListener(this);
				ui_jb_totitle.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				ui_jb_totitle.setPreferredSize(new Dimension(250, 50));
				ui_jb_exit = new JButton("終了");
				ui_jb_exit.addActionListener(this);
				ui_jb_exit.setFont(new Font("ＭＳ ゴシック", Font.PLAIN, 24));
				ui_jb_exit.setPreferredSize(new Dimension(250, 50));
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

	private ImageIcon getStoneIcon(int c, int angle) {
		Image img = createImage(50, 50);
		Graphics g = img.getGraphics();

		if (angle != -1) {
			g.setColor(new Color(0, 127, 0));
			if (angle == -2) {
				g.setColor(new Color(191, 239, 239));
			}
			for (int i = 0; i < 50; i++) {
				g.drawLine(i, 0, i, 50 - 1);
			}
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, 50 - 1, 50 - 1);
			g.drawRect(1, 1, 50 - 3, 50 - 3);
		}

		if (c == Othello.BLACK) {
			g.setColor(angle < 180 ? Color.BLACK : Color.WHITE);
			g.fillOval(10 - (int) (8 * Math.sin(Math.PI / 360 * angle)),
					9 + (int) (8 * Math.sin(Math.PI / 360 * angle)), 30 + (int) (16 * Math.sin(Math.PI / 360 * angle)),
					1 + (int) ((15 + (8 * Math.sin(Math.PI / 360 * angle))) * ((Math.cos(Math.PI / 180 * angle) + 1))));
		} else if (c == Othello.WHITE) {
			g.setColor(angle < 180 ? Color.WHITE : Color.BLACK);
			g.fillOval(10 - (int) (8 * Math.sin(Math.PI / 360 * angle)),
					9 + (int) (8 * Math.sin(Math.PI / 360 * angle)), 30 + (int) (16 * Math.sin(Math.PI / 360 * angle)),
					1 + (int) ((15 + (8 * Math.sin(Math.PI / 360 * angle))) * ((Math.cos(Math.PI / 180 * angle) + 1))));
		}

		ImageIcon icon = new ImageIcon(img);
		return icon;
	}

	public void reloadDisplay(int[] play) {
		if (play[0] == 16) { // 投了
			endmsg1 = (othello.getCurrentTurn() == othello.getPlayers()[0].isFirstMover()) ? "あなたが" : "対戦相手が";
			endmsg2 = "投了しました";
			eob_flag = true;
			return;
		} else if (play[0] >= 8 & play[0] <= 15) {
			if (play[1] == 8) {
				eob_flag = true;
				endmsg1 = "時間制限で";
				switch (othello.checkWinner()) {
					case 3: // 相手の時間切れ
						endmsg2 = "あなたの勝ち！";
						break;
					case -3: // 自分の時間切れ
						endmsg2 = "あなたの負け！";
						break;
					default:
						break;
				}
				return;
			} else {

				play[0] -= 8;
				System.out.println("applyMove: (" + play[0] + ", " + play[1] + ")");
				othello.applyMove(play);
				int[][] result_board = othello.getBoard();
				int count_black = 0;
				int count_white = 0;
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						if (result_board[i][j] == Othello.BLACK) {
							count_black++;
						} else if (result_board[i][j] == Othello.WHITE) {
							count_white++;
						}
					}
				}
				endmsg1 = (othello.getPlayers()[0].isFirstMover() ? count_black : count_white) + "対"
						+ (othello.getPlayers()[0].isFirstMover() ? count_white : count_black) + "で";
				System.out.println(endmsg1);
				eob_flag = true;
				switch (othello.checkWinner()) {
					case 1: // 自分の通常勝利
						endmsg2 = "あなたの勝ち！";
						eob_flag = true;
						break;
					case -1: // 相手の通常勝利
						endmsg2 = "あなたの負け！";
						eob_flag = true;
						break;
					case 0: // 引き分け
						endmsg2 = "引き分け！";
						eob_flag = true;
						break;
				}
				System.out.println(endmsg2);
				return;
			}
		}
		System.out.println("applyMove: (" + play[0] + ", " + play[1] + ")");
		boolean[][] change_board = othello.applyMove(play);
		if (othello.checkWinner() != 2) {
			int[][] result_board = othello.getBoard();
			int count_black = 0;
			int count_white = 0;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					if (result_board[i][j] == Othello.BLACK) {
						count_black++;
					} else if (result_board[i][j] == Othello.WHITE) {
						count_white++;
					}
				}
			}
			endmsg1 = (othello.getPlayers()[0].isFirstMover() ? count_black : count_white) + "対"
					+ (othello.getPlayers()[0].isFirstMover() ? count_white : count_black) + "で";
			System.out.println(endmsg1);
			eob_flag = true;
			switch (othello.checkWinner()) {
				case -1: // 自分の通常勝利
					endmsg2 = "あなたの勝ち！";
					eob_flag = true;
					break;
				case 1: // 相手の通常勝利
					endmsg2 = "あなたの負け！";
					eob_flag = true;
					break;
				case 0: // 引き分け
					endmsg2 = "引き分け！";
					eob_flag = true;
					break;
			}
			System.out.println(endmsg2);
			return;
		}
		int[][] board = othello.getBoard();
		int count_black = 0;
		int count_white = 0;
		if (play[0] != 18) { // パスでないとき
			(ui_jb_field[play[0]][play[1]])
					.setDisabledIcon(getStoneIcon((othello.getCurrentTurn() ? Othello.WHITE : Othello.BLACK), 0));
		}
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				int di = i;
				int dj = j;
				if (change_board[i][j]) {
					new Thread(() -> {
						try {
							for (int k = 0; k <= 360; k += 2) {
								Thread.sleep(2);
								ui_jb_field[di][dj].setDisabledIcon(
										getStoneIcon(othello.getCurrentTurn() ? Othello.BLACK : Othello.WHITE, k));
							}
							ui_jb_field[di][dj].setDisabledIcon(
									getStoneIcon(board[di][dj], 0));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				}
				if (board[i][j] == Othello.BLACK) {
					count_black++;
				} else if (board[i][j] == Othello.WHITE) {
					count_white++;
				}
			}
		}
		ui_jl_nstones1.setText("× " + (count_black >= 10 ? "" : " ") + count_black + " ");
		ui_jl_nstones2.setText("× " + (count_white >= 10 ? "" : " ") + count_white + " ");
	}

	public void doMyTurn() {
		int[] in = new int[2];
		in = getCommand();
		reloadDisplay(in);
		int[] out = new int[3];
		out[0] = in[0];
		out[1] = in[1];
		out[2] = in[0] == 16 && in[1] == 0 ? 0 : othello.getPlayers()[0].getLeftTime();
		try {
			if (out[2] <= 0) {// 持ち時間0以下
				out[0] = 8;
				out[1] = 8;
				sendCommand(out);
				eob_flag = true;
			} else if (othello.checkWinner() != 2) {// 盤面勝者確定
				out[0] += 8;
				sendCommand(out);
				eob_flag = true;
			} else {
				sendCommand(out);
			}
		} catch (IOException e) {
			// サーバーに接続できなかったら終了する
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public void doYourTurn() {
		int millis = othello.getPlayers()[1].getLeftTime();
		int[] out = new int[2];
		try {
			while (!newPlayFlag) {
				Thread.sleep(100);
				millis = othello.getPlayers()[1].getLeftTime() - 100;
				othello.getPlayers()[1].setLeftTime(millis);
				ui_jl_time2.setText((millis >= 600000 ? "" : " ") + millis / 60000 + ":"
						+ (((millis / 1000) % 60) < 10 ? "0" : "") + ((millis / 1000) % 60));
				if (millis <= 0) { // 時間切れ
					out[0] = 8;
					out[1] = 8;
					endmsg1 = "時間制限で";
					endmsg2 = "あなたの勝ち！";
					eob_flag = true;
					return;
				}
			}
			newPlayFlag = false;
			out[0] = newPlay[0];
			out[1] = newPlay[1];
			othello.getPlayers()[1].setLeftTime(newPlay[2]);
			Thread.sleep(10);
			System.out.println(out[0] + " " + out[1]);
			reloadDisplay(out);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		if (newPlay[2] <= 0) {// 持ち時間0以下
			eob_flag = true;
		} else if (othello.checkWinner() != 2) {// 盤面勝者確定
			eob_flag = true;
		}

	}

	public void connectToServer() {
		try {

			// サーバーに接続する

			// 名前とルーム番号をサーバーに送信する
			String name = ui_tf_namefield.getText();
			int roomNumber = button_selected;
			sendPlayerInfo(name, roomNumber);

			try {
				// プレイヤ名とルーム番号を受信する
				InputStream in = socket.getInputStream();
				DataInputStream dis = new DataInputStream(in);

				String opponentName = null;
				int heartbeat_0;
				int turnNum = 0;

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
				boolean turn = turnNum != 0;

				// 1のとき300秒,2のとき600秒,3のとき1200秒となる
				int leftTime;
				switch (roomNumber) {
					case 0:
						leftTime = 5 * 60 * 1000;
						break;
					case 1:
						leftTime = 10 * 60 * 1000;
						break;
					case 2:
						leftTime = 20 * 60 * 1000;
						break;
					default:
						leftTime = 1000000;
						break;
				}

				me = new Player(name, turn, leftTime);
				your = new Player(opponentName, !turn, leftTime);
				othello = new Othello(me, your);
				System.out.println(me.getPlayerName() + othello.getPlayers()[0].isFirstMover());
				System.out.println(your.getPlayerName() + othello.getPlayers()[1].isFirstMover());

			} catch (IOException e) {
				e.printStackTrace();
				socket.close();
			}

			connectFlag = false;// 画面遷移用フラグ

			new Thread(() -> {
				while (true) {
					try {
						// サーバーからのデータを受け取る
						int[] response = receiveResponse();

						if (response[0] == 16) {
							if (response[2] == 0) {
								// 投了
								newPlay = response;
								newPlayFlag = true;
							} else if (response[2] == 1) {
								// ハートビートを送り返す処理をする
								sendHeartbeat(1);
							} else if (response[0] >= 8 && response[0] < 16) {
								newPlay[0] = response[0] - 8;
								newPlay[1] = response[1];
								newPlay[2] = 0;
								newPlayFlag = true;
							}
						} else {
							if (response[0] < 20) {
								newPlay = response;
								newPlayFlag = true;
							}
						}
					} catch (SocketTimeoutException e) {
						// タイムアウトしたら例外処理を返して終了する
						System.err.println("接続がタイムアウトしました");
						try {
							socket.close();
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
						return;
					} catch (SocketException se) {

					} catch (IOException e) {
						// サーバーからのデータを受け取れなかったらエラー
						e.printStackTrace();
						System.err.println("サーバからのデータが読み取れませんでした。");
						try {
							socket.close();
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
						return;
					}
				}
			}).start();
		} catch (IOException e) {
			// サーバーに接続できなかったらエラー
			e.printStackTrace();
		}
	}

	/**
	 * 2つのint配列が等しいかどうかを判定する。
	 **/
	private static boolean isEqual(int[] a, int[] b) {
		for (int i = 0; i < 2; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}

	private void sendPlayerInfo(String name, int roomNumber) throws IOException {
		// 名前とルーム番号をサーバーに送信する
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.writeUTF(name);
		out.writeInt(roomNumber);
		out.flush();
	}

	/**
	 * int[2]のコマンドをサーバーに送信する。
	 * 配列の送信方法は
	 **/
	private void sendCommand(int[] command) throws IOException {
		command[2] = othello.getPlayers()[0].getLeftTime();
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		dos.writeInt(command[0]);
		dos.writeInt(command[1]);
		if (command[0] == 16) {
			dos.writeInt(0);
			System.out.println("投了希望を送信しました");
		} else {
			dos.writeInt(command[2]);
		}
		if (command[0] == 18) {
			System.out.println(command[0] + ", " + command[1] + ", " + command[2]);
		}
		dos.flush();
	}

	/**
	 * サーバーにハートビートを送信する。接続確認って言ってましたが俗にハートビートというらしいです。
	 **/
	private void sendHeartbeat(int flag) throws IOException {
		int[] heartbeat = new int[3];
		heartbeat[0] = 16;
		heartbeat[1] = 0;
		heartbeat[2] = flag;

		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		dos.writeInt(heartbeat[0]);
		dos.writeInt(heartbeat[1]);
		dos.writeInt(heartbeat[2]);
	}

	/**
	 * サーバーからのレスポンスを受け取る。
	 * タイムアウトした場合はSocketTimeoutExceptionを投げる。
	 **/
	private int[] receiveResponse() throws IOException {
		InputStream in = socket.getInputStream();
		DataInputStream dis = new DataInputStream(in);

		// socket.setSoTimeout(TIMEOUT_INTERVAL); // タイムアウト時間を設定する

		int[] response = new int[3];
		for (int i = 0; i < 3; i++) {
			response[i] = dis.readInt();
		}
		System.out.println("response 0,1,2 = " + response[0] + "," + response[1] + "," + response[2]);
		return response;
	}

	/**
	 * int[2]を取得する仮実装。最後に押されたボタンを所得する関数をお願いします。
	 **/
	private int[] getCommand() {
		int[] play = new int[2];
		boolean pass = false;
		boolean[][] field = othello.searchPlaceable();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				ui_jb_field[i][j].setEnabled(field[i][j]);
				if (field[i][j]) {
					ui_jb_field[i][j].setIcon(getStoneIcon(Othello.EMPTY, -2));
					pass = true;
				}
			}
		}
		if (!pass) {
			play[0] = 18;
			play[1] = 0;
			return play;
		}
		ui_jb_giveup.setEnabled(true);

		try {
			int millis = 600000;
			command_pressed = true;
			while (command_pressed) {
				Thread.sleep(100);
				millis = othello.getPlayers()[0].getLeftTime() - 100;
				othello.getPlayers()[0].setLeftTime(millis);
				ui_jl_time1.setText((millis >= 600000 ? "" : " ") + millis / 60000 + ":"
						+ (((millis / 1000) % 60) < 10 ? "0" : "") + ((millis / 1000) % 60));
				if (millis <= 0) { // 時間切れ
					play[0] = 8;
					play[1] = 8;
					break;
				}
			}
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					ui_jb_field[i][j].setEnabled(false);
				}
			}
			ui_jb_giveup.setEnabled(false);
			Thread.sleep(10);
			if (millis > 0) {
				play[0] = command_value[0];
				play[1] = command_value[1];
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return play;
	}

	public void checkVacantRoom() {
		Socket socket1 = null;
		try {
			socket1 = new Socket("localhost", SERVER_PORT_2);
			PrintWriter out = new PrintWriter(socket1.getOutputStream(), true);
			DataInputStream in = new DataInputStream(socket1.getInputStream());
			int heartbeat = 1; // ハートビートメッセージ

			out.println(heartbeat); // ハートビートメッセージを送信

			// サーバからのレスポンスをパースしてint配列に格納
			vacantRoom[0] = in.readInt();
			vacantRoom[1] = in.readInt();
			vacantRoom[2] = in.readInt();

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
	}

	public void endBattle() {
		eob_flag = false;
		changePhase(PHASE_RESULT);
		try {
			socket.close();// ソケットを閉じる
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void actionPerformed(ActionEvent ae) {
		String s = ae.getActionCommand();

		if (s.equals("5分")) {
			ui_jb_5min.setForeground(Color.GREEN);
			ui_jb_10min.setForeground(Color.BLACK);
			ui_jb_20min.setForeground(Color.BLACK);

			// ui_jb_5min.setBackground(Color.GREEN);
			// ui_jb_10min.setBackground(Color.WHITE);
			// ui_jb_20min.setBackground(Color.WHITE);
			button_selected = 0;
		} else if (s.equals("10分")) {
			ui_jb_5min.setForeground(Color.BLACK);
			ui_jb_10min.setForeground(Color.GREEN);
			ui_jb_20min.setForeground(Color.BLACK);

			// ui_jb_5min.setBackground(Color.WHITE);
			// ui_jb_10min.setBackground(Color.GREEN);
			// ui_jb_20min.setBackground(Color.WHITE);
			button_selected = 1;
		} else if (s.equals("20分")) {
			ui_jb_5min.setForeground(Color.BLACK);
			ui_jb_10min.setForeground(Color.BLACK);
			ui_jb_20min.setForeground(Color.GREEN);

			// ui_jb_5min.setBackground(Color.WHITE);
			// ui_jb_10min.setBackground(Color.WHITE);
			// ui_jb_20min.setBackground(Color.GREEN);
			button_selected = 2;
		} else if (s.equals("開始")) {
			ui_jb_start.setText("マッチング中止");
			try {
				socket.close();
			} catch (Exception ex) {

			}
			socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(hostname, SERVER_PORT_1), TIMEOUT_INTERVAL);
				matching = true;
				Thread connectThread = new Thread(() -> {
					connectToServer();
				});
				connectThread.start();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				ui_jb_start.setText("開始");
			}
		} else if (s.equals("マッチング中止")) {
			matching = false;
			try {
				socket.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			ui_jb_start.setText("開始");
		} else if (s.equals("投了")) {
			command_pressed = false;
			command_value[0] = 16;
			command_value[1] = 0;
			command_value[2] = 0;
		} else if (s.equals("終了")) {
			System.exit(0);
		} else if (s.equals("タイトルへ")) {
			changePhase(PHASE_TITLE);
		}

		else {
			command_pressed = false;
			try {
				int i = (int) s.charAt(0) - (int) '0';
				int j = (int) s.charAt(1) - (int) '0';

				if (i < 0 || i >= 8 || j < 0 || j >= 8) {
					throw new Exception("Incorrect button processing.");
				}

				command_value[0] = i;
				command_value[1] = j;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(0);
			}
		}
	}

	// JTextAreaの「プレイヤ名」の表示切り替え
	public void focusGained(FocusEvent fe) {
		String s = ui_tf_namefield.getText();
		if (s.equals("プレイヤ名")) {
			ui_tf_namefield.setText("");
			ui_tf_namefield.setForeground(Color.BLACK);
		}
	}

	public void focusLost(FocusEvent fe) {
		String s = ui_tf_namefield.getText();
		if (s.equals("")) {
			ui_tf_namefield.setText("プレイヤ名");
			ui_tf_namefield.setForeground(Color.LIGHT_GRAY);
		}
	}

	public static void main(String[] args) {
		Client client = new Client("Othello Game");
		while (true) {
			try {
				while (connectFlag) {
					Thread.sleep(1000);
					client.checkVacantRoom();
					client.ui_jl_5min.setText((client.vacantRoom[0] == 1 ? "○" : "×"));
					client.ui_jl_10min.setText((client.vacantRoom[1] == 1 ? "○" : "×"));
					client.ui_jl_20min.setText((client.vacantRoom[2] == 1 ? "○" : "×"));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			connectFlag = true;
			client.changePhase(PHASE_BATTLE);

			if (othello.getPlayers()[1].isFirstMover()) {
				System.out.println("doYourTurnから呼び出します"); // debug
				client.doYourTurn();
			} else {
				System.out.println("doMyTurnから呼び出します"); // debug
			}
			while (!client.eob_flag) {
				client.doMyTurn();
				if (client.eob_flag) {
					break;
				}
				client.doYourTurn();
			}
			client.endBattle();
		}
	}

}

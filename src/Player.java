public class Player {
    private String player_name;    //プレイヤ
    private boolean turn;    //先攻か後攻か
    private int time_left;    //残り持ち時間
    
    Player(String player_name, boolean turn, int time_left)
    {
        this.player_name = player_name;
        this.turn = turn;
        this.time_left = time_left;
    }
    
    public String getPlayerName()
    {
        return this.player_name;
    }
    
    public boolean isFirstMover()
    {
        return turn;
    }
    
    public int getLeftTime()
    {
        return time_left;
    }
    
    public void setLeftTime(int time_left)
    {
        this.time_left = time_left;
    }
}

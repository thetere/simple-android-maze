package ar.edu.ips.aus.seminario2.sampleproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.abemart.wroup.client.WroupClient;
import com.abemart.wroup.common.messages.MessageWrapper;
import com.abemart.wroup.service.WroupService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = GameView.class.getSimpleName();
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_UPDATING = "updating";
    private boolean updating = false;
    private GameAnimationThread thread;
    private Player player;
    private Map<String, Player> players = new HashMap<>();
    private PlayerSprite playerSprites;
    private int moves = 0;
    private static final int SERVER_UPDATE_RATIO = 3;
    private static final int CLIENT_UPDATE_RATIO = 2;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public GameView(Context context){
        super(context);
        init();
    }

    private void init() {
        getHolder().addCallback(this);

        String id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        player = new Player(id,0.5,0.5);
        players.put(id, player);

        playerSprites = new PlayerSprite(getResources());
        player.setOrder(playerSprites.getRandomSpriteNumber());

        thread = new GameAnimationThread(getHolder(), this);
        setFocusable(true);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry){
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    public void update(long delay) {
        if (this.updating) {
            MazeBoard board = GameApp.getInstance().getMazeBoard();
            // update only actual player
            player.move(board, delay);
            this.moves++;

            // send all players data
            if (GameApp.getInstance().isGameServer()) {
                if (this.moves % SERVER_UPDATE_RATIO == 0) {
                    WroupService server = GameApp.getInstance().getServer();
                    MessageWrapper message = new MessageWrapper();
                    Gson json = new Gson();
                    Message<Player[]> data = new Message<Player[]>(Message.MessageType.PLAYER_DATA,
                            players.values().toArray(new Player[]{}));
                    String msg = json.toJson(data);
                    message.setMessage(msg);
                    message.setMessageType(MessageWrapper.MessageType.NORMAL);
                    server.sendMessageToAllClients(message);
                }
            } else {
                if (this.moves % CLIENT_UPDATE_RATIO == 0) {
                    // send player data
                    WroupClient client = GameApp.getInstance().getClient();
                    MessageWrapper message = new MessageWrapper();
                    Gson json = new Gson();
                    Message<Player[]> data = new Message<Player[]>(Message.MessageType.PLAYER_DATA,
                            new Player[]{player});
                    String msg = json.toJson(data);
                    message.setMessage(msg);
                    message.setMessageType(MessageWrapper.MessageType.NORMAL);
                    client.sendMessageToServer(message);
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);

        if (canvas != null){
            MazeBoard board = GameApp.getInstance().getMazeBoard();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (Player p:this.players.values()) {
                Rect srcRect = playerSprites.getSourceRectangle(this, board, p, p.getOrder());
                Rect dstRect = playerSprites.getDestinationRectangle(this, board, p);
                Log.d("MAZE: ", String.format("src rect: %s - dst rect: %s", srcRect.toShortString(), dstRect.toShortString()));
                canvas.drawBitmap(playerSprites.getSprites(), srcRect, dstRect, null);
            }
        }
    }

    public void updatePlayerData(String message) {
        Gson gson = new Gson();
        Message<Player[]> playerData = gson.fromJson(message,
                new TypeToken<Message<Player[]>>(){}.getType());
        for (Player pd:playerData.getPayload()) {
            if (!player.getID().equals(pd.getID())) {
                Player p = players.get(pd.getID());
                if (p == null) {
                    p = new Player(pd.getID(), pd.getX(), pd.getY());
                    p.setOrder(pd.getOrder());
                    players.put(pd.getID(), p);
                }
                p.setX(pd.getX());
                p.setY(pd.getY());
                p.setXVel(pd.getXVel());
                p.setYVel(pd.getYVel());
            }
        }
    }

    public void updateStatus(String message) {
        Gson gson = new Gson();
        Message<String> gameStatus = gson.fromJson(message,
                new TypeToken<Message<String>>(){}.getType());
        if (gameStatus.getType() == Message.MessageType.GAME_STATUS){
            String data = gameStatus.getPayload();
            switch (data){
                case STATUS_PAUSED:
                    this.updating = false;
                    Log.d(TAG, "Game paused.");
                    //Toast.makeText(getContext(), "GAME PAUSED", Toast.LENGTH_LONG).show();
                    break;
                case STATUS_UPDATING:
                    this.updating = true;
                    Log.d(TAG, "Game updating.");
                    //Toast.makeText(getContext(), "GAME RESUMED", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }

    public void toggleStatus() {
        if (GameApp.getInstance().isGameServer()) {
            String status = null;
            if (this.updating) {
                this.updating = false;
                status = STATUS_PAUSED;
            } else {
                this.updating = true;
                status = STATUS_UPDATING;
            }
            WroupService server = GameApp.getInstance().getServer();
            MessageWrapper message = new MessageWrapper();
            Gson json = new Gson();
            Message<String> data = new Message<String>(Message.MessageType.GAME_STATUS,
                    status);
            String msg = json.toJson(data);
            message.setMessage(msg);
            message.setMessageType(MessageWrapper.MessageType.NORMAL);
            server.sendMessageToAllClients(message);
        }
    }
}

package ar.edu.ips.aus.seminario2.sampleproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameAnimationThread thread;

    private PlayerSprite playerSprites;

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
        Game.getInstance(this.getContext());
        getHolder().addCallback(this);

        Game.getInstance().initPlayers();

        playerSprites = new PlayerSprite(getResources());

        thread = new GameAnimationThread(getHolder(), this);
        setFocusable(true);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        if (thread.getState() == Thread.State.NEW)
            {
                thread.start();
            }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry){
            try {
                // interrupt thread
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                Log.d("MAZE: ", String.format("Error when destroying Surface %s", e.getMessage()));
            }
            retry = false;
        }
    }


    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);

        if (canvas != null){
            MazeBoard board = Game.getInstance().getMazeBoard();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            int count = 0;
            for (Player p: Game.getInstance().getPlayers()) {
                Rect srcRect = playerSprites.getSourceRectangle(this, board, p, count);
                Rect dstRect = playerSprites.getDestinationRectangle(this, board, p);
                //Log.d("PLAYER: ", String.format("x: %d - y: %d", (int) p.getX(), (int) p.getY()));
                //Log.d("MAZE: ", String.format("src rect: %s - dst rect: %s", srcRect.toShortString(), dstRect.toShortString()));
                if ((int) p.getX() == (board.getExitX()-1) && (int) p.getY() == (board.getExitY()-1)){
                    //Log.d("WIN: ", String.format("GANASTESSS"));
                    Intent intent = new Intent(getContext(), FinishSplashActivity.class);
                    getContext().startActivity(intent);
                    Game.getInstance().setStatus("FINISHED");
                    Game.getInstance().publicUpdateGameStatus();
                    //thread.setRunning(false);
                }
                canvas.drawBitmap(playerSprites.getSprites(), srcRect, dstRect, null);
                count++;
            }
        }
    }

}

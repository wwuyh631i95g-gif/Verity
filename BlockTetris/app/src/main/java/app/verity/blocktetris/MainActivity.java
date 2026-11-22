package app.verity.blocktetris;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public final class MainActivity extends Activity {
    private BlockTetrisView game;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        game = new BlockTetrisView(this);
        setContentView(game);
    }

    @Override protected void onResume() {
        super.onResume();
        if (game != null) game.resumeGame();
    }

    @Override protected void onPause() {
        if (game != null) game.pauseGame();
        super.onPause();
    }
}

package app.verity.cartoonblocks;
import android.app.Activity;
import android.os.Bundle;
public final class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF090615);
        getWindow().setNavigationBarColor(0xFF090615);
        setContentView(new GameView(this));
    }
}
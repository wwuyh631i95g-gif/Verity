package org.touchhle.android;

import android.content.Intent;
import org.libsdl.app.SDLActivity;

/** SDL-based native touchHLE runner. */
public final class MainActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL2", "touchHLE" };
    }

    @Override
    protected String[] getArguments() {
        Intent intent = getIntent();
        String ipaPath = intent == null ? null : intent.getStringExtra("ipa_path");
        if (ipaPath == null || ipaPath.trim().isEmpty()) {
            return new String[0];
        }
        return new String[] { ipaPath };
    }
}

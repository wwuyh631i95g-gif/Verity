package com.twinfold.duolink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

public class InstallStatusReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String pkg = intent.getStringExtra("package");
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { context.startActivity(confirm); } catch (Exception ignored) {}
            }
            return;
        }
        boolean success = status == PackageInstaller.STATUS_SUCCESS;
        if (pkg != null && !pkg.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences("twinfold", Context.MODE_PRIVATE);
            if (success) prefs.edit().putString("installed_move_pending", pkg).apply();
            else prefs.edit().remove("pending_move_package").remove("installed_move_pending").apply();
        }
        Intent complete = new Intent("com.twinfold.duolink.INSTALL_COMPLETE");
        complete.setPackage(context.getPackageName());
        complete.putExtra("package", pkg);
        complete.putExtra("success", success);
        context.sendBroadcast(complete);

        if (success) {
            Toast.makeText(context, "TwinFold: приложение установлено.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "TwinFold: установка не завершена. " + (message == null ? "" : message), Toast.LENGTH_LONG).show();
        }
    }
}

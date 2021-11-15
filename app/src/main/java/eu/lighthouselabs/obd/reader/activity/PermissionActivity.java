package eu.lighthouselabs.obd.reader.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

/**
 * @author caixingcun
 * @date 2021/11/15
 * Description :
 */
public class PermissionActivity extends AppCompatActivity {
    ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                Toast.makeText(PermissionActivity.this, entry.getKey() + " 请同意权限", Toast.LENGTH_LONG).show();
                return;
            }
        }
        showDialog();
    });

    private void showDialog() {
        String[] items = {"BlueBooth", "OBD"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择页面");
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                startActivity(new Intent(PermissionActivity.this, BlueBoothActivity.class));
                finish();
                return;
            }
            if (which == 1) {
                startActivity(new Intent(PermissionActivity.this, ObdActivity.class));
                finish();
                return;
            }

        });
        builder.show();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.WAKE_LOCK,
        };
        launcher.launch(permissions);
    }
}

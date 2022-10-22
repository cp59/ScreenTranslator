package com.zeroapp.screentranslator;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;

public class TileService extends android.service.quicksettings.TileService {
    @Override
    public void onClick() {
        super.onClick();
        Intent serviceIntent = new Intent(this, TranslatorAccessibilityService.class);
        startService(serviceIntent);
    }
    @Override
    public void onStartListening() {
        Tile tile=getQsTile();
        int state = Tile.STATE_INACTIVE;

        tile.setIcon(Icon.createWithResource(this,
                R.drawable.ic_baseline_translate_24));
        tile.setLabel(getString(R.string.app_name));
        tile.setState(state);
        tile.updateTile();
    }
}

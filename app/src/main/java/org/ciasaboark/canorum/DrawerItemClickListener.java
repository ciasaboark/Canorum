package org.ciasaboark.canorum;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by Jonathan Nelson on 1/16/15.
 */
public class DrawerItemClickListener implements ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
    }

    private void selectItem(int position) {

    }
}

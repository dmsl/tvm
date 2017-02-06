/*
 * Copyright (c) 2013, Data Management Systems Lab (DMSL), University of Cyprus.
 *
 * Author: P. Mpeis pmpeis01@cs.ucy.ac.cy (University of Cyprus)
 *
 * Project supervisors: A. Konstantinides, D. Zeinalipour-Yazti (University of Cyprus)
 *
 * This file is part of TVM.
 * TVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cy.ac.ucy.cs.tvm;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Navigation drawer. Can be pulled from left corner while in MainActivity. It provides access to:
 *
 * <ul>
 *   <li>some quick settings
 *   <li>starting experiments
 *   <li>recording real user routes
 * </ul>
 */
public class CustomNavDrawer extends ArrayAdapter<CustomNavDrawer.ListItemObject> {
    private static final String TAG = CustomNavDrawer.class.getSimpleName();
    private final App mApp;
    ArrayList<ListItemObject> mListItemObjects;

    public CustomNavDrawer(App app, ArrayList<ListItemObject> objects) {
        super(app.getApplicationContext(), R.layout.drawer_list_item, objects);
        this.mApp = app;
        this.mListItemObjects = objects;
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater =
                (LayoutInflater) mApp.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);

        final TextView textViewListItem =
                (TextView) rowView.findViewById(R.id.textViewListItemTitle);
        ListItemObject selectedObject = mListItemObjects.get(position);
        textViewListItem.setText(mListItemObjects.get(position).string);

        DisplayMetrics dm = mApp.getResources().getDisplayMetrics();
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);

        int textsize = 0;

        if (screenInches < 5) textsize = 20;
        else if (screenInches < 7.5) textsize = 22;
        else textsize = 24;

        switch (selectedObject.listItemType) {
            case Label:
                textViewListItem.setGravity(Gravity.CENTER);
                textViewListItem.setTextColor(mApp.getResources().getColor(android.R.color.black));
                textViewListItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, textsize);
                break;
            case Setting:
                break;
            case Experiment:
                break;
        }

        return rowView;
    }

    @Override
    public boolean isEnabled(int position) {

        //Labels cant be selected
        if (mListItemObjects.get(position).listItemType.equals(ListItemType.Label)) return false;

        return true;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public static enum ListItemType {
        //an experiment
        Experiment,
        // eg a title
        Label,
        // eg k anonymity
        Setting
    }

    public static class ListItemObject {
        final String string;
        final ListItemType listItemType;
        final String key;

        public ListItemObject(String string, ListItemType listItemType, String key) {
            this.string = string;
            this.listItemType = listItemType;
            this.key = key;
        }
    }
}

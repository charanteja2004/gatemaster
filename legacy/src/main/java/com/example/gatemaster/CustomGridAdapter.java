package com.example.gatemaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomGridAdapter extends BaseAdapter {
    private Context context;
    private final String[] items;
    private final int[] images;

    public CustomGridAdapter(Context context, String[] items, int[] images) {
        this.context = context;
        this.items = items;
        this.images = images;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridItem;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            gridItem = inflater.inflate(R.layout.grid_item, null);
        } else {
            gridItem = convertView;
        }

        TextView textView = gridItem.findViewById(R.id.text_view_item);
        ImageView imageView = gridItem.findViewById(R.id.image_view_item);

        textView.setText(items[position]);
        imageView.setImageResource(images[position]);

        return gridItem;
    }
}

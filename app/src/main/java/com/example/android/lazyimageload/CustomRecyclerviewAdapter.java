package com.example.android.lazyimageload;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomRecyclerviewAdapter extends BaseAdapter {
    private static final String LOG_TAG = CustomRecyclerviewAdapter.class.getName();
    private String[] data;
    public ImageLoader imageLoader;
    Context mContext;
    public CustomRecyclerviewAdapter(Context context,String[] data){
        mContext = context;
        this.data = data;
        imageLoader = new ImageLoader(context.getApplicationContext());
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view==null)
            view = LayoutInflater.from(mContext).inflate(R.layout.popular_movie_item,parent,false);
        TextView text = (TextView) view.findViewById(R.id.textView);
        ImageView image = (ImageView)view.findViewById(R.id.imageView);
        text.setText("item"+position);
        imageLoader.displayImage(data[position],image);
        return view;
    }
}

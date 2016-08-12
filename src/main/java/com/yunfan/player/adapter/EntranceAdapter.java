package com.yunfan.player.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.yfcloudplayer.R;
import com.yunfan.player.bean.VideoItem;

import java.util.List;

public class EntranceAdapter extends BaseAdapter {
	List<VideoItem> data;
	Context mContext;
	LayoutInflater inflater = null;
	
	public EntranceAdapter(Context mContext, List<VideoItem> data) {
		this.mContext = mContext;
		this.data = data;
		inflater = LayoutInflater.from(mContext);
	}


	public List<VideoItem> getList() {
		return data;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return data == null ? 0 : data.size();
	}


	@Override
	public VideoItem getItem(int position) {
		// TODO Auto-generated method stub
		if (data != null && data.size() != 0) {
			return data.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder mHolder;
		View view = convertView;
		if (view == null) {
			mHolder = new ViewHolder();
			view = inflater.inflate(R.layout.item_entrance, null);
			mHolder.function = (TextView) view.findViewById(R.id.videoName);
			mHolder.icon = (ImageView) view.findViewById(R.id.videoCapture);
			view.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) view.getTag();
		}
		mHolder.function.setText(data.get(position).getVideoName());
		mHolder.icon.setImageBitmap(data.get(position).getVideoCaptrue());
		return view;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	static class ViewHolder {
		TextView function;
		ImageView icon;
	}


}
